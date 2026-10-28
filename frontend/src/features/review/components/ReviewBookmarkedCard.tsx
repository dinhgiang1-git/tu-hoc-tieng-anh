import React from "react";
import { Link } from "react-router-dom";
import { Bookmark, ExternalLink, Trash2 } from "lucide-react";
import type { BookmarkResponse } from "../api/review.api";

interface ReviewBookmarkedCardProps {
  bookmark: BookmarkResponse;
  onDelete: (id: string) => void;
  isDeleting?: boolean;
}

export const ReviewBookmarkedCard: React.FC<ReviewBookmarkedCardProps> = ({
  bookmark,
  onDelete,
  isDeleting,
}) => {
  return (
    <div
      data-testid={`bookmarked-item-${bookmark.id}`}
      className="p-5 rounded-xl border border-border bg-surface shadow-sm space-y-3 hover:border-primary/40 transition-colors"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1 rounded bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400">
              <Bookmark className="w-4 h-4 fill-amber-600/20" />
            </span>
            <h3 className="font-semibold text-foreground text-base">
              {bookmark.lessonTitle}
            </h3>
          </div>

          {bookmark.activityTitle && (
            <div className="flex items-center gap-2 pt-1">
              <span className="text-xs font-medium px-2 py-0.5 rounded bg-muted text-muted-foreground">
                {bookmark.activityType || "Nhiệm vụ"}
              </span>
              <span className="text-sm text-muted-foreground">
                {bookmark.activityTitle}
              </span>
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={() => onDelete(bookmark.id)}
          disabled={isDeleting}
          data-testid={`delete-bookmark-${bookmark.id}`}
          aria-label="Xóa đánh dấu"
          className="text-muted-foreground hover:text-rose-600 p-1.5 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {bookmark.note && (
        <div className="text-sm bg-muted/40 p-2.5 rounded-lg text-foreground italic border-l-2 border-amber-400">
          "{bookmark.note}"
        </div>
      )}

      <div className="flex items-center justify-between pt-2 border-t border-border/60 text-xs text-muted-foreground">
        <span>Đã lưu vào {new Date(bookmark.createdAt).toLocaleDateString("vi-VN")}</span>

        <Link
          to={`/enrollments/${bookmark.enrollmentId}/lessons/${bookmark.lessonId}`}
          className="inline-flex items-center gap-1.5 text-primary hover:underline font-medium"
        >
          <span>Mở bài học</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </Link>
      </div>
    </div>
  );
};
