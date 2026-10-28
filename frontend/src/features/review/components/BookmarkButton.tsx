import React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bookmark, BookmarkCheck, Loader2 } from "lucide-react";
import { reviewApi, reviewKeys } from "../api/review.api";

interface BookmarkButtonProps {
  enrollmentId: string;
  lessonId: string;
  activityId?: string;
  className?: string;
}

export const BookmarkButton: React.FC<BookmarkButtonProps> = ({
  enrollmentId,
  lessonId,
  activityId,
  className = "",
}) => {
  const queryClient = useQueryClient();

  const { data: reviewData, isLoading } = useQuery({
    queryKey: reviewKeys.summary(enrollmentId),
    queryFn: ({ signal }) => reviewApi.getReviewSummary(enrollmentId, signal),
    enabled: !!enrollmentId,
  });

  const existingBookmark = reviewData?.bookmarkedItems.find(
    (b) =>
      b.lessonId === lessonId &&
      (activityId ? b.activityId === activityId : !b.activityId),
  );
  const isBookmarked = !!existingBookmark;

  const createMutation = useMutation({
    mutationFn: () =>
      reviewApi.createBookmark({
        enrollmentId,
        lessonId,
        activityId,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reviewKeys.summary(enrollmentId) });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (bookmarkId: string) => reviewApi.deleteBookmark(bookmarkId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reviewKeys.summary(enrollmentId) });
    },
  });

  const isPending = createMutation.isPending || deleteMutation.isPending;

  const handleToggle = () => {
    if (isPending || isLoading) return;
    if (isBookmarked && existingBookmark) {
      deleteMutation.mutate(existingBookmark.id);
    } else {
      createMutation.mutate();
    }
  };

  return (
    <button
      type="button"
      onClick={handleToggle}
      disabled={isPending || isLoading}
      data-testid={activityId ? `bookmark-activity-${activityId}` : `bookmark-lesson-${lessonId}`}
      aria-label={isBookmarked ? "Bỏ đánh dấu ôn tập" : "Đánh dấu ôn tập"}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors border ${
        isBookmarked
          ? "bg-amber-50 text-amber-700 border-amber-300 hover:bg-amber-100 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800"
          : "bg-surface text-muted-foreground border-border hover:bg-surface-hover hover:text-foreground"
      } ${className}`}
    >
      {isPending ? (
        <Loader2 className="w-3.5 h-3.5 animate-spin" />
      ) : isBookmarked ? (
        <BookmarkCheck className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400 fill-amber-600/20" />
      ) : (
        <Bookmark className="w-3.5 h-3.5" />
      )}
      <span>{isBookmarked ? "Đã đánh dấu" : "Đánh dấu ôn tập"}</span>
    </button>
  );
};
