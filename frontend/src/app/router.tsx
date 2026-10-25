import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "@/features/auth/pages/LoginPage";
import { HomePage } from "@/features/home/HomePage";
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
        element: <HomePage />,
      },
      {
        path: "curriculum",
        element: <Navigate to="/curricula" replace />,
      },
      { path: "curricula", element: <CurriculaPage /> },
      { path: "curricula/:slug", element: <CurriculumDetailPage /> },
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
        element: <HomePage />,
      },
      {
        path: "*",
        element: <Navigate to="/" replace />,
      },
    ],
  },
]);
