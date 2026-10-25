import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { BookOpen, ArrowRight } from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/ui/skeleton";
import { curriculaApi, curriculaKeys } from "../api/curricula.api";

export function CurriculaPage() {
  const query = useQuery({
    queryKey: curriculaKeys.all,
    queryFn: ({ signal }) => curriculaApi.list(signal),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Lộ trình học" description="Chọn một lộ trình và khám phá nội dung từng tuần." />
      {query.isPending ? (
        <div aria-label="Đang tải lộ trình" role="status" className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-52 rounded-xl" /><Skeleton className="h-52 rounded-xl" />
        </div>
      ) : query.isError ? (
        <ErrorState message="Không thể tải danh mục lộ trình." onRetry={() => void query.refetch()} />
      ) : query.data.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-10 text-center">
          <BookOpen className="mx-auto mb-3 h-9 w-9 text-muted-foreground" />
          <h2 className="font-semibold">Chưa có lộ trình được xuất bản</h2>
          <p className="mt-2 text-sm text-muted-foreground">Các lộ trình mới sẽ xuất hiện tại đây khi nội dung sẵn sàng.</p>
        </div>
      ) : (
        <div className="grid gap-5 lg:grid-cols-2">
          {query.data.map((curriculum) => (
            <article key={curriculum.id} className="flex flex-col gap-4 rounded-xl border border-border bg-card p-6 shadow-sm">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <BookOpen className="h-4 w-4" />
                <span>{curriculum.targetLocale === "en-GB" ? "Anh-Anh · en-GB" : curriculum.targetLocale}</span>
                <span className="ml-auto">v{curriculum.currentRevisionVersion}</span>
              </div>
              <h2 className="text-xl font-semibold">{curriculum.title}</h2>
              <p className="flex-1 text-sm leading-relaxed text-muted-foreground">{curriculum.description}</p>
              <Link className="inline-flex items-center gap-2 self-start font-medium text-primary hover:underline focus-visible:outline focus-visible:outline-2" to={`/curricula/${encodeURIComponent(curriculum.slug)}`}>
                Xem lộ trình <ArrowRight className="h-4 w-4" />
              </Link>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
