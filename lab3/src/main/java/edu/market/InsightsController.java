package edu.market;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class InsightsController {
    private final StatisticsService statistics;
    private final RankingService ranking;
    private final AccountService accounts;
    public InsightsController(StatisticsService statistics, RankingService ranking, AccountService accounts) {
        this.statistics = statistics;
        this.ranking = ranking;
        this.accounts = accounts;
    }
    @GetMapping("/ranking")
    String ranking(Principal principal, @RequestParam(defaultValue = "week") String period,
                   @RequestParam(defaultValue = "") String category, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.current(principal.getName()));
        }
        model.addAttribute("items", ranking.rank(principal == null ? null : principal.getName(), period, category));
        return "ranking";
    }
    @GetMapping("/admin/statistics")
    String statistics(Principal principal, @RequestParam(required = false) LocalDate from,
                      @RequestParam(required = false) LocalDate until, Model model) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("summary", statistics.summary(principal.getName(), from == null ? today.withDayOfMonth(1) : from,
                until == null ? today : until));
        return "statistics";
    }
}
