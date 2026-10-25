import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, Clock } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/shared/PageHeader";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys } from "@/features/auth";

export function PreferencesPage() {
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

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tùy chọn học tập"
        description="Quản lý mục tiêu thời gian và tùy chọn cá nhân cho việc tự học."
      />

      <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
        <div className="space-y-1">
          <h3 className="text-base font-semibold text-foreground flex items-center gap-2">
            <Clock className="h-4 w-4 text-primary" />
            Mục tiêu thời lượng học tập mỗi ngày
          </h3>
          <p className="text-xs text-muted-foreground">
            Đặt thời lượng bạn mong muốn dành ra để học phát âm tiếng Anh mỗi ngày (10 – 180 phút).
          </p>
        </div>

        {saveSuccessMessage && (
          <div className="flex items-center gap-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 p-3 text-sm text-emerald-600 dark:text-emerald-400">
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
              onClick={() => void refetchPreferences()}
            >
              Tải lại
            </Button>
          </div>
        )}

        <form onSubmit={handleSaveGoal} className="flex flex-col sm:flex-row gap-3 max-w-md pt-2">
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
