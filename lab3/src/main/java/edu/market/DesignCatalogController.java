package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DesignCatalogController {
    private final ProductService products;
    private final DesignViewService views;
    private final PersonalService personal;
    private final AccountService accounts;

    public DesignCatalogController(ProductService products, DesignViewService views, PersonalService personal, AccountService accounts) {
        this.products = products;
        this.views = views;
        this.personal = personal;
        this.accounts = accounts;
    }

    @GetMapping("/search")
    String search(Principal principal, Model model) {
        model.addAttribute("activeNav", "nearby");
        model.addAttribute("viewer", principal == null ? null : accounts.current(principal.getName()));
        return "search";
    }

    @GetMapping("/api/catalog")
    @ResponseBody
    Catalog catalog(Principal principal, @RequestParam(defaultValue = "") String keyword,
                    @RequestParam(defaultValue = "") String category, @RequestParam(required = false) BigDecimal min,
                    @RequestParam(required = false) BigDecimal max, @RequestParam(defaultValue = "all") String condition,
                    @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
                    @RequestParam(required = false) Integer radius, @RequestParam(defaultValue = "0") int sold,
                    @RequestParam(defaultValue = "0") int rating, @RequestParam(defaultValue = "newest") String sort,
                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "false") boolean remember) {
        String username = principal == null ? null : principal.getName();
        var result = products.searchPage(username, keyword, category, min, max, null, sort, page,
                new CatalogFilter(condition, latitude, longitude, radius, sold, rating));
        if (remember) {
            personal.history(username, "SEARCH", keyword, null);
        }
        return new Catalog(views.cards(result.getContent(), username, latitude, longitude), result.getTotalElements(),
                result.getNumber(), result.hasNext());
    }

    @GetMapping("/api/search-history")
    @ResponseBody
    List<String> history(Principal principal) {
        return personal.list(principal.getName(), "SEARCH").stream().limit(8).map(PersonalEntry::getTargetKey).toList();
    }

    @PostMapping("/api/search-history/clear")
    @ResponseBody
    Map<String, Boolean> clearHistory(Principal principal) {
        personal.clearHistory(principal.getName());
        return Map.of("saved", true);
    }

    @PostMapping("/api/favorite")
    @ResponseBody
    Map<String, Boolean> favorite(Principal principal, @RequestParam Long productId, @RequestParam boolean enabled) {
        personal.toggle(principal.getName(), "FAVORITE", productId, enabled);
        return Map.of("liked", enabled);
    }

    public record Catalog(List<DesignViewService.Card> items, long total, int page, boolean hasNext) {
    }
}
