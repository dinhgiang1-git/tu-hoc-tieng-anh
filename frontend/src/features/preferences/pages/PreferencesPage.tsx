import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/shared/PageHeader";
import { SubmitButton } from "@/components/shared/SubmitButton";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api-error";
import { authApi, authKeys } from "@/features/auth";

export function PreferencesPage() {
  const queryClient = useQueryClient();
  const [draftMinutes, setDraftMinutes] = useState<string | null>(null);

  const {
    data: preferences,
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
      setDraftMinutes(null);
    },
  });

  const targetMinutesInput = draftMinutes ?? String(preferences?.dailyTargetMinutes ?? "");
  const minutes = Number(targetMinutesInput);
  const isValid = targetMinutesInput.trim() !== "" && Number.isInteger(minutes)
    && minutes >= 10 && minutes <= 180;
  const isDisabled = !preferences || isPreferencesError || updatePreferencesMutation.isPending;
  const canSave = !isDisabled && isValid && minutes !== preferences.dailyTargetMinutes;

  const handleChange = (value: string) => {
    setDraftMinutes(value);
    updatePreferencesMutation.reset();
  };

  const handleSaveGoal = (e: React.FormEvent) => {
    e.preventDefault();
    if (canSave) updatePreferencesMutation.mutate(minutes);
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Tùy chọn học tập" />

      <section aria-labelledby="goal-heading" className="max-w-2xl space-y-6 rounded-xl border border-border bg-card p-6 shadow-sm">
        <h2 id="goal-heading" className="text-lg font-semibold">Mục tiêu học tập</h2>

        {isPreferencesError && (
          <div role="alert" className="flex items-center gap-2 rounded-lg border border-destructive/20 bg-destructive/10 p-3 text-sm text-destructive">
            <AlertCircle aria-hidden="true" className="h-4 w-4 shrink-0" />
            <span>
              {preferencesError instanceof ApiError
                ? preferencesError.problem.detail
                : "Không thể tải mục tiêu học tập."}
            </span>
            <Button variant="outline" size="sm" className="ml-auto" onClick={() => void refetchPreferences()}>
              Tải lại
            </Button>
          </div>
        )}

        <form onSubmit={handleSaveGoal} className="space-y-4">
          <Label htmlFor="goal-input" className="block">Thời lượng mỗi ngày</Label>
          <div role="group" aria-label="Chọn nhanh thời lượng" className="flex flex-wrap gap-2">
            {[15, 30, 45, 60].map((preset) => (
              <Button
                key={preset}
                type="button"
                variant={isValid && minutes === preset ? "secondary" : "outline"}
                aria-pressed={isValid && minutes === preset}
                disabled={isDisabled}
                onClick={() => handleChange(String(preset))}
              >
                {preset} phút
              </Button>
            ))}
          </div>

          <div className="flex items-start gap-3">
            <div className="relative w-40">
              <Input
                id="goal-input"
                type="number"
                min={10}
                max={180}
                step={1}
                value={targetMinutesInput}
                onChange={(e) => handleChange(e.target.value)}
                disabled={isDisabled}
                aria-invalid={draftMinutes !== null && !isValid}
                aria-describedby={draftMinutes !== null && !isValid ? "goal-error" : undefined}
                className="h-10 pr-14 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
              />
              <span aria-hidden="true" className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">phút</span>
            </div>
            <SubmitButton className="h-10" pending={updatePreferencesMutation.isPending} disabled={!canSave}>
              Lưu mục tiêu
            </SubmitButton>
          </div>

          {draftMinutes !== null && !isValid && (
            <p id="goal-error" className="text-sm text-destructive">Nhập số phút nguyên từ 10 đến 180.</p>
          )}

          {updatePreferencesMutation.isError && (
            <p role="alert" className="text-sm text-destructive">
              {updatePreferencesMutation.error instanceof ApiError
                ? updatePreferencesMutation.error.problem.detail
                : "Không thể lưu mục tiêu. Vui lòng thử lại."}
            </p>
          )}

          <div role="status" aria-live="polite">
            {updatePreferencesMutation.isSuccess && (
              <p className="flex items-center gap-2 text-sm text-foreground">
                <CheckCircle2 aria-hidden="true" className="h-4 w-4 text-primary" />
                Đã lưu mục tiêu
              </p>
            )}
          </div>
        </form>
      </section>
    </div>
  );
}
