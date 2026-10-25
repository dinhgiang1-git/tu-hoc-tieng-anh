import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, Calendar, Clock, CheckCircle2 } from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button-variants";
import { ApiError } from "@/lib/api/api-error";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";
import { ActivityProgressPanel } from "../components/ActivityProgressPanel";

export function LessonSummaryPage() {
  const { enrollmentId = "", lessonId = "" } = useParams();

  const lessonSummaryQuery = useQuery({
    queryKey: enrollmentKeys.lessonSummary(enrollmentId, lessonId),
    queryFn: ({ signal }) =>
      enrollmentApi.getLessonSummary(enrollmentId, lessonId, signal),
    enabled: Boolean(enrollmentId && lessonId),
  });

  const enrollmentDetailQuery = useQuery({
    queryKey: enrollmentKeys.detail(enrollmentId),
    queryFn: ({ signal }) => enrollmentApi.getDetail(enrollmentId, signal),
    enabled: Boolean(enrollmentId),
  });

  if (lessonSummaryQuery.isPending || enrollmentDetailQuery.isPending) {
    return (
      <div className="space-y-4" role="status" aria-label="Đang tải thông tin bài học">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full rounded-xl" />
        <Skeleton className="h-48 w-full rounded-xl" />
      </div>
    );
  }

  if (lessonSummaryQuery.isError) {
    const error = lessonSummaryQuery.error;
    const isNotFound = error instanceof ApiError && error.isNotFound();
    return (
      <div className="space-y-6">
        <Link
          to="/today"
          className="inline-flex items-center gap-2 text-sm text-primary hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại trang Hôm nay
        </Link>
        <ErrorState
          message={
            isNotFound
              ? "Không tìm thấy buổi học hoặc buổi học không thuộc lộ trình đã đăng ký."
              : "Không thể tải thông tin bài học. Vui lòng thử lại sau."
          }
          onRetry={isNotFound ? undefined : () => void lessonSummaryQuery.refetch()}
        />
        <div className="flex justify-center">
          <Link
            to="/today"
            className={buttonVariants({ variant: "outline" })}
          >
            Về trang Hôm nay
          </Link>
        </div>
      </div>
    );
  }

  if (enrollmentDetailQuery.isError && !enrollmentDetailQuery.data) {
    return (
      <div className="space-y-6">
        <Link
          to="/today"
          className="inline-flex items-center gap-2 text-sm text-primary hover:underline"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại trang Hôm nay
        </Link>
        <ErrorState
          message="Không thể tải thông tin lộ trình học. Vui lòng thử lại sau."
          onRetry={() => void enrollmentDetailQuery.refetch()}
        />
        <div className="flex justify-center">
          <Link
            to="/today"
            className={buttonVariants({ variant: "outline" })}
          >
            Về trang Hôm nay
          </Link>
        </div>
      </div>
    );
  }

  const summary = lessonSummaryQuery.data;
  const enrollmentStatus = enrollmentDetailQuery.data?.status;
  const isReadOnly =
    enrollmentDetailQuery.isError ||
    enrollmentDetailQuery.isFetching ||
    !enrollmentStatus ||
    enrollmentStatus === "PAUSED" ||
    enrollmentStatus === "ABANDONED";

  return (
    <div className="space-y-6">
      <Link
        to="/today"
        className="inline-flex items-center gap-2 text-sm text-primary hover:underline"
      >
        <ArrowLeft className="h-4 w-4" />
        Quay lại trang Hôm nay
      </Link>

      <PageHeader
        title={summary.lessonTitle}
        description={`Lộ trình: ${summary.curriculumTitle}`}
      />

      {/* Lesson Details Card */}
      <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-semibold text-primary">
            <BookOpen className="h-4 w-4" />
            <span>Thông tin buổi học</span>
          </div>
          {summary.isRequired ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
              <CheckCircle2 className="h-3 w-3" />
              Bắt buộc
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
              Tự chọn
            </span>
          )}
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          {summary.plannedDate && (
            <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/40 border border-border">
              <Calendar className="h-5 w-5 text-muted-foreground mt-0.5" />
              <div>
                <span className="text-xs text-muted-foreground block">Ngày học theo lịch</span>
                <span className="text-sm font-semibold text-foreground">
                  {summary.plannedDate}
                </span>
              </div>
            </div>
          )}

          <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/40 border border-border">
            <Clock className="h-5 w-5 text-muted-foreground mt-0.5" />
            <div>
              <span className="text-xs text-muted-foreground block">
                Thời lượng ước tính
              </span>
              <span className="text-sm font-semibold text-foreground">
                {summary.estimatedMinutes} phút
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Activity Progress Panel */}
      {enrollmentDetailQuery.isError && (
        <ErrorState
          message="Không thể đồng bộ trạng thái lộ trình. Câu trả lời đang nhập vẫn được giữ nguyên."
          onRetry={() => void enrollmentDetailQuery.refetch()}
        />
      )}
      <ActivityProgressPanel
        enrollmentId={enrollmentId}
        lessonId={lessonId}
        isEnrollmentReadOnly={isReadOnly}
      />
    </div>
  );
}
