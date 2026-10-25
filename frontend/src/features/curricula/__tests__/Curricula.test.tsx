import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route, Navigate } from "react-router-dom";
import { CurriculaPage } from "../pages/CurriculaPage";
import { CurriculumDetailPage } from "../pages/CurriculumDetailPage";
import { curriculaApi, type CurriculumSummary, type CurriculumDetail } from "../api/curricula.api";
import { ApiError } from "@/lib/api/api-error";

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        staleTime: Infinity,
      },
    },
  });
}

const mockPilotSummary: CurriculumSummary = {
  id: "c1111111-1111-1111-1111-111111111111",
  slug: "pilot-curriculum",
  title: "Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng",
  description:
    "Lộ trình thí điểm chuẩn hóa phát âm IPA Anh-Anh và củng cố nền tảng ngữ pháp cơ bản.",
  targetLocale: "en-GB",
  currentRevisionId: "r1111111-1111-1111-1111-111111111111",
  currentRevisionVersion: "1.0.0",
};

const mockPilotDetail: CurriculumDetail = {
  id: "c1111111-1111-1111-1111-111111111111",
  slug: "pilot-curriculum",
  title: "Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng",
  description:
    "Lộ trình thí điểm chuẩn hóa phát âm IPA Anh-Anh và củng cố nền tảng ngữ pháp cơ bản.",
  targetLocale: "en-GB",
  authors: ["AI Specialist", "Curriculum Lead"],
  provenance: {
    sourceName: "Khung mục tiêu Lộ trình tiếng Anh tham khảo",
    sourceUrl: "https://docs.google.com/spreadsheets/d/1Yim4AAiIfHMmNTrPsTXQtGfAkhsMcsBc_6-eURCgFEU/edit#gid=0",
    sourceLicense: "Educational Fair Use / Pedagogical Framework Reference",
    authorCompiled: true,
    verificationStatus: "UNVERIFIED_DRAFT",
  },
  revision: {
    id: "r1111111-1111-1111-1111-111111111111",
    version: "1.0.0",
    status: "PUBLISHED",
    phases: [
      {
        id: "p1111111-1111-1111-1111-111111111111",
        key: "pilot.phase01",
        title: "Giai đoạn 1: Chuẩn hóa Ngữ âm & Ngữ pháp Nền tảng",
        orderIndex: 1,
        weeks: [
          {
            id: "w1111111-1111-1111-1111-111111111111",
            key: "pilot.w01",
            title: "Chuẩn hóa Ngữ âm IPA Anh-Anh cốt lõi",
            weekNumber: 1,
            orderIndex: 1,
            lessons: [
              {
                id: "l1111111-1111-1111-1111-111111111101",
                key: "pilot.w01.l01",
                title: "Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/",
                orderIndex: 1,
                estimatedMinutes: 25,
                required: true,
              },
              {
                id: "l1111111-1111-1111-1111-111111111102",
                key: "pilot.w01.l02",
                title: "Buổi 2: Cặp phụ âm /p/ và /b/",
                orderIndex: 2,
                estimatedMinutes: 20,
                required: true,
              },
              {
                id: "l1111111-1111-1111-1111-111111111103",
                key: "pilot.w01.l03",
                title: "Buổi 3: Quy tắc Trọng âm từ 2 âm tiết",
                orderIndex: 3,
                estimatedMinutes: 25,
                required: true,
              },
              {
                id: "l1111111-1111-1111-1111-111111111104",
                key: "pilot.w01.l04",
                title: "Buổi 4: Âm lướt Schwa /ə/",
                orderIndex: 4,
                estimatedMinutes: 20,
                required: true,
              },
              {
                id: "l1111111-1111-1111-1111-111111111105",
                key: "pilot.w01.l05",
                title: "Buổi 5: Đánh giá & Tổng hợp tuần 1",
                orderIndex: 5,
                estimatedMinutes: 30,
                required: true,
              },
              {
                id: "l1111111-1111-1111-1111-111111111106",
                key: "pilot.w01.l06",
                title: "Buổi 6: Hiện tượng Non-Rhoticity trong tiếng Anh Anh",
                orderIndex: 6,
                estimatedMinutes: 15,
                required: false,
              },
            ],
          },
        ],
      },
    ],
  },
};

