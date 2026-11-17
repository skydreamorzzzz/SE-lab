package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class PersonalController {
    private final PersonalService service;
    private final AccountService accounts;
    public PersonalController(PersonalService service, AccountService accounts) {
        this.service = service;
        this.accounts = accounts;
    }
    @GetMapping("/cart")
    String cart(Principal principal, @RequestParam(defaultValue = "") String keyword,
                @RequestParam(defaultValue = "") String group, Model model) {
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("rows", service.cartRows(principal.getName(), keyword, group));
        model.addAttribute("key", UUID.randomUUID().toString());
        return "cart";
    }
    @PostMapping("/cart/add")
    String add(Principal principal, @RequestParam Long productId, @RequestParam int quantity,
               @RequestParam(defaultValue = "默认") String group) {
        service.cart(principal.getName(), productId, quantity, group);
        return "redirect:/cart";
    }
    @PostMapping("/cart/{id}/checkout")
    String checkout(Principal principal, @PathVariable Long id, @RequestParam BigDecimal price, @RequestParam String key) {
        return "redirect:/orders/" + service.checkout(principal.getName(), id, price, key).getId();
    }
    @GetMapping("/personal")
    String personal(Principal principal, @RequestParam(defaultValue = "FAVORITE") String kind, Model model) {
        model.addAttribute("viewer", accounts.current(principal.getName()));
        model.addAttribute("kind", kind);
        model.addAttribute("entries", service.list(principal.getName(), kind));
        return "personal";
    }
    @PostMapping("/personal/toggle")
    String toggle(Principal principal, @RequestParam String kind, @RequestParam Long target,
                  @RequestParam(defaultValue = "true") boolean enabled) {
        service.toggle(principal.getName(), kind, target, enabled);
        return "redirect:/personal?kind=" + kind;
    }
    @PostMapping("/personal/{id}/delete")
    String delete(Principal principal, @PathVariable Long id) {
        service.remove(principal.getName(), id);
        return "redirect:/profile";
    }
}
