import { useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  RotateCcw,
  Bookmark,
  BookOpen,
  Filter,
  Sparkles,
} from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/ui/skeleton";
import { authApi, authKeys } from "@/features/auth";
import {
  enrollmentApi,
  enrollmentKeys,
} from "@/features/enrollment/api/enrollment.api";
import { reviewApi, reviewKeys } from "../api/review.api";
import { ReviewMistakeCard } from "../components/ReviewMistakeCard";
import { ReviewBookmarkedCard } from "../components/ReviewBookmarkedCard";

export function ReviewPage() {
  const queryClient = useQueryClient();

  // 1. Fetch user preferences (for default focused enrollment)
  const preferencesQuery = useQuery({
    queryKey: authKeys.preferences(),
    queryFn: ({ signal }) => authApi.getPreferences(signal),
  });

  // 2. Fetch all user enrollments (for switcher dropdown)
  const myEnrollmentsQuery = useQuery({
    queryKey: enrollmentKeys.my(),
    queryFn: ({ signal }) => enrollmentApi.myEnrollments(signal),
  });

  const enrollments = useMemo(
    () => myEnrollmentsQuery.data ?? [],
    [myEnrollmentsQuery.data],
  );
  const focusedEnrollmentId = preferencesQuery.data?.focusedEnrollmentId ?? null;

  // Local state for selected enrollment in review view
  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<string | null>(null);

  // Active tab: 'mistakes' | 'bookmarks'
  const [activeTab, setActiveTab] = useState<"mistakes" | "bookmarks">("mistakes");

  // Mistakes filter: 'unresolved' | 'all'
  const [mistakeFilter, setMistakeFilter] = useState<"unresolved" | "all">("unresolved");

  // Resolve target enrollment ID
  const effectiveEnrollmentId = useMemo(() => {
    if (selectedEnrollmentId) return selectedEnrollmentId;
    if (
      focusedEnrollmentId &&
      enrollments.some((e) => e.id === focusedEnrollmentId)
    ) {
      return focusedEnrollmentId;
    }
    return enrollments[0]?.id ?? null;
  }, [selectedEnrollmentId, focusedEnrollmentId, enrollments]);

  // 3. Fetch review summary
  const reviewSummaryQuery = useQuery({
    queryKey: reviewKeys.summary(effectiveEnrollmentId ?? ""),
    queryFn: ({ signal }) =>
      reviewApi.getReviewSummary(effectiveEnrollmentId!, signal),
    enabled: !!effectiveEnrollmentId,
    staleTime: 0,
    refetchOnMount: "always",
  });

  // Delete bookmark mutation
  const deleteBookmarkMutation = useMutation({
    mutationFn: (bookmarkId: string) => reviewApi.deleteBookmark(bookmarkId),
    onSuccess: () => {
      if (effectiveEnrollmentId) {
        queryClient.invalidateQueries({
          queryKey: reviewKeys.summary(effectiveEnrollmentId),
        });
      }
    },
  });

  const reviewData = reviewSummaryQuery.data;
  const mistakes = useMemo(() => reviewData?.mistakes ?? [], [reviewData?.mistakes]);
  const bookmarkedItems = useMemo(
    () => reviewData?.bookmarkedItems ?? [],
    [reviewData?.bookmarkedItems],
  );

  const unresolvedMistakes = useMemo(
    () => mistakes.filter((m) => !m.resolved),
    [mistakes],
  );

  const displayedMistakes = useMemo(() => {
    if (mistakeFilter === "unresolved") {
      return unresolvedMistakes;
    }
    return mistakes;
  }, [mistakeFilter, unresolvedMistakes, mistakes]);

  // Loading state
  const isLoading =
    preferencesQuery.isLoading ||
    myEnrollmentsQuery.isLoading ||
    (!!effectiveEnrollmentId && reviewSummaryQuery.isLoading);

  // Error state
  const isError =
    preferencesQuery.isError ||
    myEnrollmentsQuery.isError ||
    (!!effectiveEnrollmentId && reviewSummaryQuery.isError);

  const handleRetry = () => {
    preferencesQuery.refetch();
    myEnrollmentsQuery.refetch();
    if (effectiveEnrollmentId) {
      reviewSummaryQuery.refetch();
    }
  };

  return (
    <div className="space-y-6">
      {/* Header and enrollment switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <PageHeader
          title="Ôn tập & Khắc phục câu sai"
          description="Củng cố kiến thức qua các câu hỏi làm sai và bài học đã đánh dấu"
        />

        {enrollments.length > 1 && (
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <label
              htmlFor="review-enrollment-select"
              className="text-xs text-muted-foreground whitespace-nowrap"
            >
              Lộ trình:
            </label>
            <select
              id="review-enrollment-select"
              data-testid="review-enrollment-select"
              value={effectiveEnrollmentId ?? ""}
              onChange={(e) => setSelectedEnrollmentId(e.target.value)}
              className="px-3 py-1.5 text-xs font-medium rounded-lg border border-border bg-surface text-foreground shadow-sm focus:outline-none focus:ring-1 focus:ring-primary"
            >
              {enrollments.map((enr) => (
                <option key={enr.id} value={enr.id}>
                  {enr.curriculumTitle || "Lộ trình học"}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="space-y-4" role="status" aria-label="Đang tải dữ liệu ôn tập">
          <div className="flex gap-3">
            <Skeleton className="h-10 w-40 rounded-lg" />
            <Skeleton className="h-10 w-40 rounded-lg" />
          </div>
          <Skeleton className="h-44 w-full rounded-xl" />
          <Skeleton className="h-44 w-full rounded-xl" />
        </div>
      )}

      {/* Error state */}
      {!isLoading && isError && (
        <ErrorState
          message="Không thể tải nội dung ôn tập. Đã xảy ra lỗi khi kết nối tới máy chủ. Vui lòng thử lại."
          onRetry={handleRetry}
        />
      )}

      {/* No enrollment registered */}
      {!isLoading && !isError && enrollments.length === 0 && (
        <EmptyState
          icon={BookOpen}
          title="Chưa có lộ trình học nào"
          description="Hãy đăng ký một lộ trình học để bắt đầu học và sử dụng tính năng ôn tập."
          action={
            <Link
              to="/curricula"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary-hover transition-colors"
            >
              Khám phá lộ trình
            </Link>
          }
        />
      )}

      {/* Main Review Content */}
      {!isLoading && !isError && enrollments.length > 0 && (
        <div className="space-y-5">
          {/* Navigation Tabs */}
          <div className="flex border-b border-border">
            <button
              type="button"
              data-testid="tab-mistakes"
              onClick={() => setActiveTab("mistakes")}
              className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                activeTab === "mistakes"
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground hover:border-border"
              }`}
            >
              <RotateCcw className="w-4 h-4" />
              <span>Câu hỏi cần ôn lại</span>
              {unresolvedMistakes.length > 0 ? (
                <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                  {unresolvedMistakes.length}
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-muted text-muted-foreground">
                  0
                </span>
              )}
            </button>

            <button
              type="button"
              data-testid="tab-bookmarks"
              onClick={() => setActiveTab("bookmarks")}
              className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                activeTab === "bookmarks"
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground hover:border-border"
              }`}
            >
              <Bookmark className="w-4 h-4" />
              <span>Bài học đã đánh dấu</span>
              <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-muted text-muted-foreground">
                {bookmarkedItems.length}
              </span>
            </button>
          </div>

          {/* Tab 1: Mistakes */}
          {activeTab === "mistakes" && (
            <div className="space-y-4" data-testid="mistakes-section">
              {/* Filter controls */}
              {mistakes.length > 0 && (
                <div className="flex items-center justify-between gap-3 text-xs text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <Filter className="w-3.5 h-3.5" />
                    <span>Lọc hiển thị:</span>
                    <button
                      type="button"
                      data-testid="filter-unresolved"
                      onClick={() => setMistakeFilter("unresolved")}
                      className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                        mistakeFilter === "unresolved"
                          ? "bg-primary/10 text-primary"
                          : "hover:bg-muted text-muted-foreground"
                      }`}
                    >
                      Chưa giải quyết ({unresolvedMistakes.length})
                    </button>
                    <button
                      type="button"
                      data-testid="filter-all"
                      onClick={() => setMistakeFilter("all")}
                      className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                        mistakeFilter === "all"
                          ? "bg-primary/10 text-primary"
                          : "hover:bg-muted text-muted-foreground"
                      }`}
                    >
                      Tất cả ({mistakes.length})
                    </button>
                  </div>
                </div>
              )}

              {/* List of mistakes */}
              {displayedMistakes.length > 0 ? (
                <div className="space-y-4">
                  {displayedMistakes.map((mistake) => (
                    <ReviewMistakeCard
                      key={mistake.questionId}
                      mistake={mistake}
                      enrollmentId={effectiveEnrollmentId!}
                    />
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={Sparkles}
                  title={
                    mistakes.length === 0
                      ? "Không có câu hỏi nào cần ôn lại"
                      : "Tất cả câu sai đã được giải quyết!"
                  }
                  description={
                    mistakes.length === 0
                      ? "Khi làm các bài kiểm tra trắc nghiệm, các câu trả lời chưa đúng sẽ tự động xuất hiện ở đây để bạn ôn luyện."
                      : "Chúc mừng bạn đã trả lời đúng tất cả các câu hỏi trắc nghiệm đã làm sai trước đó."
                  }
                />
              )}
            </div>
          )}

          {/* Tab 2: Bookmarks */}
          {activeTab === "bookmarks" && (
            <div className="space-y-4" data-testid="bookmarks-section">
              {bookmarkedItems.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {bookmarkedItems.map((bookmark) => (
                    <ReviewBookmarkedCard
                      key={bookmark.id}
                      bookmark={bookmark}
                      onDelete={(id) => deleteBookmarkMutation.mutate(id)}
                      isDeleting={deleteBookmarkMutation.isPending}
                    />
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={Bookmark}
                  title="Chưa có bài học nào được đánh dấu"
                  description="Trong lúc học bài, bạn có thể nhấn nút 'Đánh dấu ôn tập' để lưu lại bài học hoặc nhiệm vụ quan trọng vào danh sách này."
                />
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
