package edu.market;

import java.security.Principal;
import java.time.Instant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ActivityController {
    private final ActivityService activities;
    private final AccountService accounts;

    public ActivityController(ActivityService activities, AccountService accounts) {
        this.activities = activities;
        this.accounts = accounts;
    }

    @GetMapping("/admin/activities")
    String activity(Principal principal, @RequestParam(required = false) String actor,
                    @RequestParam(required = false) String objectType,
                    @RequestParam(required = false) Long objectId,
                    @RequestParam(required = false) Instant from,
                    @RequestParam(required = false) Instant until,
                    @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("result", activities.search(principal.getName(), actor, objectType, objectId, from, until, page));
        return "activities";
    }
}
