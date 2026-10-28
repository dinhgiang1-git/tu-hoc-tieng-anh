import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QuizActivityView } from "../components/QuizActivityView";
import { quizApi } from "../api/quiz.api";
import type { ActivityWorkspaceDto } from "../api/learning-workspace.api";
import { ApiError } from "@/lib/api/api-error";

vi.mock("../api/quiz.api", () => ({
  quizApi: {
    getQuiz: vi.fn(),
    submitQuiz: vi.fn(),
  },
}));

describe("QuizActivityView", () => {
  const enrollmentId = "e1111111-1111-1111-1111-111111111111";
  const activityId = "a1111111-1111-1111-1111-111111111111";
  const question1Id = "q1111111-1111-1111-1111-111111111111";
  const opt1AId = "opt-1a";
  const opt1BId = "opt-1b";

  const question2Id = "q2222222-2222-2222-2222-222222222222";
  const opt2AId = "opt-2a";
  const opt2BId = "opt-2b";

  const mockActivity: ActivityWorkspaceDto = {
    activityId,
    title: "Bài trắc nghiệm cuối bài",
    type: "QUIZ",
    required: true,
    completionPolicy: "PASS_SCORE",
    status: "NOT_STARTED",
    resources: [],
    instructions: "Chọn đáp án đúng nhất cho mỗi câu hỏi sau:",
  };

  const mockQuizData = {
    activityId,
    title: "Bài trắc nghiệm cuối bài",
    completionPolicy: "PASS_SCORE",
    passingScorePercentage: 80,
    questions: [
      {
        id: question1Id,
        orderIndex: 1,
        prompt: "Âm /i:/ là nguyên âm dài hay ngắn?",
        options: [
          { id: opt1AId, orderIndex: 1, content: "Nguyên âm dài" },
          { id: opt1BId, orderIndex: 2, content: "Nguyên âm ngắn" },
        ],
      },
      {
        id: question2Id,
        orderIndex: 2,
        prompt: "Từ nào sau đây chứa âm /i:/?",
        options: [
          { id: opt2AId, orderIndex: 1, content: "sheep" },
          { id: opt2BId, orderIndex: 2, content: "ship" },
        ],
      },
    ],
    latestAttempt: undefined,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders questions and options after loading", async () => {
    vi.mocked(quizApi.getQuiz).mockResolvedValueOnce(mockQuizData);

    render(
      <QuizActivityView
        activity={mockActivity}
        enrollmentId={enrollmentId}
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={vi.fn()}
      />,
    );

    expect(screen.getByText("Đang tải nội dung trắc nghiệm...")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Âm /i:/ là nguyên âm dài hay ngắn?")).toBeInTheDocument();
      expect(screen.getByText("Từ nào sau đây chứa âm /i:/?")).toBeInTheDocument();
    });

    expect(screen.getByText("Nguyên âm dài")).toBeInTheDocument();
    expect(screen.getByText("Nguyên âm ngắn")).toBeInTheDocument();
    expect(screen.getByText("sheep")).toBeInTheDocument();
    expect(screen.getByText("ship")).toBeInTheDocument();

    // Check policy banner
    expect(screen.getByText(/Đạt từ 80% trở lên/)).toBeInTheDocument();

    // Submit button should be disabled until all questions are answered
    const submitBtn = screen.getByTestId("quiz-submit-button");
    expect(submitBtn).toBeDisabled();
  });

  it("submits quiz successfully and renders passing result banner and feedback", async () => {
    vi.mocked(quizApi.getQuiz).mockResolvedValueOnce(mockQuizData);

    const onVersionUpdate = vi.fn();
    const onActivityUpdated = vi.fn();

    vi.mocked(quizApi.submitQuiz).mockResolvedValueOnce({
      attemptId: "att-1",
      attemptNumber: 1,
      score: 2,
      totalQuestions: 2,
      scorePercentage: 100,
      passed: true,
      activityCompleted: true,
      enrollmentVersion: 2,
      questionResults: [
        {
          questionId: question1Id,
          selectedOptionId: opt1AId,
          correctOptionId: opt1AId,
          isCorrect: true,
          explanation: "Âm /i:/ có dấu hai chấm biểu thị nguyên âm dài.",
        },
        {
          questionId: question2Id,
          selectedOptionId: opt2AId,
          correctOptionId: opt2AId,
          isCorrect: true,
          explanation: "'sheep' phát âm là /ʃi:p/ với âm dài.",
        },
      ],
    });

    render(
      <QuizActivityView
        activity={mockActivity}
        enrollmentId={enrollmentId}
        enrollmentVersion={1}
        onVersionUpdate={onVersionUpdate}
        onActivityUpdated={onActivityUpdated}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("Âm /i:/ là nguyên âm dài hay ngắn?")).toBeInTheDocument();
    });

    // Select options
    fireEvent.click(screen.getByTestId(`quiz-option-${question1Id}-${opt1AId}`));
    fireEvent.click(screen.getByTestId(`quiz-option-${question2Id}-${opt2AId}`));

    // Now submit button should be enabled
    const submitBtn = screen.getByTestId("quiz-submit-button");
    expect(submitBtn).not.toBeDisabled();

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByTestId("quiz-result-banner")).toBeInTheDocument();
      expect(screen.getByText("Chúc mừng! Bạn đã đạt bài kiểm tra.")).toBeInTheDocument();
    });

    expect(screen.getByText(/2\/2/)).toBeInTheDocument();
    expect(screen.getByText("Nhiệm vụ đã hoàn thành")).toBeInTheDocument();
    expect(screen.getByText("Âm /i:/ có dấu hai chấm biểu thị nguyên âm dài.")).toBeInTheDocument();

    expect(onVersionUpdate).toHaveBeenCalledWith(2);
    expect(onActivityUpdated).toHaveBeenCalledWith({ status: "COMPLETED" });
  });

  it("handles failing quiz attempt and allows retry", async () => {
    vi.mocked(quizApi.getQuiz).mockResolvedValueOnce(mockQuizData);

    const onActivityUpdated = vi.fn();

    vi.mocked(quizApi.submitQuiz).mockResolvedValueOnce({
      attemptId: "att-2",
      attemptNumber: 1,
      score: 1,
      totalQuestions: 2,
      scorePercentage: 50,
      passed: false,
      activityCompleted: false,
      enrollmentVersion: 2,
      questionResults: [
        {
          questionId: question1Id,
          selectedOptionId: opt1AId,
          correctOptionId: opt1AId,
          isCorrect: true,
          explanation: "Đúng rồi!",
        },
        {
          questionId: question2Id,
          selectedOptionId: opt2BId,
          correctOptionId: opt2AId,
          isCorrect: false,
          explanation: "'ship' có âm ngắn /ɪ/, còn 'sheep' mới có âm dài /i:/.",
        },
      ],
    });

    render(
      <QuizActivityView
        activity={mockActivity}
        enrollmentId={enrollmentId}
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={onActivityUpdated}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("Âm /i:/ là nguyên âm dài hay ngắn?")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId(`quiz-option-${question1Id}-${opt1AId}`));
    fireEvent.click(screen.getByTestId(`quiz-option-${question2Id}-${opt2BId}`));

    fireEvent.click(screen.getByTestId("quiz-submit-button"));

    await waitFor(() => {
      expect(screen.getByText("Chưa đạt yêu cầu.")).toBeInTheDocument();
    });

    expect(onActivityUpdated).toHaveBeenCalledWith({ status: "IN_PROGRESS" });
    expect(screen.getByTestId(`quiz-explanation-${question2Id}`)).toHaveTextContent(
      "'ship' có âm ngắn /ɪ/, còn 'sheep' mới có âm dài /i:/.",
    );

    // Click retry
    const retryBtn = screen.getByTestId("quiz-retry-button");
    fireEvent.click(retryBtn);

    // Results hidden and options reset
    expect(screen.queryByTestId("quiz-result-banner")).not.toBeInTheDocument();
    expect(screen.getByTestId("quiz-submit-button")).toBeDisabled();
  });

  it("handles version conflict 409 error", async () => {
    vi.mocked(quizApi.getQuiz).mockResolvedValueOnce(mockQuizData);

    vi.mocked(quizApi.submitQuiz).mockRejectedValueOnce(
      new ApiError({
        status: 409,
        code: "VERSION_CONFLICT",
        title: "Conflict",
        detail: "Dữ liệu lộ trình đã thay đổi trên thiết bị khác.",
        type: "urn:problem-type:conflict",
        traceId: "trace-1",
        fieldErrors: [],
      }),
    );

    render(
      <QuizActivityView
        activity={mockActivity}
        enrollmentId={enrollmentId}
        enrollmentVersion={1}
        onVersionUpdate={vi.fn()}
        onActivityUpdated={vi.fn()}
        onRefreshWorkspace={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("Âm /i:/ là nguyên âm dài hay ngắn?")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId(`quiz-option-${question1Id}-${opt1AId}`));
    fireEvent.click(screen.getByTestId(`quiz-option-${question2Id}-${opt2AId}`));

    fireEvent.click(screen.getByTestId("quiz-submit-button"));

    await waitFor(() => {
      expect(screen.getByText(/Dữ liệu lộ trình đã thay đổi trên thiết bị khác/)).toBeInTheDocument();
      expect(screen.getByText("Đồng bộ dữ liệu")).toBeInTheDocument();
    });
  });
});
