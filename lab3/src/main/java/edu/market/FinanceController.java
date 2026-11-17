package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FinanceController {
    private final FinanceService finance;
    private final TradingService trading;
    private final AccountService accounts;
    private final RefundRepository refunds;
    private final SettlementRepository settlements;
    private final PlatformService platform;
    private final PlatformRuleRepository rules;
    public FinanceController(FinanceService finance, TradingService trading, AccountService accounts,
                             RefundRepository refunds, SettlementRepository settlements,
                             PlatformService platform, PlatformRuleRepository rules) {
        this.finance = finance;
        this.trading = trading;
        this.accounts = accounts;
        this.refunds = refunds;
        this.settlements = settlements;
        this.platform = platform;
        this.rules = rules;
    }
    @GetMapping("/aftercare/{id}")
    String aftercare(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("order", trading.detail(principal.getName(), id));
        model.addAttribute("refund", refunds.findFirstByOrderIdOrderByIdDesc(id).orElse(null));
        model.addAttribute("settlement", settlements.findByOrderId(id).orElse(null));
        return "aftercare";
    }
    @PostMapping("/aftercare/{id}/{action}")
    String action(Principal principal, @PathVariable Long id, @PathVariable String action,
                  @RequestParam(defaultValue = "") String note) {
        switch (action) {
            case "request" -> finance.request(principal.getName(), id, note);
            case "approve" -> finance.decide(principal.getName(), id, true, note);
            case "reject" -> finance.decide(principal.getName(), id, false, note);
            case "return" -> finance.returnGoods(principal.getName(), id, note);
            case "received" -> finance.receivedReturn(principal.getName(), id);
            default -> throw new BusinessException("未知售后操作");
        }
        return "redirect:/aftercare/" + id;
    }
    @GetMapping("/admin/platform")
    String platform(Principal principal, Model model) {
        platform.requireSuper(principal.getName());
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("rule", rules.findById(1L).orElseGet(PlatformRule::new));
        return "platform";
    }
    @PostMapping("/admin/platform/rules")
    String rules(Principal principal, @RequestParam BigDecimal commission, @RequestParam BigDecimal promotion) {
        platform.configure(principal.getName(), commission, promotion);
        return "redirect:/admin/platform";
    }
    @PostMapping("/admin/platform/admin-status")
    String adminStatus(Principal principal, @RequestParam String username, @RequestParam boolean disabled) {
        platform.adminStatus(principal.getName(), username, disabled);
        return "redirect:/admin/platform";
    }

    @PostMapping("/admin/platform/admin")
    String admin(Principal principal, @RequestParam String username, @RequestParam String password) {
        platform.createAdmin(principal.getName(), username, password);
        return "redirect:/admin/platform";
    }
}
