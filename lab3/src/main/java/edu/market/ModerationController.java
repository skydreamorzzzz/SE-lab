package edu.market;

import java.security.Principal;
import java.time.Instant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ModerationController {
    private final AccountService accounts;
    private final ModerationService service;
    private final ModerationRepository cases;
    public ModerationController(AccountService accounts, ModerationService service, ModerationRepository cases) {
        this.accounts = accounts;
        this.service = service;
        this.cases = cases;
    }
    @GetMapping("/moderation")
    String page(Principal principal, Model model) {
        Account actor = accounts.current(principal.getName());
        model.addAttribute("viewer", actor);
        model.addAttribute("cases", actor.getRole().equals("ADMIN") ? cases.findAllByOrderByIdDesc()
                : cases.findByAuthorIdOrderByIdDesc(actor.getId()));
        return "moderation";
    }
    @PostMapping("/moderation/submit")
    String submit(Principal principal, @RequestParam String kind, @RequestParam String type,
                  @RequestParam Long target, @RequestParam String reason,
                  @RequestParam(defaultValue = "") String image) {
        switch (kind) {
            case "REPORT" -> service.report(principal.getName(), type, target, reason, image, Instant.now());
            case "APPEAL" -> service.appeal(principal.getName(), type, target, reason);
            case "PUNISH" -> service.punish(principal.getName(), type, target, reason);
            default -> throw new BusinessException("操作类型无效");
        }
        return "redirect:/moderation";
    }
    @PostMapping("/moderation/decide")
    String decide(Principal principal, @RequestParam Long id, @RequestParam boolean accept, @RequestParam String feedback) {
        service.decide(principal.getName(), id, accept, feedback);
        return "redirect:/moderation";
    }
}
