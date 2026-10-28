import type { DayOfWeek } from "../api/enrollment.api";

const DAY_TO_NUMBER: Record<DayOfWeek, number> = {
  SUNDAY: 0,
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
};

export interface EstimatedPlan {
  endDateStr: string;
  totalWeeks: number;
}

export function calculateEstimatedPlan(
  startDateStr: string,
  studyDays: DayOfWeek[],
  totalLessons: number,
): EstimatedPlan | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(startDateStr) || !studyDays?.length ||
      !Number.isSafeInteger(totalLessons) || totalLessons <= 0) {
    return null;
  }
  // Calendar dates use UTC fields so DST cannot add a day or skip a weekday.
  const current = new Date(`${startDateStr}T00:00:00Z`);
  if (isNaN(current.getTime()) || current.toISOString().slice(0, 10) !== startDateStr) {
    return null;
  }
  const startTime = current.getTime();

  const validDayNumbers = new Set(studyDays.map((d) => DAY_TO_NUMBER[d]));
  if ([...validDayNumbers].some((d) => d === undefined)) return null;
  let scheduledCount = 0;
  let lastDate = new Date(current);
  let guard = 0;

  while (scheduledCount < totalLessons && guard < 5000) {
    guard++;
    if (validDayNumbers.has(current.getUTCDay())) {
      scheduledCount++;
      lastDate = new Date(current);
    }
    current.setUTCDate(current.getUTCDate() + 1);
  }

  if (scheduledCount < totalLessons) {
    return null;
  }

  const endDay = String(lastDate.getUTCDate()).padStart(2, "0");
  const endMonth = String(lastDate.getUTCMonth() + 1).padStart(2, "0");
  const endYear = String(lastDate.getUTCFullYear()).padStart(4, "0");
  const endDateStr = `${endDay}/${endMonth}/${endYear}`;

  const diffTime = lastDate.getTime() - startTime;
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  const totalWeeks = Math.ceil(diffDays / 7);

  return { endDateStr, totalWeeks };
}
