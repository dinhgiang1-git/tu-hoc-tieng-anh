package vn.englishlearning.learning.api;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.learning.application.TodayQueryService;

@RestController
@RequestMapping("/api/v1/me")
public class TodayController {

  private final TodayQueryService todayQueryService;

  public TodayController(TodayQueryService todayQueryService) {
    this.todayQueryService = todayQueryService;
  }

  @GetMapping("/today")
  public TodayResponses.TodayResponse getToday(
      @RequestParam(value = "enrollmentId", required = false) UUID enrollmentId) {
    return todayQueryService.getToday(enrollmentId);
  }
}