describe("CurriculaPage", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.restoreAllMocks();
    queryClient = createTestQueryClient();
  });

  it("renders loading skeleton while query is pending", () => {
    vi.spyOn(curriculaApi, "list").mockReturnValue(new Promise(() => {}));

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CurriculaPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("status", { name: "Đang tải lộ trình" })).toBeInTheDocument();
  });

  it("renders error state with retry button when fetch fails", async () => {
    const listSpy = vi.spyOn(curriculaApi, "list").mockRejectedValue(new Error("Network error"));

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CurriculaPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Không thể tải danh mục lộ trình.")).toBeInTheDocument();
    });

    const retryButton = screen.getByRole("button", { name: "Thử lại" });
    expect(retryButton).toBeInTheDocument();

    listSpy.mockResolvedValueOnce([mockPilotSummary]);
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(
        screen.getByText("Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng"),
      ).toBeInTheDocument();
    });
  });

  it("renders empty state when no curricula are published", async () => {
    vi.spyOn(curriculaApi, "list").mockResolvedValue([]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CurriculaPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Chưa có lộ trình được xuất bản")).toBeInTheDocument();
      expect(
        screen.getByText("Các lộ trình mới sẽ xuất hiện tại đây khi nội dung sẵn sàng."),
      ).toBeInTheDocument();
    });
  });

  it("renders published curricula cards with en-GB badge, version, and link", async () => {
    vi.spyOn(curriculaApi, "list").mockResolvedValue([mockPilotSummary]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CurriculaPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByRole("heading", {
          name: "Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng",
        }),
      ).toBeInTheDocument();
    });

    expect(screen.getByText("Anh-Anh · en-GB")).toBeInTheDocument();
    expect(screen.getByText("v1.0.0")).toBeInTheDocument();
    expect(
      screen.getByText(/Lộ trình thí điểm chuẩn hóa phát âm IPA Anh-Anh/),
    ).toBeInTheDocument();

    const detailLink = screen.getByRole("link", { name: /Xem lộ trình/ });
    expect(detailLink).toHaveAttribute("href", "/curricula/pilot-curriculum");
  });
});

