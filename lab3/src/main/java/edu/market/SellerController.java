package edu.market;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SellerController {
    private final SellerService sellers;
    private final AccountService accounts;

    public SellerController(SellerService sellers, AccountService accounts) {
        this.sellers = sellers;
        this.accounts = accounts;
    }

    @GetMapping("/sellers/{id}")
    String seller(@PathVariable Long id, Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.current(principal.getName()));
        }
        model.addAttribute("seller", sellers.profile(id));
        return "seller";
    }
}
