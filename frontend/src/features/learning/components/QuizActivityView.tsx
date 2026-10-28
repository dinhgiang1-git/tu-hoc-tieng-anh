import { useEffect, useState } from "react";
import {
  AlertCircle,
  Award,
  CheckCircle2,
  HelpCircle,
  Loader2,
  RefreshCw,
  RotateCcw,
  Send,
  XCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/api-error";
import type {
  QuizDetailResponse,
  QuizResultResponse,
} from "../api/quiz.api";
import { quizApi } from "../api/quiz.api";
import type { ActivityWorkspaceDto } from "../api/learning-workspace.api";
import { queryClient } from "@/app/query-client";

interface QuizActivityViewProps {
  activity: ActivityWorkspaceDto;
  enrollmentId: string;
  enrollmentVersion: number;
  onVersionUpdate: (newVersion: number) => void | Promise<void>;
  onActivityUpdated: (updatedActivity: Partial<ActivityWorkspaceDto>) => void;
  onRefreshWorkspace?: () => Promise<void>;
  isReadOnly?: boolean;
}

export function QuizActivityView({
  activity,
  enrollmentId,
  enrollmentVersion,
  onVersionUpdate,
  onActivityUpdated,
  onRefreshWorkspace,
  isReadOnly = false,
}: QuizActivityViewProps) {
  const [quizData, setQuizData] = useState<QuizDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Selected options: map of questionId -> selectedOptionId
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isConflict, setIsConflict] = useState(false);

  // Latest result from submission
  const [result, setResult] = useState<QuizResultResponse | null>(null);

  // Fetch quiz detail
  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();

    async function fetchQuiz() {
      setIsLoading(true);
      setFetchError(null);
      try {
        const data = await quizApi.getQuiz(enrollmentId, activity.activityId, controller.signal);
        if (isMounted) {
          setQuizData(data);
          // Reset answers & results when quiz data reloads
          setSelectedAnswers({});
          setResult(null);
        }
      } catch (err: unknown) {
        if (isMounted && !(err instanceof DOMException && err.name === "AbortError")) {
          const message =
            err instanceof ApiError ? err.message : "Không thể tải nội dung bài kiểm tra.";
          setFetchError(message);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void fetchQuiz();

    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [enrollmentId, activity.activityId]);

  const questions = quizData?.questions ?? [];
  const totalQuestions = questions.length;
  const answeredCount = Object.keys(selectedAnswers).length;
  const allAnswered = totalQuestions > 0 && answeredCount === totalQuestions;

  const handleSelectOption = (questionId: string, optionId: string) => {
    if (result !== null || isSubmitting || isReadOnly) return;
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: optionId,
    }));
  };

  const handleSubmit = async () => {
    if (!allAnswered || isSubmitting || isReadOnly) return;

    setIsSubmitting(true);
    setSubmitError(null);
    setIsConflict(false);

    const idempotencyKey = crypto.randomUUID();
    const answersPayload = Object.entries(selectedAnswers).map(([qId, optId]) => ({
      questionId: qId,
      selectedOptionId: optId,
    }));

    try {
      const res = await quizApi.submitQuiz(
        enrollmentId,
        activity.activityId,
        {
          expectedVersion: enrollmentVersion,
          answers: answersPayload,
        },
        idempotencyKey,
      );

      setResult(res);
      if (res.enrollmentVersion) {
        void onVersionUpdate(res.enrollmentVersion);
      }
      onActivityUpdated({
        status: res.activityCompleted ? "COMPLETED" : "IN_PROGRESS",
      });
      void queryClient.invalidateQueries({ queryKey: ["review"] });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setIsConflict(true);
          setSubmitError("Dữ liệu lộ trình đã thay đổi trên thiết bị khác. Vui lòng tải lại.");
        } else {
          setSubmitError(err.message);
        }
      } else {
        setSubmitError("Đã có lỗi xảy ra khi nộp bài. Vui lòng thử lại.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRetry = () => {
    setResult(null);
    setSelectedAnswers({});
    setSubmitError(null);
  };

  if (isLoading) {
    return (
      <div
        data-testid={`quiz-loading-${activity.activityId}`}
        className="flex items-center justify-center py-12 space-x-2 text-sm text-muted-foreground"
      >
        <Loader2 className="h-5 w-5 animate-spin text-primary" />
        <span>Đang tải nội dung trắc nghiệm...</span>
      </div>
    );
  }

  if (fetchError || !quizData) {
    return (
      <div
        data-testid={`quiz-error-${activity.activityId}`}
        className="p-4 rounded-xl border border-destructive/30 bg-destructive/5 text-sm text-destructive space-y-3"
      >
        <div className="flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{fetchError ?? "Không tìm thấy nội dung bài kiểm tra."}</span>
        </div>
        {onRefreshWorkspace && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => void onRefreshWorkspace()}
            className="text-xs border-destructive/30 text-destructive hover:bg-destructive/10"
          >
            <RefreshCw className="h-3 w-3 mr-1.5" />
            Thử lại
          </Button>
        )}
      </div>
    );
  }

  const isPassScorePolicy = quizData.completionPolicy === "PASS_SCORE";
  const passingScorePercentage = quizData.passingScorePercentage;

  // Build a lookup map of question result after submission
  const resultMap = new Map(result?.questionResults.map((qr) => [qr.questionId, qr]));

  return (
    <div
      data-testid={`quiz-activity-${activity.activityId}`}
      className="space-y-6 pt-2"
    >
      {/* Quiz Policy & Past Attempt Banner */}
      <div className="rounded-xl border border-border bg-muted/40 p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2 text-foreground/80">
          <HelpCircle className="h-4 w-4 text-primary shrink-0" />
          <span>
            {isPassScorePolicy ? (
              <>
                Chính sách: <strong className="font-semibold text-foreground">Đạt từ {passingScorePercentage}% trở lên</strong> để hoàn thành nhiệm vụ.
              </>
            ) : (
              <>
                Chính sách: <strong className="font-semibold text-foreground">Hoàn thành khi nộp bài</strong> (ON_SUBMIT).
              </>
            )}
          </span>
        </div>

        {quizData.latestAttempt && !result && (
          <div
            data-testid="quiz-latest-attempt-badge"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-background border border-border text-muted-foreground"
          >
            <Award className="h-3.5 w-3.5 text-primary" />
            <span>
              Lần làm gần nhất:{" "}
              <strong className="text-foreground">
                {quizData.latestAttempt.score}/{quizData.latestAttempt.totalQuestions} ({quizData.latestAttempt.scorePercentage}%)
              </strong>{" "}
              — {quizData.latestAttempt.passed ? (
                <span className="text-emerald-600 dark:text-emerald-400 font-semibold">Đạt</span>
              ) : (
                <span className="text-amber-600 dark:text-amber-400 font-medium">Chưa đạt</span>
              )}
            </span>
          </div>
        )}
      </div>

      {/* Result Summary Banner (Shown after submission) */}
      {result && (
        <div
          data-testid="quiz-result-banner"
          className={`p-4 rounded-xl border text-sm transition-all space-y-3 ${
            result.passed
              ? "bg-emerald-50/80 border-emerald-300 text-emerald-950 dark:bg-emerald-950/30 dark:border-emerald-800 dark:text-emerald-200"
              : "bg-amber-50/80 border-amber-300 text-amber-950 dark:bg-amber-950/30 dark:border-amber-800 dark:text-amber-200"
          }`}
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2.5">
              {result.passed ? (
                <CheckCircle2 className="h-5 w-5 text-emerald-600 dark:text-emerald-400 shrink-0" />
              ) : (
                <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0" />
              )}
              <div>
                <span className="font-bold text-base">
                  {result.passed ? "Chúc mừng! Bạn đã đạt bài kiểm tra." : "Chưa đạt yêu cầu."}
                </span>
                <div className="text-xs text-muted-foreground mt-0.5">
                  Điểm số: <strong className="text-foreground">{result.score}/{result.totalQuestions}</strong> ({result.scorePercentage}%) — Lần nộp #{result.attemptNumber}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 self-start sm:self-auto">
              {result.activityCompleted && (
                <span className="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-200 border border-emerald-300 dark:border-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Nhiệm vụ đã hoàn thành
                </span>
              )}
              <Button
                data-testid="quiz-retry-button"
                variant="outline"
                size="sm"
                onClick={handleRetry}
                className="text-xs h-7 px-2.5 flex items-center gap-1.5"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Làm lại
              </Button>
            </div>
          </div>

          {!result.passed && isPassScorePolicy && (
            <p className="text-xs text-muted-foreground">
              Bạn cần đạt tối thiểu {passingScorePercentage}% để hoàn thành nhiệm vụ này. Lần luyện lại thấp hơn không xóa thành tích đã đạt trước đó.
            </p>
          )}
        </div>
      )}

      {/* Progress Counter (when taking quiz) */}
      {!result && (
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>
            Tiến độ trả lời:{" "}
            <strong className="text-foreground font-semibold">
              {answeredCount}/{totalQuestions} câu
            </strong>
          </span>
          {allAnswered ? (
            <span className="text-emerald-600 dark:text-emerald-400 font-medium flex items-center gap-1">
              <CheckCircle2 className="h-3.5 w-3.5" />
              Đã trả lời hết tất cả câu hỏi
            </span>
          ) : (
            <span className="text-amber-600 dark:text-amber-400 font-medium">
              Còn {totalQuestions - answeredCount} câu chưa chọn
            </span>
          )}
        </div>
      )}

      {/* Questions List */}
      <div className="space-y-5">
        {questions.map((question, qIndex) => {
          const selectedOptionId = selectedAnswers[question.id];
          const questionResult = resultMap.get(question.id);

          return (
            <div
              key={question.id}
              data-testid={`quiz-question-card-${question.id}`}
              className="rounded-xl border border-border bg-card p-4 sm:p-5 shadow-xs space-y-3.5"
            >
              {/* Question Header */}
              <div className="flex items-start gap-2.5">
                <span className="shrink-0 flex items-center justify-center h-6 w-6 rounded-full bg-primary/10 text-primary font-bold text-xs">
                  {qIndex + 1}
                </span>
                <p className="font-semibold text-sm sm:text-base text-foreground leading-snug pt-0.5">
                  {question.prompt}
                </p>
              </div>

              {/* Options */}
              <div className="space-y-2 pt-1 pl-8">
                {question.options.map((option) => {
                  const isSelected = selectedOptionId === option.id;

                  // Post-submission status styling
                  let optionStyles =
                    "border-border bg-background hover:bg-muted/40 hover:border-primary/40 text-foreground";

                  if (result && questionResult) {
                    const isOptionCorrect = option.id === questionResult.correctOptionId;
                    const isOptionSelected = option.id === questionResult.selectedOptionId;

                    if (isOptionCorrect) {
                      optionStyles =
                        "border-emerald-400 bg-emerald-50/80 text-emerald-950 dark:bg-emerald-950/40 dark:border-emerald-600 dark:text-emerald-200 font-medium";
                    } else if (isOptionSelected && !questionResult.isCorrect) {
                      optionStyles =
                        "border-rose-400 bg-rose-50/80 text-rose-950 dark:bg-rose-950/40 dark:border-rose-600 dark:text-rose-200";
                    } else {
                      optionStyles = "border-border/60 bg-muted/20 text-muted-foreground opacity-60";
                    }
                  } else if (isSelected) {
                    optionStyles =
                      "border-primary bg-primary/5 text-foreground ring-1 ring-primary/30";
                  }

                  return (
                    <label
                      key={option.id}
                      data-testid={`quiz-option-${question.id}-${option.id}`}
                      className={`flex items-start gap-3 p-3 rounded-lg border text-xs sm:text-sm cursor-pointer transition ${optionStyles} ${
                        result || isReadOnly ? "cursor-default" : ""
                      }`}
                    >
                      <input
                        type="radio"
                        name={`question-${question.id}`}
                        value={option.id}
                        checked={isSelected}
                        disabled={result !== null || isSubmitting || isReadOnly}
                        onChange={() => handleSelectOption(question.id, option.id)}
                        className="mt-0.5 h-4 w-4 text-primary focus:ring-primary border-muted-foreground/40 shrink-0"
                      />
                      <span className="flex-1 leading-relaxed">{option.content}</span>
                      {result && questionResult && (
                        <>
                          {option.id === questionResult.correctOptionId && (
                            <CheckCircle2 className="h-4 w-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                          )}
                          {option.id === questionResult.selectedOptionId &&
                            !questionResult.isCorrect && (
                              <XCircle className="h-4 w-4 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
                            )}
                        </>
                      )}
                    </label>
                  );
                })}
              </div>

              {/* Question Feedback & Explanation (After submit) */}
              {result && questionResult && (
                <div
                  data-testid={`quiz-explanation-${question.id}`}
                  className="mt-3 pl-8 pt-2"
                >
                  <div className="rounded-lg bg-muted/50 border border-border p-3 text-xs space-y-1.5">
                    <div className="flex items-center gap-1.5 font-semibold">
                      {questionResult.isCorrect ? (
                        <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                          <CheckCircle2 className="h-3.5 w-3.5" /> Đúng
                        </span>
                      ) : (
                        <span className="text-rose-600 dark:text-rose-400 flex items-center gap-1">
                          <XCircle className="h-3.5 w-3.5" /> Chưa đúng
                        </span>
                      )}
                    </div>
                    {questionResult.explanation && (
                      <p className="text-foreground/80 leading-relaxed">
                        <strong className="text-foreground">Giải thích:</strong> {questionResult.explanation}
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Error alert on submit failure */}
      {submitError && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/30 text-xs text-destructive">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{submitError}</span>
          </div>
          {isConflict && onRefreshWorkspace && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void onRefreshWorkspace()}
              className="h-7 text-xs px-2.5 text-destructive border-destructive/30 hover:bg-destructive/10 shrink-0 self-start sm:self-auto"
            >
              <RefreshCw className="h-3 w-3 mr-1" />
              Đồng bộ dữ liệu
            </Button>
          )}
        </div>
      )}

      {/* Submit Action Bar */}
      {!result && (
        <div className="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-border">
          <span className="text-xs text-muted-foreground">
            {allAnswered
              ? "Bạn đã hoàn tất tất cả câu trả lời. Sẵn sàng nộp bài!"
              : "Vui lòng chọn câu trả lời cho tất cả các câu hỏi trước khi nộp."}
          </span>
          <Button
            data-testid="quiz-submit-button"
            variant="default"
            size="default"
            disabled={!allAnswered || isSubmitting || isReadOnly}
            onClick={() => void handleSubmit()}
            className="flex items-center gap-2 text-xs font-semibold px-5"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Đang chấm điểm...
              </>
            ) : (
              <>
                <Send className="h-4 w-4" />
                Nộp bài kiểm tra
              </>
            )}
          </Button>
        </div>
      )}
    </div>
  );
}