describe("CurriculumDetailPage", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.restoreAllMocks();
    queryClient = createTestQueryClient();
  });

  it("renders loading skeleton while fetching detail", () => {
    vi.spyOn(curriculaApi, "detail").mockReturnValue(new Promise(() => {}));

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/curricula/pilot-curriculum"]}>
          <Routes>
            <Route path="/curricula/:slug" element={<CurriculumDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      screen.getByRole("status", { name: "Đang tải chi tiết lộ trình" }),
    ).toBeInTheDocument();
  });

  it("renders 404 message when curriculum is not found or unpublished", async () => {
    const notFoundError = new ApiError({
      type: "about:blank",
      status: 404,
      code: "RESOURCE_NOT_FOUND",
      title: "Not Found",
      detail: "Curriculum not found",
      traceId: "trace-404",
      fieldErrors: [],
    });
    vi.spyOn(curriculaApi, "detail").mockRejectedValue(notFoundError);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/curricula/unknown-slug"]}>
          <Routes>
            <Route path="/curricula/:slug" element={<CurriculumDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByText("Lộ trình không tồn tại hoặc chưa được xuất bản."),
      ).toBeInTheDocument();
    });
  });

  it("renders generic error message and retry button when network error occurs", async () => {
    const detailSpy = vi
      .spyOn(curriculaApi, "detail")
      .mockRejectedValue(new Error("Internal Server Error"));

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/curricula/pilot-curriculum"]}>
          <Routes>
            <Route path="/curricula/:slug" element={<CurriculumDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Không thể tải chi tiết lộ trình.")).toBeInTheDocument();
    });

    const retryButton = screen.getByRole("button", { name: "Thử lại" });
    expect(retryButton).toBeInTheDocument();

    detailSpy.mockResolvedValueOnce(mockPilotDetail);
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(
        screen.getByRole("heading", {
          name: "Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng",
        }),
      ).toBeInTheDocument();
    });
  });

  it("renders full pilot detail with provenance transparency, phase/week accordions, and lesson badges", async () => {
    vi.spyOn(curriculaApi, "detail").mockResolvedValue(mockPilotDetail);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/curricula/pilot-curriculum"]}>
          <Routes>
            <Route path="/curricula/:slug" element={<CurriculumDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByRole("heading", {
          name: "Lộ trình Thử nghiệm: Phát âm Anh-Anh & Nền tảng",
        }),
      ).toBeInTheDocument();
    });

    // Back link
    expect(screen.getByRole("link", { name: /Tất cả lộ trình/ })).toHaveAttribute(
      "href",
      "/curricula",
    );

    // Curriculum info section
    expect(screen.getByText(/Chuẩn phát âm Anh-Anh \(en-GB\) · Phiên bản 1.0.0/)).toBeInTheDocument();
    expect(screen.getByText("Biên soạn: AI Specialist, Curriculum Lead")).toBeInTheDocument();
    expect(
      screen.getByRole("link", {
        name: "Khung mục tiêu Lộ trình tiếng Anh tham khảo",
      }),
    ).toHaveAttribute("href", "https://docs.google.com/spreadsheets/d/1Yim4AAiIfHMmNTrPsTXQtGfAkhsMcsBc_6-eURCgFEU/edit#gid=0");
    expect(screen.getByText("Educational Fair Use / Pedagogical Framework Reference")).toBeInTheDocument();

    // Truthful provenance: must NOT claim to be verified
    expect(
      screen.getByText(
        /Nguồn tham khảo chưa được xác minh\. Giáo trình do tác giả biên soạn dựa trên nguồn tham khảo\./,
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Đã thẩm định nội dung sư phạm/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Đã kiểm tra liên kết nguồn/)).not.toBeInTheDocument();

    // Outline section
    expect(screen.getByText("Giai đoạn 1: Chuẩn hóa Ngữ âm & Ngữ pháp Nền tảng")).toBeInTheDocument();
    expect(
      screen.getByText(/Tuần 1: Chuẩn hóa Ngữ âm IPA Anh-Anh cốt lõi/),
    ).toBeInTheDocument();
    expect(screen.getByText(/\(6 buổi\)/)).toBeInTheDocument();

    // All 6 lessons
    expect(screen.getByText("Buổi 1: Cặp nguyên âm đơn /iː/ và /ɪ/")).toBeInTheDocument();
    expect(screen.getByText("Buổi 2: Cặp phụ âm /p/ và /b/")).toBeInTheDocument();
    expect(screen.getByText("Buổi 3: Quy tắc Trọng âm từ 2 âm tiết")).toBeInTheDocument();
    expect(screen.getByText("Buổi 4: Âm lướt Schwa /ə/")).toBeInTheDocument();
    expect(screen.getByText("Buổi 5: Đánh giá & Tổng hợp tuần 1")).toBeInTheDocument();
    expect(
      screen.getByText("Buổi 6: Hiện tượng Non-Rhoticity trong tiếng Anh Anh"),
    ).toBeInTheDocument();

    // Lesson durations (Buổi 1 & 3 are 25m, Buổi 2 & 4 are 20m, Buổi 5 is 30m, Buổi 6 is 15m)
    expect(screen.getAllByText("25 phút")).toHaveLength(2);
    expect(screen.getAllByText("20 phút")).toHaveLength(2);
    expect(screen.getByText("30 phút")).toBeInTheDocument();
    expect(screen.getByText("15 phút")).toBeInTheDocument();

    // Exactly 5 "Bắt buộc" badges and 1 "Tùy chọn" badge
    const requiredBadges = screen.getAllByText("Bắt buộc");
    expect(requiredBadges).toHaveLength(5);

    const optionalBadges = screen.getAllByText("Tùy chọn");
    expect(optionalBadges).toHaveLength(1);
  });
});

describe("Router Compatibility", () => {
  it("redirects /curriculum to /curricula", () => {
    const queryClient = createTestQueryClient();
    vi.spyOn(curriculaApi, "list").mockResolvedValue([]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/curriculum"]}>
          <Routes>
            <Route path="/curriculum" element={<Navigate to="/curricula" replace />} />
            <Route path="/curricula" element={<CurriculaPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Lộ trình học" })).toBeInTheDocument();
  });
});
