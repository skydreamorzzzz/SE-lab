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
    private final PersonalService personal;

    public SellerController(SellerService sellers, AccountService accounts, PersonalService personal) {
        this.sellers = sellers;
        this.accounts = accounts;
        this.personal = personal;
    }

    @GetMapping("/sellers/{id}")
    String seller(@PathVariable Long id, Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.current(principal.getName()));
        }
        BusinessException.require(!personal.blocked(principal == null ? null : principal.getName(), id), "已屏蔽此卖家");
        model.addAttribute("sellerId", id);
        model.addAttribute("seller", sellers.profile(id));
        return "seller";
    }
}
