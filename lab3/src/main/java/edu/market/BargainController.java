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
public class BargainController {
    private final AccountService accounts;
    private final BargainRepository bargains;
    private final BargainService service;
    public BargainController(AccountService accounts, BargainRepository bargains, BargainService service) {
        this.accounts = accounts;
        this.bargains = bargains;
        this.service = service;
    }
    @GetMapping("/bargains")
    String list(Principal principal, Model model) {
        Account actor = accounts.current(principal.getName());
        model.addAttribute("viewer", actor);
        model.addAttribute("items", bargains.findByBuyerIdOrSellerIdOrderByIdDesc(actor.getId(), actor.getId()));
        return "bargains";
    }
    @PostMapping("/bargains/propose")
    String propose(Principal principal, @RequestParam Long productId, @RequestParam int quantity, @RequestParam BigDecimal price) {
        service.propose(principal.getName(), productId, quantity, price);
        return "redirect:/bargains";
    }
    @PostMapping("/bargains/{id}/{action}")
    String action(Principal principal, @PathVariable Long id, @PathVariable String action) {
        if (action.equals("order")) {
            return "redirect:/orders/" + service.order(principal.getName(), id).getId();
        }
        BusinessException.require(action.equals("accept") || action.equals("reject"), "议价操作无效");
        service.decide(principal.getName(), id, action.equals("accept"));
        return "redirect:/bargains";
    }
}
