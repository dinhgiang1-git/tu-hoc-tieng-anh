import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, BookOpen, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/shared/PageHeader";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError } from "@/lib/api/api-error";
import { curriculaApi, curriculaKeys } from "../api/curricula.api";
import { EnrollmentDialog } from "@/features/enrollment/components/EnrollmentDialog";

const verificationLabels: Record<string, string> = {
  UNVERIFIED_DRAFT: "Nguồn tham khảo chưa được xác minh",
  LINK_CHECKED: "Đã kiểm tra liên kết nguồn",
  PEDAGOGICALLY_REVIEWED: "Đã thẩm định nội dung sư phạm",
};

export function CurriculumDetailPage() {
  const { slug = "" } = useParams();
  const [isEnrollDialogOpen, setIsEnrollDialogOpen] = useState(false);

  const query = useQuery({
    queryKey: curriculaKeys.detail(slug),
    queryFn: ({ signal }) => curriculaApi.detail(slug, signal),
    enabled: Boolean(slug),
  });

  if (query.isPending) return <div role="status" aria-label="Đang tải chi tiết lộ trình" className="space-y-4"><Skeleton className="h-28" /><Skeleton className="h-64" /></div>;
  if (query.isError) return <ErrorState message={query.error instanceof ApiError && query.error.isNotFound() ? "Lộ trình không tồn tại hoặc chưa được xuất bản." : "Không thể tải chi tiết lộ trình."} onRetry={() => void query.refetch()} />;

  const curriculum = query.data;
  const provenance = curriculum.provenance;
  // Links come from imported content; only browser-safe protocols are rendered.
  const sourceUrl = provenance.sourceUrl && /^https?:\/\//i.test(provenance.sourceUrl) ? provenance.sourceUrl : null;

  return (
    <div className="space-y-6">
      <Link to="/curricula" className="inline-flex items-center gap-2 text-sm text-primary hover:underline"><ArrowLeft className="h-4 w-4" />Tất cả lộ trình</Link>
      <PageHeader
        title={curriculum.title}
        description={curriculum.description}
        actions={
          <Button onClick={() => setIsEnrollDialogOpen(true)} className="gap-2">
            <BookOpen className="h-4 w-4" />
            Đăng ký học
          </Button>
        }
      />
      <EnrollmentDialog
        curriculumId={curriculum.id}
        revisionId={curriculum.revision.id}
        curriculumTitle={curriculum.title}
        isOpen={isEnrollDialogOpen}
        onClose={() => setIsEnrollDialogOpen(false)}
      />
      <section aria-label="Thông tin giáo trình" className="space-y-2 rounded-xl border border-border bg-card p-5 text-sm">
        <p className="font-semibold">{curriculum.targetLocale === "en-GB" ? "Chuẩn phát âm Anh-Anh (en-GB)" : curriculum.targetLocale} · Phiên bản {curriculum.revision.version}</p>
        <p>Biên soạn: {curriculum.authors.join(", ")}</p>
        <p>Nguồn tham khảo: {sourceUrl ? <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className="text-primary underline">{provenance.sourceName}</a> : provenance.sourceName}</p>
        <p className="text-muted-foreground">{provenance.sourceLicense}</p>
        <p>{verificationLabels[provenance.verificationStatus] ?? provenance.verificationStatus}{provenance.authorCompiled ? ". Giáo trình do tác giả biên soạn dựa trên nguồn tham khảo." : "."}</p>
      </section>
      <section aria-label="Mục lục lộ trình" className="space-y-4">
        <h2 className="text-xl font-semibold">Nội dung học</h2>
        {curriculum.revision.phases.length === 0 && <p className="text-muted-foreground">Chưa có mục lục cho phiên bản này.</p>}
        {curriculum.revision.phases.map((phase) => (
          <details key={phase.id} open className="rounded-xl border border-border bg-card">
            <summary className="cursor-pointer px-5 py-4 font-semibold focus-visible:outline focus-visible:outline-2">{phase.title}</summary>
            <div className="space-y-3 px-4 pb-4">
              {phase.weeks.map((week) => (
                <details key={week.id} open className="rounded-lg border border-border">
                  <summary className="cursor-pointer p-4 font-medium focus-visible:outline focus-visible:outline-2">Tuần {week.weekNumber}: {week.title} <span className="ml-2 text-sm font-normal text-muted-foreground">({week.lessons.length} buổi)</span></summary>
                  <ul className="divide-y divide-border border-t border-border">
                    {week.lessons.map((lesson) => (
                      <li key={lesson.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                        <span className="min-w-0 flex-1 basis-48 text-sm">{lesson.title}</span>
                        <span className="inline-flex items-center gap-1 text-xs text-muted-foreground"><Clock className="h-3 w-3" />{lesson.estimatedMinutes} phút</span>
                        <span className={lesson.required ? "rounded-full bg-primary/10 px-2 py-1 text-xs font-medium text-primary" : "rounded-full bg-muted px-2 py-1 text-xs text-muted-foreground"}>{lesson.required ? "Bắt buộc" : "Tùy chọn"}</span>
                      </li>
                    ))}
                  </ul>
                </details>
              ))}
            </div>
          </details>
        ))}
      </section>
    </div>
  );
}
