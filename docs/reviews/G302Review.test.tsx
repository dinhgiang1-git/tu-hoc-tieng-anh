import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { ActivityProgressPanel } from "../components/ActivityProgressPanel";
import { learningProgressApi } from "../api/learning-progress.api";

it("review: cannot send another mutation with stale version while refresh is pending", async () => {
  vi.spyOn(learningProgressApi,"getLessonActivities").mockResolvedValueOnce({lessonId:"l",enrollmentVersion:1,
    activities:["a","b"].map(id=>({id,title:id,type:"READING" as const,required:true,completionPolicy:"MANUAL_CHECK" as const,status:"NOT_STARTED" as const,responseText:null}))
  }).mockImplementation(()=>new Promise(()=>{}));
  const update = vi.spyOn(learningProgressApi,"updateProgress").mockResolvedValue({activityId:"a",status:"COMPLETED",enrollmentVersion:2,completedRequiredActivities:1,totalRequiredActivities:2,progressPercent:50});
  const client = new QueryClient({defaultOptions:{queries:{retry:false}}});
  render(<QueryClientProvider client={client}><MemoryRouter><ActivityProgressPanel enrollmentId="e" lessonId="l" /></MemoryRouter></QueryClientProvider>);
  fireEvent.click(await screen.findByTestId("complete-button-a"));
  await waitFor(()=>expect(update).toHaveBeenCalledTimes(1));
  await waitFor(()=>expect(screen.getByTestId("complete-button-b")).toBeDisabled());
});
