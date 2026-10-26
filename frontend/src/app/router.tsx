import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "@/features/auth/pages/LoginPage";
import { TodayPage } from "@/features/learning/pages/TodayPage";
import { LessonWorkspacePage } from "@/features/learning/pages/LessonWorkspacePage";
import { PreferencesPage } from "@/features/preferences/pages/PreferencesPage";
import { AppShell } from "./layouts/AppShell";
import { CurriculaPage } from "@/features/curricula/pages/CurriculaPage";
import { CurriculumDetailPage } from "@/features/curricula/pages/CurriculumDetailPage";
import { SchedulePage } from "@/features/schedule/pages/SchedulePage";

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
        element: <LessonWorkspacePage />,
      },
      {
        path: "schedule",
        element: <SchedulePage />,
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
