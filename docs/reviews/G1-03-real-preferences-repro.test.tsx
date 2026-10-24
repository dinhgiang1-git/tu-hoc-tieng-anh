import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { AuthProvider } from '@/features/auth/context/AuthContext';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { authApi } from '@/features/auth/api/auth.api';
import { csrfSession } from '@/lib/api/csrf-session';

const user={id:'A',username:'account-a',fullName:'Account A',timeZone:'Europe/London',role:'LEARNER'};
const json=(data:unknown,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json'}});
const problem=(status:number,code:string)=>json({status,code,detail:code,title:code,fieldErrors:[],traceId:'review'},status);
const originalFetch=globalThis.fetch;
beforeEach(()=>csrfSession.clear());
afterEach(()=>{cleanup();globalThis.fetch=originalFetch;vi.restoreAllMocks();csrfSession.clear();});

async function setupAuth(){
  const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});
  const wrapper=({children}:{children:ReactNode})=><QueryClientProvider client={client}><AuthProvider>{children}</AuthProvider></QueryClientProvider>;
  const hook=renderHook(()=>useAuth(),{wrapper});
  await waitFor(()=>expect(hook.result.current.isAuthenticated).toBe(true));
  return{client,...hook};
}

it('review: mutation 401 must expire the cached authenticated session',async()=>{
  globalThis.fetch=vi.fn(async(url)=>String(url).endsWith('/auth/csrf')?json({token:'t',headerName:'X-CSRF-TOKEN'}):String(url).endsWith('/me')?json(user):problem(401,'AUTHENTICATION_REQUIRED'));
  const{client,result}=await setupAuth();
  await act(async()=>{await client.getMutationCache().build(client,{mutationFn:()=>authApi.updatePreferences({dailyTargetMinutes:45})}).execute(undefined).catch(()=>{});});
  await waitFor(()=>expect(result.current.user).toBeNull(),{timeout:500});
});

it('review: query 401 must remove cached personal data as well as the user',async()=>{
  globalThis.fetch=vi.fn(async(url)=>String(url).endsWith('/me')?json(user):problem(401,'AUTHENTICATION_REQUIRED'));
  const{client,result}=await setupAuth();
  client.setQueryData(['auth','preferences'],{dailyTargetMinutes:90,focusedEnrollmentId:null});
  await act(async()=>{await client.fetchQuery({queryKey:['auth','preferences'],queryFn:()=>authApi.getPreferences()}).catch(()=>{});});
  await waitFor(()=>expect(result.current.user).toBeNull());
  expect(client.getQueryData(['auth','preferences'])).toBeUndefined();
});

it('review: retrying login after CSRF_INVALID must not keep sending the stale token',async()=>{
  let csrfRequests=0;
  globalThis.fetch=vi.fn(async(url,options)=>{
    if(String(url).endsWith('/auth/csrf'))return json({token:++csrfRequests===1?'stale':'fresh',headerName:'X-CSRF-TOKEN'});
    return new Headers(options?.headers).get('X-CSRF-TOKEN')==='stale'?problem(403,'CSRF_INVALID'):json(user);
  });
  await expect(authApi.login({username:'account-a',password:'fixture'})).rejects.toThrow();
  await expect(authApi.login({username:'account-a',password:'fixture'})).resolves.toMatchObject({id:'A'});
});
