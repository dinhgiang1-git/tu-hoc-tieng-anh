import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/shared/ErrorState";
import { PageHeader } from "@/components/shared/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError } from "@/lib/api/api-error";
import { curriculaApi, curriculaKeys } from "../api/curricula.api";
import { EnrollmentDialog } from "@/features/enrollment/components/EnrollmentDialog";
import { IpaSoundboardModal } from "@/features/learning/components/IpaSoundboardModal";

const verificationLabels: Record<string, string> = {
  UNVERIFIED_DRAFT: "Nguồn tham khảo chưa được xác minh",
  LINK_CHECKED: "Đã kiểm tra liên kết nguồn học liệu",
  PEDAGOGICALLY_REVIEWED: "Đã thẩm định nội dung sư phạm",
};

export function CurriculumDetailPage() {
  const { slug = "" } = useParams();
  const [isEnrollDialogOpen, setIsEnrollDialogOpen] = useState(false);
  const [isIpaModalOpen, setIsIpaModalOpen] = useState(false);
  const query = useQuery({
    queryKey: curriculaKeys.detail(slug),
    queryFn: ({ signal }) => curriculaApi.detail(slug, signal),
    enabled: Boolean(slug),
  });

  if (query.isPending) {
    return (
      <div role="status" aria-label="Đang tải chi tiết lộ trình" className="space-y-6">
        <Skeleton className="h-36 rounded-xl" />
        <Skeleton className="h-64 rounded-xl" />
      </div>
    );
  }
  if (query.isError) {
    return (
      <ErrorState
        message={query.error instanceof ApiError && query.error.isNotFound()
          ? "Lộ trình không tồn tại hoặc chưa được xuất bản."
          : "Không thể tải chi tiết lộ trình."}
        onRetry={() => void query.refetch()}
      />
    );
  }

  const curriculum = query.data;
  const provenance = curriculum.provenance;
  const sourceUrl = provenance.sourceUrl && /^https?:\/\//i.test(provenance.sourceUrl)
    ? provenance.sourceUrl : null;
  const totalStages = curriculum.revision.phases.reduce((sum, phase) => sum + phase.weeks.length, 0);
  const lessons = curriculum.revision.phases.flatMap((phase) => phase.weeks.flatMap((week) => week.lessons));
  const totalRequiredLessons = lessons.filter((lesson) => lesson.required).length;

  return (
    <div className="space-y-6 [&_h1]:[text-wrap:balance]">
      <Link to="/curricula" className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
        <ArrowLeft aria-hidden="true" className="h-4 w-4" /> Tất cả lộ trình
      </Link>
      <PageHeader
        title={curriculum.title}
        description={`${totalStages} chặng · ${lessons.length} buổi học`}
        actions={(
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setIsIpaModalOpen(true)}>Bảng IPA</Button>
            <Button onClick={() => setIsEnrollDialogOpen(true)}>Đăng ký học</Button>
          </div>
        )}
      />

      <section aria-label="Mục lục lộ trình học" className="space-y-4">
        <h2 className="text-lg font-semibold">Nội dung học</h2>
        {curriculum.revision.phases.length === 0 && (
          <p className="text-sm text-muted-foreground">Chưa có mục lục cho phiên bản này.</p>
        )}
        {curriculum.revision.phases.map((phase, phaseIndex) => (
          <details key={phase.id} open={phaseIndex === 0} className="group/phase overflow-hidden rounded-xl border border-border bg-card">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-4 p-5 font-semibold hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring [&::-webkit-details-marker]:hidden">
              <span>{phase.title}</span>
              <ChevronDown aria-hidden="true" className="h-4 w-4 shrink-0 text-muted-foreground group-open/phase:rotate-180" />
            </summary>
            <div className="border-t border-border px-5">
              {phase.weeks.map((week, weekIndex) => (
                <details key={week.id} open={phaseIndex === 0 && weekIndex === 0} className="group/week border-b border-border last:border-b-0">
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-sm font-medium hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring [&::-webkit-details-marker]:hidden">
                    <span>{week.title}</span>
                    <span className="flex shrink-0 items-center gap-3 text-muted-foreground">
                      <span>{week.lessons.length} buổi</span>
                      <ChevronDown aria-hidden="true" className="h-4 w-4 group-open/week:rotate-180" />
                    </span>
                  </summary>
                  <ul className="divide-y divide-border pb-3">
                    {week.lessons.map((lesson) => (
                      <li key={lesson.id} className="flex items-start justify-between gap-4 py-3 text-sm">
                        <span className="min-w-0 leading-relaxed">{lesson.title}</span>
                        <span className="flex shrink-0 items-center gap-3 text-muted-foreground">
                          {!lesson.required && <span>Tùy chọn</span>}
                          <span>{lesson.estimatedMinutes} phút</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </details>
              ))}
            </div>
          </details>
        ))}
      </section>

      <details className="group/info rounded-xl border border-border bg-card">
        <summary className="flex cursor-pointer list-none items-center justify-between gap-4 p-5 text-sm font-medium text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring [&::-webkit-details-marker]:hidden">
          Thông tin lộ trình
          <ChevronDown aria-hidden="true" className="h-4 w-4 group-open/info:rotate-180" />
        </summary>
        <section aria-label="Thông tin giáo trình và bản quyền" className="space-y-4 border-t border-border p-5 text-sm leading-relaxed text-muted-foreground">
          <p>{curriculum.description}</p>
          <p>{curriculum.targetLocale === "en-GB" ? "Anh-Anh (en-GB)" : curriculum.targetLocale} · Phiên bản {curriculum.revision.version}</p>
          <p>Biên soạn: {curriculum.authors.join(", ")}</p>
          <p>
            Nguồn:{" "}
            {sourceUrl ? (
              <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className="text-primary underline">{provenance.sourceName}</a>
            ) : provenance.sourceName}
          </p>
          <p>Giấy phép: {provenance.sourceLicense}</p>
          <p>{verificationLabels[provenance.verificationStatus] ?? provenance.verificationStatus}</p>
        </section>
      </details>

      <EnrollmentDialog
        curriculumId={curriculum.id}
        revisionId={curriculum.revision.id}
        curriculumTitle={curriculum.title}
        totalRequiredLessons={totalRequiredLessons}
        totalStages={totalStages}
        isOpen={isEnrollDialogOpen}
        onClose={() => setIsEnrollDialogOpen(false)}
      />
      <IpaSoundboardModal isOpen={isIpaModalOpen} onClose={() => setIsIpaModalOpen(false)} />
    </div>
  );
}
