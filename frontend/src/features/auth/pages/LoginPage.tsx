import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, BookOpen, Lock, User as UserIcon } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import { useAuth } from "@/features/auth";
import { loginSchema, type LoginFormData } from "../schemas/login.schema";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || "/";
  const sessionExpired = (location.state as { sessionExpired?: boolean })?.sessionExpired;
  const initialError = sessionExpired
    ? "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại."
    : null;

  const [generalError, setGeneralError] = useState<string | null>(initialError);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setGeneralError(null);
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.code === "INVALID_CREDENTIALS") {
          setGeneralError("Tên đăng nhập hoặc mật khẩu không chính xác.");
        } else if (error.isCsrfInvalid()) {
          setGeneralError("Phiên bảo mật đã hết hạn. Vui lòng thử lại.");
        } else {
          setGeneralError(error.problem.detail || "Đã xảy ra lỗi đăng nhập.");
        }
      } else {
        setGeneralError("Không thể kết nối đến máy chủ. Vui lòng thử lại.");
      }
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-12 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-8 rounded-xl border border-border bg-card p-8 shadow-sm">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
            <BookOpen className="h-6 w-6" />
          </div>
          <h2 className="mt-4 text-2xl font-bold tracking-tight text-foreground">
            Tự Học Tiếng Anh
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Đăng nhập để tiếp tục lộ trình học có cấu trúc chuẩn Anh-Anh
          </p>
        </div>

        {generalError && (
          <div
            className="flex items-start gap-3 rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-sm text-destructive"
            role="alert"
          >
            <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
            <span>{generalError}</span>
          </div>
        )}

        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username" className="flex items-center gap-2">
                <UserIcon className="h-4 w-4 text-muted-foreground" />
                Tên đăng nhập
              </Label>
              <Input
                id="username"
                type="text"
                autoComplete="username"
                disabled={isSubmitting}
                aria-invalid={!!errors.username}
                aria-describedby={errors.username ? "username-error" : undefined}
                placeholder="Nhập tên đăng nhập của bạn"
                {...register("username")}
              />
              {errors.username && (
                <p id="username-error" className="text-xs text-destructive">
                  {errors.username.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="flex items-center gap-2">
                <Lock className="h-4 w-4 text-muted-foreground" />
                Mật khẩu
              </Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                disabled={isSubmitting}
                aria-invalid={!!errors.password}
                aria-describedby={errors.password ? "password-error" : undefined}
                placeholder="Nhập mật khẩu"
                {...register("password")}
              />
              {errors.password && (
                <p id="password-error" className="text-xs text-destructive">
                  {errors.password.message}
                </p>
              )}
            </div>
          </div>

          <div>
            <SubmitButton
              pending={isSubmitting}
              className="w-full h-10 text-base font-medium"
            >
              Đăng nhập
            </SubmitButton>
          </div>
        </form>

      </div>
    </div>
  );
}
