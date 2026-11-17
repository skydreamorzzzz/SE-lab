package edu.market;

import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DesignWorkspaceController {
    private final DesignAccountService profiles;
    private final DesignAdminService admin;
    private final ProductService products;
    private final ModerationService moderation;

    public DesignWorkspaceController(DesignAccountService profiles, DesignAdminService admin, ProductService products, ModerationService moderation) {
        this.profiles = profiles;
        this.admin = admin;
        this.products = products;
        this.moderation = moderation;
    }

    @GetMapping("/api/profile")
    @ResponseBody
    Map<String, Object> profile(Principal actor) {
        return profiles.profile(actor.getName());
    }

    @PostMapping("/api/profile")
    @ResponseBody
    Map<String, Object> save(Principal actor, @RequestParam String displayName, @RequestParam String address,
            @RequestParam String city, @RequestParam String bio) {
        profiles.update(actor.getName(), displayName, address, city, bio);
        return Map.of("saved", true);
    }

    @GetMapping("/messages")
    String messages(Principal actor, Model model) {
        model.addAttribute("rows", ((Map<?, ?>) profiles.profile(actor.getName()).get("records")).get("MESSAGES"));
        return "messages";
    }

    @GetMapping("/api/admin/dashboard")
    @ResponseBody
    Map<String, Object> dashboard(Principal actor) {
        return admin.dashboard(actor.getName());
    }

    @GetMapping("/api/admin/statistics")
    @ResponseBody
    Map<String, Object> statistics(Principal actor, @RequestParam(defaultValue = "7") int days) {
        return admin.statistics(actor.getName(), days);
    }

    @PostMapping("/api/admin/audit")
    @ResponseBody
    Map<String, Object> audit(Principal actor, @RequestParam Long id, @RequestParam boolean approve, @RequestParam String note) {
        products.audit(actor.getName(), id, approve, note);
        return Map.of("saved", true);
    }

    @PostMapping("/api/admin/punish")
    @ResponseBody
    Map<String, Object> punish(Principal actor, @RequestParam String type, @RequestParam Long id, @RequestParam String reason) {
        moderation.punish(actor.getName(), type, id, reason);
        return Map.of("saved", true);
    }

    @PostMapping("/api/admin/decide")
    @ResponseBody
    Map<String, Object> decide(Principal actor, @RequestParam Long id, @RequestParam boolean accept, @RequestParam String feedback) {
        moderation.decide(actor.getName(), id, accept, feedback);
        return Map.of("saved", true);
    }
}
