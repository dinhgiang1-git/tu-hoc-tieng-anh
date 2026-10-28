import React, { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, XCircle, HelpCircle, Check, ArrowRight, Loader2 } from "lucide-react";
import { Link } from "react-router-dom";
import type { MistakeItemResponse, QuestionCheckResponse } from "../api/review.api";
import { reviewApi, reviewKeys } from "../api/review.api";

interface ReviewMistakeCardProps {
  mistake: MistakeItemResponse;
  enrollmentId: string;
}

export const ReviewMistakeCard: React.FC<ReviewMistakeCardProps> = ({
  mistake,
  enrollmentId,
}) => {
  const queryClient = useQueryClient();
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [checkResult, setCheckResult] = useState<QuestionCheckResponse | null>(null);

  const checkMutation = useMutation({
    mutationFn: (optionId: string) =>
      reviewApi.checkMistakeQuestion(enrollmentId, mistake.questionId, {
        selectedOptionId: optionId,
      }),
    onSuccess: (data) => {
      setCheckResult(data);
      if (data.resolved) {
        queryClient.invalidateQueries({ queryKey: reviewKeys.summary(enrollmentId) });
      }
    },
  });

  const isResolved = checkResult ? checkResult.resolved : mistake.resolved;

  const handleCheck = () => {
    if (!selectedOptionId || checkMutation.isPending) return;
    checkMutation.mutate(selectedOptionId);
  };

  const handleSelectOption = (optId: string) => {
    if (checkMutation.isPending) return;
    setSelectedOptionId(optId);
  };

  return (
    <div
      data-testid={`mistake-card-${mistake.questionId}`}
      className="p-5 rounded-xl border border-border bg-surface shadow-sm space-y-4 transition-all"
    >
      {/* Header: Status badge & Source lesson */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/60 pb-3">
        <div className="flex items-center gap-2">
          {isResolved ? (
            <span
              data-testid={`mistake-status-${mistake.questionId}`}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              Đã giải quyết
            </span>
          ) : (
            <span
              data-testid={`mistake-status-${mistake.questionId}`}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-300 dark:border-amber-800"
            >
              <HelpCircle className="w-3.5 h-3.5" />
              Cần ôn lại
            </span>
          )}

          {mistake.activityTitle && (
            <span className="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded">
              {mistake.activityTitle}
            </span>
          )}
        </div>

        {mistake.lessonId && (
          <Link
            to={`/enrollments/${enrollmentId}/lessons/${mistake.lessonId}`}
            className="inline-flex items-center gap-1 text-xs text-primary hover:underline font-medium"
          >
            <span>{mistake.lessonTitle || "Về bài học"}</span>
            <ArrowRight className="w-3 h-3" />
          </Link>
        )}
      </div>

      {/* Prompt */}
      <div className="text-base font-semibold text-foreground">
        {mistake.prompt}
      </div>

      {/* Options */}
      <div className="space-y-2.5">
        {mistake.options.map((opt) => {
          const isSelected = selectedOptionId === opt.id;
          const isCorrectAnswer = checkResult?.correctOptionId === opt.id;
          const isWrongSelection = checkResult && !checkResult.isCorrect && isSelected;

          let optionStyle =
            "border-border bg-surface hover:bg-surface-hover hover:border-border-hover text-foreground";
          if (checkResult) {
            if (isCorrectAnswer) {
              optionStyle =
                "border-emerald-500 bg-emerald-50/60 dark:bg-emerald-950/30 text-emerald-900 dark:text-emerald-200 font-medium";
            } else if (isWrongSelection) {
              optionStyle =
                "border-rose-500 bg-rose-50/60 dark:bg-rose-950/30 text-rose-900 dark:text-rose-200";
            } else {
              optionStyle = "border-border/60 bg-muted/30 text-muted-foreground opacity-70";
            }
          } else if (isSelected) {
            optionStyle =
              "border-primary bg-primary/5 text-primary font-medium ring-1 ring-primary";
          }

          return (
            <label
              key={opt.id}
              onClick={() => handleSelectOption(opt.id)}
              className={`flex items-center justify-between p-3.5 rounded-lg border cursor-pointer transition-all ${optionStyle}`}
            >
              <div className="flex items-center gap-3">
                <input
                  type="radio"
                  name={`question-${mistake.questionId}`}
                  checked={isSelected}
                  onChange={() => handleSelectOption(opt.id)}
                  disabled={checkMutation.isPending}
                  className="w-4 h-4 text-primary border-border focus:ring-primary"
                />
                <span className="text-sm">{opt.content}</span>
              </div>

              {checkResult && isCorrectAnswer && (
                <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              )}
              {checkResult && isWrongSelection && (
                <XCircle className="w-4 h-4 text-rose-600 dark:text-rose-400" />
              )}
            </label>
          );
        })}
      </div>

      {/* Action and feedback */}
      <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <button
          type="button"
          onClick={handleCheck}
          disabled={!selectedOptionId || checkMutation.isPending}
          data-testid={`check-button-${mistake.questionId}`}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-primary text-primary-foreground hover:bg-primary-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {checkMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
          <span>Kiểm tra đáp án</span>
        </button>

        {checkResult && (
          <div className="text-sm font-medium">
            {checkResult.isCorrect ? (
              <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4" />
                Chính xác! Câu hỏi đã được giải quyết.
              </span>
            ) : (
              <span className="text-rose-600 dark:text-rose-400 flex items-center gap-1.5">
                <XCircle className="w-4 h-4" />
                Chưa chính xác. Hãy đọc giải thích bên dưới.
              </span>
            )}
          </div>
        )}
      </div>

      {/* Explanation Banner */}
      {checkResult && checkResult.explanation && (
        <div
          data-testid={`mistake-explanation-${mistake.questionId}`}
          className="p-3.5 rounded-lg bg-muted/60 border border-border text-xs text-muted-foreground space-y-1"
        >
          <div className="font-semibold text-foreground">Giải thích chi tiết:</div>
          <p>{checkResult.explanation}</p>
        </div>
      )}
    </div>
  );
};
