import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  BookOpen,
  Calendar,
  CheckCircle2,
  Clock,
  GraduationCap,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/shared/PageHeader";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys, useAuth } from "@/features/auth";

export function HomePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [targetMinutesInput, setTargetMinutesInput] = useState<string>("");
  const [saveSuccessMessage, setSaveSuccessMessage] = useState<string | null>(null);

  const {
    data: preferences,
    isLoading: isPreferencesLoading,
    isError: isPreferencesError,
    error: preferencesError,
    refetch: refetchPreferences,
  } = useQuery({
    queryKey: authKeys.preferences(),
    queryFn: ({ signal }) => authApi.getPreferences(signal),
  });

  const updatePreferencesMutation = useMutation({
    mutationFn: (newMinutes: number) =>
      authApi.updatePreferences({ dailyTargetMinutes: newMinutes }),
    onSuccess: (updated) => {
      queryClient.setQueryData(authKeys.preferences(), updated);
      setSaveSuccessMessage("Đã cập nhật mục tiêu học tập thành công!");
      setTargetMinutesInput("");
      setTimeout(() => setSaveSuccessMessage(null), 3000);
    },
  });

  const handleSaveGoal = (e: React.FormEvent) => {
    e.preventDefault();
    const minutes = parseInt(targetMinutesInput, 10);
    if (!isNaN(minutes) && minutes >= 10 && minutes <= 180) {
      updatePreferencesMutation.mutate(minutes);
    }
  };

  const displayName = user?.fullName || user?.displayName || user?.username || "Người học";

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Chào mừng, ${displayName}!`}
        description="Bắt đầu vòng học tập hôm nay theo lộ trình chuẩn Anh-Anh (RP)."
      />

      {/* Hero Overview Card */}
      <div className="rounded-xl border border-border bg-gradient-to-br from-primary/5 via-card to-card p-6 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
              <Sparkles className="h-3.5 w-3.5" />
              Lộ trình Thử nghiệm: Phát âm Anh-Anh
            </div>
            <h2 className="text-xl font-bold text-foreground">
              Vòng học cốt lõi sẵn sàng kết nối
            </h2>
            <p className="text-sm text-muted-foreground max-w-xl">
              Học tập theo cấu trúc bài giảng sư phạm có theo dõi tiến độ bền vững và chuẩn phát âm Anh-Anh.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button className="gap-2">
              <BookOpen className="h-4 w-4" />
              Bắt đầu bài học
            </Button>
          </div>
        </div>
      </div>

      {/* Metrics / Status Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-xl border border-border bg-card p-5 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-sm font-medium">Mục tiêu hàng ngày</span>
            <Clock className="h-4 w-4 text-primary" />
          </div>
          {isPreferencesLoading ? (
            <div className="text-2xl font-bold text-muted-foreground animate-pulse">
              Đang tải...
            </div>
          ) : isPreferencesError ? (
            <div className="space-y-1">
              <div className="text-xs text-destructive flex items-center gap-1">
                <AlertCircle className="h-3.5 w-3.5" />
                Lỗi tải mục tiêu
              </div>
              <Button
                variant="outline"
                size="sm"
                className="h-6 text-[11px] px-2"
                onClick={() => refetchPreferences()}
              >
                Thử lại
              </Button>
            </div>
          ) : (
            <div className="text-2xl font-bold text-foreground">
              {preferences?.dailyTargetMinutes} phút
            </div>
          )}
          <p className="text-xs text-muted-foreground">
            Khoảng hợp lệ: 10 – 180 phút/ngày
          </p>
        </div>

        <div className="rounded-xl border border-border bg-card p-5 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-sm font-medium">Lịch học tuần</span>
            <Calendar className="h-4 w-4 text-primary" />
          </div>
          <div className="text-2xl font-bold text-foreground">T2, T4, T6</div>
          <p className="text-xs text-muted-foreground">
            Mặc định 3 buổi/tuần (Pilot 1)
          </p>
        </div>

        <div className="rounded-xl border border-border bg-card p-5 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-sm font-medium">Chuẩn phát âm</span>
            <GraduationCap className="h-4 w-4 text-primary" />
          </div>
          <div className="text-2xl font-bold text-foreground">Anh - Anh (RP)</div>
          <p className="text-xs text-muted-foreground">
            Non-rhotic & IPA Anh chuẩn
          </p>
        </div>
      </div>

      {/* Target Minutes Interactive Setting */}
      <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
        <div className="space-y-1">
          <h3 className="text-base font-semibold text-foreground">
            Điều chỉnh mục tiêu học tập hàng ngày
          </h3>
          <p className="text-xs text-muted-foreground">
            Cập nhật thời lượng học tập mỗi ngày phù hợp với quỹ thời gian của bạn.
          </p>
        </div>

        {saveSuccessMessage && (
          <div className="flex items-center gap-2 rounded-lg bg-success/10 border border-success/20 p-3 text-sm text-success">
            <CheckCircle2 className="h-4 w-4" />
            <span>{saveSuccessMessage}</span>
          </div>
        )}

        {updatePreferencesMutation.isError && (
          <div className="flex items-center gap-2 rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-sm text-destructive">
            <AlertCircle className="h-4 w-4" />
            <span>
              {updatePreferencesMutation.error instanceof ApiError
                ? updatePreferencesMutation.error.problem.detail
                : "Không thể lưu mục tiêu học tập. Vui lòng thử lại."}
            </span>
          </div>
        )}

        {isPreferencesError && (
          <div className="flex items-center gap-2 rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-sm text-destructive">
            <AlertCircle className="h-4 w-4" />
            <span>
              {preferencesError instanceof ApiError
                ? preferencesError.problem.detail
                : "Không thể lấy thông tin mục tiêu học tập."}
            </span>
            <Button
              variant="outline"
              size="sm"
              className="ml-auto h-7 text-xs"
              onClick={() => refetchPreferences()}
            >
              Tải lại
            </Button>
          </div>
        )}

        <form onSubmit={handleSaveGoal} className="flex flex-col sm:flex-row gap-3 max-w-md">
          <div className="flex-1 space-y-1">
            <Label htmlFor="goal-input" className="sr-only">
              Số phút mục tiêu
            </Label>
            <Input
              id="goal-input"
              type="number"
              min={10}
              max={180}
              placeholder={
                preferences?.dailyTargetMinutes
                  ? `Hiện tại: ${preferences.dailyTargetMinutes} phút`
                  : "Nhập số phút (10 - 180)"
              }
              value={targetMinutesInput}
              onChange={(e) => setTargetMinutesInput(e.target.value)}
              disabled={updatePreferencesMutation.isPending || isPreferencesLoading}
            />
          </div>
          <SubmitButton
            pending={updatePreferencesMutation.isPending}
            disabled={
              !targetMinutesInput ||
              parseInt(targetMinutesInput, 10) < 10 ||
              parseInt(targetMinutesInput, 10) > 180 ||
              isPreferencesLoading
            }
          >
            Lưu mục tiêu
          </SubmitButton>
        </form>
      </div>
    </div>
  );
}
