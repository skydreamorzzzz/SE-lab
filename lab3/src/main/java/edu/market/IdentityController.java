package edu.market;

import java.security.Principal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class IdentityController {
    private final IdentityService identity;
    private final AccountService accounts;
    public IdentityController(IdentityService identity, AccountService accounts) {
        this.identity = identity;
        this.accounts = accounts;
    }
    @GetMapping("/identity")
    String identity(Principal principal, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("viewer", accounts.current(principal.getName()));
        return "identity";
    }
    @PostMapping("/identity/recovery")
    String issue(Principal principal, @RequestParam String password, Model model, HttpServletResponse response) {
        identity(principal, model, response);
        model.addAttribute("recoveryCode", identity.recoveryCode(principal.getName(), password));
        return "identity";
    }
    @PostMapping("/identity/challenge")
    String challenge(Principal principal, @RequestParam String password, @RequestParam String phone,
                     Model model, HttpServletResponse response) {
        identity(principal, model, response);
        model.addAttribute("smsCode", identity.phoneChallenge(principal.getName(), password, phone));
        return "identity";
    }
    @PostMapping("/identity/bind")
    String bind(Principal principal, @RequestParam String code, Model model, HttpServletResponse response) {
        boolean valid = identity.bindPhone(principal.getName(), code);
        identity(principal, model, response);
        model.addAttribute("result", valid ? "手机号绑定成功" : "验证码错误");
        return "identity";
    }
    @GetMapping("/recover")
    String recovery() {
        return "recover";
    }
    @PostMapping("/recover")
    String recover(@RequestParam String username, @RequestParam String code, @RequestParam String password) {
        identity.recover(username, code, password);
        return "redirect:/login?recovered";
    }
}
