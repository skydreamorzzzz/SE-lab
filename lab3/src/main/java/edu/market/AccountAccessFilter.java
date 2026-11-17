package edu.market;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class AccountAccessFilter extends OncePerRequestFilter {
    private final AccountRepository accounts;
    public AccountAccessFilter(AccountRepository accounts) {
        this.accounts = accounts;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var principal = request.getUserPrincipal();
        Account actor = principal == null ? null : accounts.findByUsername(principal.getName()).orElse(null);
        String path = request.getRequestURI();
        if (actor != null && actor.getBanned()) {
            boolean appeal = path.equals("/moderation") || path.equals("/moderation/submit") || path.equals("/logout");
            boolean oldTrade = path.startsWith("/aftercare/") || path.matches("/orders/[0-9]+/(cancel|receive)");
            if ((actor.getRole().equals("ADMIN") && !path.equals("/logout"))
                    || (!request.getMethod().equals("GET") && !appeal && !oldTrade)) {
                response.sendError(403, "账户已封禁，可在举报与申诉入口申请复核");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
