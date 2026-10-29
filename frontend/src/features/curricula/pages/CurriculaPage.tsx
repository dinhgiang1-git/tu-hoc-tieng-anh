import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ArrowRight, BookOpen } from "lucide-react";
import { ErrorState } from "@/components/shared/ErrorState";
import { PageHeader } from "@/components/shared/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { buttonVariants } from "@/components/ui/button-variants";
import { curriculaApi, curriculaKeys } from "../api/curricula.api";
import { enrollmentApi, enrollmentKeys } from "@/features/enrollment/api/enrollment.api";

const enrollmentLabels = {
  ACTIVE: "Đang học",
  PAUSED: "Tạm dừng",
  COMPLETED: "Đã hoàn thành",
  ABANDONED: "Đã dừng",
};

export function CurriculaPage() {
  const curriculaQuery = useQuery({
    queryKey: curriculaKeys.all,
    queryFn: ({ signal }) => curriculaApi.list(signal),
  });
  const myEnrollmentsQuery = useQuery({
    queryKey: enrollmentKeys.my(),
    queryFn: ({ signal }) => enrollmentApi.myEnrollments(signal),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Lộ trình học" />

      {myEnrollmentsQuery.isError && (
        <div role="alert" className="flex items-center gap-3 text-sm text-destructive">
          <span>Không thể tải tiến độ học.</span>
          <Button variant="outline" size="sm" onClick={() => void myEnrollmentsQuery.refetch()}>Tải lại</Button>
        </div>
      )}

      {curriculaQuery.isPending ? (
        <div aria-label="Đang tải lộ trình" role="status" className="grid gap-4 lg:grid-cols-2">
          <Skeleton className="h-56 rounded-xl" />
          <Skeleton className="h-56 rounded-xl" />
        </div>
      ) : curriculaQuery.isError ? (
        <ErrorState message="Không thể tải danh mục lộ trình." onRetry={() => void curriculaQuery.refetch()} />
      ) : curriculaQuery.data.length === 0 ? (
        <div className="space-y-3 rounded-xl border border-dashed border-border p-10 text-center">
          <BookOpen aria-hidden="true" className="mx-auto h-8 w-8 text-muted-foreground" />
          <h2 className="font-semibold">Chưa có lộ trình được xuất bản</h2>
        </div>
      ) : (
        <div className="grid items-stretch gap-4 lg:grid-cols-2">
          {curriculaQuery.data.map((curriculum) => {
            const enrollment = myEnrollmentsQuery.isError ? undefined : myEnrollmentsQuery.data?.find(
              (item) => item.curriculumTitle === curriculum.title,
            );

            return (
              <article key={curriculum.id} className="flex flex-col gap-4 rounded-xl border border-border bg-card p-6 shadow-sm">
                <div className="flex-1 space-y-3">
                  <h2 className="text-xl font-semibold leading-snug [text-wrap:balance]">{curriculum.title}</h2>
                  <p className="line-clamp-2 text-sm leading-relaxed text-muted-foreground">{curriculum.description}</p>
                </div>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <p className="text-sm text-muted-foreground">
                    {enrollment ? (
                      <>
                        {enrollmentLabels[enrollment.status]}
                        {enrollment.progressPercent !== null && ` · ${enrollment.progressPercent}%`}
                      </>
                    ) : curriculum.targetLocale === "en-GB" ? "Anh-Anh" : curriculum.targetLocale}
                  </p>
                  <Link
                    to={`/curricula/${encodeURIComponent(curriculum.slug)}`}
                    aria-label={`Xem lộ trình: ${curriculum.title}`}
                    className={buttonVariants({ variant: "outline", className: "gap-2" })}
                  >
                    Xem lộ trình <ArrowRight aria-hidden="true" className="h-4 w-4" />
                  </Link>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
