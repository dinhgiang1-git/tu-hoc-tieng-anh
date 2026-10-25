import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "@/features/auth/pages/LoginPage";
import { TodayPage } from "@/features/learning/pages/TodayPage";
import { LessonSummaryPage } from "@/features/learning/pages/LessonSummaryPage";
import { PreferencesPage } from "@/features/preferences/pages/PreferencesPage";
import { AppShell } from "./layouts/AppShell";
import { CurriculaPage } from "@/features/curricula/pages/CurriculaPage";
import { CurriculumDetailPage } from "@/features/curricula/pages/CurriculumDetailPage";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <TodayPage />,
      },
      {
        path: "today",
        element: <TodayPage />,
      },
      {
        path: "curriculum",
        element: <Navigate to="/curricula" replace />,
      },
      { path: "curricula", element: <CurriculaPage /> },
      { path: "curricula/:slug", element: <CurriculumDetailPage /> },
      {
        path: "enrollments/:enrollmentId/lessons/:lessonId",
        element: <LessonSummaryPage />,
      },
      {
        path: "schedule",
        element: (
          <div className="space-y-4">
            <h1 className="text-2xl font-bold">Lịch học Cá nhân</h1>
            <p className="text-muted-foreground">
              Tính năng điều chỉnh lịch học linh hoạt sẽ được triển khai trong Giai đoạn G4.
            </p>
          </div>
        ),
      },
      {
        path: "preferences",
        element: <PreferencesPage />,
      },
      {
        path: "*",
        element: <Navigate to="/" replace />,
      },
    ],
  },
]);
