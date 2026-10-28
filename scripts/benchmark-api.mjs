#!/usr/bin/env node
/**
 * API Performance Benchmark Tool for tu-hoc-tieng-anh
 * Measures response latency (min, avg, p50, p90, p95, max) against core API endpoints
 * Benchmark Budget: p95 < 500ms
 */

import { performance } from 'node:perf_hooks';
import { randomUUID } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080/api/v1';
const BENCHMARK_USERNAME = process.env.BENCHMARK_USER || 'e2e_learner_ux';
const BENCHMARK_PASSWORD = process.env.BENCHMARK_PASSWORD || 'Pass123456!';
const WARMUP_RUNS = 3;
const ITERATIONS = 30;

class BenchmarkClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.cookies = '';
    this.csrfToken = '';
  }

  updateCookies(response) {
    const setCookie = response.headers.get('set-cookie');
    if (setCookie) {
      // Split cookie string while preserving individual cookie directives
      const parts = setCookie.split(/,(?=\s*[^;=]+=[^;=]+)/g);
      const cookieMap = new Map();
      if (this.cookies) {
        this.cookies.split(';').forEach(c => {
          const [k, v] = c.trim().split('=');
          if (k) cookieMap.set(k, v);
        });
      }
      parts.forEach(part => {
        const [pair] = part.split(';');
        const [k, v] = pair.trim().split('=');
        if (k) cookieMap.set(k, v);
      });
      this.cookies = Array.from(cookieMap.entries()).map(([k, v]) => `${k}=${v}`).join('; ');
    }
  }

  async getCsrf() {
    const res = await fetch(`${this.baseUrl}/auth/csrf`, {
      headers: { Cookie: this.cookies }
    });
    this.updateCookies(res);
    const data = await res.json();
    this.csrfToken = data.token;
    return this.csrfToken;
  }

  async login(username, password) {
    await this.getCsrf();
    const res = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': this.csrfToken,
        'Cookie': this.cookies
      },
      body: JSON.stringify({ username, password })
    });
    this.updateCookies(res);
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Login failed for ${username}: ${res.status} ${errText}`);
    }
    await this.getCsrf(); // Refresh token post-login
    return await res.json();
  }

  async request(path, options = {}) {
    const headers = {
      'Cookie': this.cookies,
      ...(this.csrfToken ? { 'X-CSRF-TOKEN': this.csrfToken } : {}),
      ...(options.headers || {})
    };
    return fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers
    });
  }
}

function calculatePercentile(sortedValues, percentile) {
  if (sortedValues.length === 0) return 0;
  const index = Math.ceil((percentile / 100) * sortedValues.length) - 1;
  return sortedValues[Math.max(0, Math.min(index, sortedValues.length - 1))];
}

async function runBenchmark() {
  console.log('='.repeat(75));
  console.log('       API PERFORMANCE BENCHMARK (Task G7-02: p95 < 500ms)');
  console.log('='.repeat(75));
  console.log(`Base URL    : ${BASE_URL}`);
  console.log(`Test User   : ${BENCHMARK_USERNAME}`);
  console.log(`Iterations  : ${ITERATIONS} runs (+ ${WARMUP_RUNS} warmups) per endpoint`);
  console.log(`Date/Time   : ${new Date().toISOString()}\n`);

  const client = new BenchmarkClient(BASE_URL);

  // 1. Authenticate
  console.log('[1/4] Authenticating test learner...');
  const user = await client.login(BENCHMARK_USERNAME, BENCHMARK_PASSWORD);
  console.log(`Logged in as: ${user.fullName || user.username} (ID: ${user.id})\n`);

  // 2. Discover or setup Roadmap A enrollment
  console.log('[2/4] Setting up test context for Roadmap A (26-week curriculum)...');
  const curriculaRes = await client.request('/curricula');
  const curricula = await curriculaRes.json();
  const roadmapA = curricula.find(c => c.slug === 'roadmap-a' || c.title?.includes('Lộ trình A'));
  if (!roadmapA) {
    throw new Error('Roadmap A curriculum not found! Please import Roadmap A first.');
  }

  const revisionId = roadmapA.currentRevisionId || roadmapA.revisionId;
  console.log(`Roadmap A Revision ID: ${revisionId}`);

  // Fetch outline to get lesson IDs
  const outlineRes = await client.request(`/revisions/${revisionId}/outline`);
  const outline = await outlineRes.json();
  const firstPhase = outline.phases[0];
  const firstWeek = firstPhase.weeks[0];
  const firstLesson = firstWeek.lessons[0];
  console.log(`Total phases: ${outline.phases.length}, Total weeks: ${outline.phases.reduce((acc, p) => acc + p.weeks.length, 0)}`);
  console.log(`Target Lesson: "${firstLesson.title}" (ID: ${firstLesson.id})`);

  // Check enrollment
  const myEnrollmentsRes = await client.request('/me/enrollments');
  const myEnrollments = await myEnrollmentsRes.json();
  let enrollment = myEnrollments.find(e => 
    e.curriculumTitle?.includes('Lộ trình A') || 
    e.curriculumTitle?.includes('Roadmap A')
  );

  if (!enrollment) {
    console.log('User not yet enrolled in Roadmap A. Creating enrollment now...');
    const enrollPayload = {
      curriculumId: roadmapA.id,
      revisionId: revisionId,
      startDate: new Date().toISOString().split('T')[0],
      studyDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
      dailyTargetMinutes: 30,
      timeZone: 'Asia/Ho_Chi_Minh'
    };
    const enrollRes = await client.request('/enrollments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': randomUUID()
      },
      body: JSON.stringify(enrollPayload)
    });
    if (!enrollRes.ok) {
      const err = await enrollRes.text();
      throw new Error(`Enrollment creation failed: ${enrollRes.status} ${err}`);
    }
    enrollment = await enrollRes.json();
    console.log(`Enrollment created successfully: ${enrollment.id}`);
  } else {
    console.log(`Using existing active enrollment: ${enrollment.id}`);
  }

  // Focus enrollment on Today page if not already
  await client.request('/me/preferences', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ focusedEnrollmentId: enrollment.id })
  });

  const enrollmentId = enrollment.id;
  const lessonId = firstLesson.id;

  // 3. Define endpoints to benchmark
  const endpoints = [
    { name: 'GET /curricula', path: '/curricula', desc: 'Public Catalog List' },
    { name: 'GET /curricula/roadmap-a', path: '/curricula/roadmap-a', desc: 'Curriculum Detail' },
    { name: `GET /revisions/.../outline (26 wks)`, path: `/revisions/${revisionId}/outline`, desc: 'Full 26-Week Outline (130 lessons)' },
    { name: 'GET /me/today', path: '/me/today', desc: 'Learner Today Dashboard' },
    { name: 'GET /me/enrollments', path: '/me/enrollments', desc: 'Learner Enrollments List' },
    { name: `GET /enrollments/.../detail`, path: `/enrollments/${enrollmentId}`, desc: 'Enrollment Detail' },
    { name: `GET /enrollments/.../lessons/.../summary`, path: `/enrollments/${enrollmentId}/lessons/${lessonId}/summary`, desc: 'Lesson Summary DTO' },
    { name: `GET /enrollments/.../lessons/... (Workspace)`, path: `/enrollments/${enrollmentId}/lessons/${lessonId}`, desc: 'Lesson Workspace (Activities)' },
    { name: `GET /enrollments/.../summary`, path: `/enrollments/${enrollmentId}/summary`, desc: 'Enrollment Progress Summary' },
    { name: `GET /enrollments/.../analytics`, path: `/enrollments/${enrollmentId}/analytics`, desc: 'Weekly Analytics & Streak' },
    { name: `GET /enrollments/.../review`, path: `/enrollments/${enrollmentId}/review`, desc: 'Review Mistakes & Bookmarks' },
    { name: `GET /enrollments/.../schedule`, path: `/enrollments/${enrollmentId}/schedule`, desc: 'Lesson Schedules' }
  ];

  console.log(`\n[3/4] Running warm-up runs (${WARMUP_RUNS} per endpoint)...`);
  for (const ep of endpoints) {
    for (let i = 0; i < WARMUP_RUNS; i++) {
      const res = await client.request(ep.path);
      if (!res.ok) {
        console.warn(`Warmup warning for ${ep.name}: ${res.status}`);
      }
    }
  }
  console.log('Warm-up complete. Starting timed benchmarks...\n');

  // 4. Measure
  console.log(`[4/4] Executing ${ITERATIONS} measurement cycles...`);
  const benchmarkResults = [];

  for (const ep of endpoints) {
    process.stdout.write(`Testing: ${ep.name.padEnd(45)} `);
    const timings = [];
    let errorCount = 0;

    for (let i = 0; i < ITERATIONS; i++) {
      const start = performance.now();
      const res = await client.request(ep.path);
      const elapsed = performance.now() - start;

      if (res.ok) {
        timings.push(elapsed);
      } else {
        errorCount++;
      }
    }

    timings.sort((a, b) => a - b);
    const min = timings[0] || 0;
    const max = timings[timings.length - 1] || 0;
    const avg = timings.reduce((a, b) => a + b, 0) / timings.length || 0;
    const p50 = calculatePercentile(timings, 50);
    const p90 = calculatePercentile(timings, 90);
    const p95 = calculatePercentile(timings, 95);
    const passBudget = p95 < 500 && errorCount === 0;

    benchmarkResults.push({
      endpoint: ep.name,
      description: ep.desc,
      samples: timings.length,
      errors: errorCount,
      min: min.toFixed(1),
      avg: avg.toFixed(1),
      p50: p50.toFixed(1),
      p90: p90.toFixed(1),
      p95: p95.toFixed(1),
      max: max.toFixed(1),
      passed: passBudget
    });

    console.log(`avg: ${avg.toFixed(1)}ms | p95: ${p95.toFixed(1)}ms | [${passBudget ? 'PASS' : 'FAIL'}]`);
  }

  // Display Final Report Table
  console.log('\n' + '='.repeat(105));
  console.log('                                  FINAL PERFORMANCE REPORT TABLE');
  console.log('='.repeat(105));
  console.log(
    'Endpoint'.padEnd(42) +
    'Samples'.padEnd(9) +
    'Min(ms)'.padEnd(9) +
    'Avg(ms)'.padEnd(9) +
    'p50(ms)'.padEnd(9) +
    'p90(ms)'.padEnd(9) +
    'p95(ms)'.padEnd(9) +
    'Max(ms)'.padEnd(9) +
    'Status'
  );
  console.log('-'.repeat(105));

  let allPassed = true;
  for (const r of benchmarkResults) {
    if (!r.passed) allPassed = false;
    console.log(
      r.endpoint.padEnd(42) +
      String(r.samples).padEnd(9) +
      String(r.min).padEnd(9) +
      String(r.avg).padEnd(9) +
      String(r.p50).padEnd(9) +
      String(r.p90).padEnd(9) +
      String(r.p95).padEnd(9) +
      String(r.max).padEnd(9) +
      (r.passed ? 'PASS (<500ms)' : 'FAILED')
    );
  }
  console.log('='.repeat(105));

  // Save report to JSON file
  const reportDir = path.resolve('reports');
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }
  const reportPath = path.join(reportDir, 'api-benchmark-report.json');
  fs.writeFileSync(reportPath, JSON.stringify({
    timestamp: new Date().toISOString(),
    benchmarkUser: BENCHMARK_USERNAME,
    iterationsPerEndpoint: ITERATIONS,
    allPassed,
    budgetThresholdMs: 500,
    results: benchmarkResults
  }, null, 2));
  console.log(`Detailed report exported to: ${reportPath}`);

  if (allPassed) {
    console.log('\n>>> BENCHMARK RESULT: 100% OF ENDPOINTS ACHIEVED p95 < 500ms! (ALL TESTS PASSED) <<<\n');
    process.exit(0);
  } else {
    console.error('\n>>> BENCHMARK RESULT: ONE OR MORE ENDPOINTS EXCEEDED 500ms BUDGET! <<<\n');
    process.exit(1);
  }
}

runBenchmark().catch(err => {
  console.error('\nBenchmark failed with uncaught error:', err);
  process.exit(1);
});
