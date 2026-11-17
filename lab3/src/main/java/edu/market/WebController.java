package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {
    private final AccountService accounts;
    private final ProductService products;
    private final ProductRepository productStore;
    private final TradingService trading;
    private final PurchaseOrderRepository orders;
    private final ReservationRepository reservations;
    private final ReviewRepository reviews;
    private final ReviewService reviewService;
    private final PersonalService personal;
    private final FinanceService finance;
    private final DesignViewService design;

    public WebController(AccountService accounts, ProductService products, ProductRepository productStore,
                         TradingService trading, PurchaseOrderRepository orders, ReservationRepository reservations,
                         ReviewRepository reviews, ReviewService reviewService, PersonalService personal, FinanceService finance, DesignViewService design) {
        this.accounts = accounts;
        this.products = products;
        this.productStore = productStore;
        this.trading = trading;
        this.orders = orders;
        this.reservations = reservations;
        this.reviews = reviews;
        this.reviewService = reviewService;
        this.personal = personal;
        this.finance = finance;
        this.design = design;
    }

    @ModelAttribute
    void account(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.current(principal.getName()));
        }
    }

    @GetMapping("/")
    String home(@RequestParam(defaultValue = "") String keyword,
                @RequestParam(defaultValue = "") String category,
                @RequestParam(required = false) BigDecimal min,
                @RequestParam(required = false) BigDecimal max,
                @RequestParam(required = false) ProductStatus status,
                @RequestParam(defaultValue = "newest") String sort, @RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        String username = principal == null ? null : principal.getName();
        var result = products.searchPage(username, keyword, category, min, max, status, sort, page);
        personal.history(username, "SEARCH", keyword, null);
        model.addAttribute("items", design.cards(result.getContent(), username, null, null));
        model.addAttribute("activeNav", "home");
        model.addAttribute("result", result);
        return "home";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/register")
    String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    String register(@RequestParam String username, @RequestParam String password, @RequestParam String role) {
        accounts.register(username, password, role);
        return "redirect:/login?registered";
    }

    @GetMapping("/profile")
    String profile() {
        return "profile";
    }

    @PostMapping("/profile")
    String profileSave(Principal principal, @RequestParam String displayName, @RequestParam String address) {
        accounts.profile(principal.getName(), displayName, address);
        return "redirect:/profile?saved";
    }

    @PostMapping("/profile/password")
    String password(Principal principal, @RequestParam String oldPassword, @RequestParam String newPassword) {
        accounts.password(principal.getName(), oldPassword, newPassword);
        return "redirect:/profile?saved";
    }

    @GetMapping("/products/{id}")
    String detail(@PathVariable Long id, Principal principal, Model model) {
        Product product = products.detail(id, principal == null ? null : principal.getName());
        personal.history(principal == null ? null : principal.getName(), "VIEW", id.toString(), id);
        model.addAttribute("product", product);
        model.addAttribute("card", design.cards(List.of(product), principal == null ? null : principal.getName(), null, null).getFirst());
        java.util.ArrayList<String> photos = new java.util.ArrayList<>();
        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            photos.add(product.getImageUrl());
        }
        if (product.getExtraImages() != null && !product.getExtraImages().isBlank()) {
            photos.addAll(List.of(product.getExtraImages().strip().split("\\s+")));
        }
        if (photos.isEmpty()) {
            photos.add("/images/sample.svg");
        }
        model.addAttribute("photos", photos);
        model.addAttribute("tradeTags", product.getTradeTags() == null ? List.of() : List.of(product.getTradeTags().split("\\s+")));
        model.addAttribute("publishedLabel", product.getPublishedAt() == null ? "尚未上架"
                : java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneId.of("Asia/Shanghai")).format(product.getPublishedAt()));
        model.addAttribute("reviews", reviews.findByProductIdOrderByIdDesc(id));
        model.addAttribute("requestKey", UUID.randomUUID().toString());
        model.addAttribute("actualPrice", product.getMinPrice() == null ? null : finance.price(product.getMinPrice()));
        return "detail";
    }

    @GetMapping("/seller/products")
    String selling(Principal principal, Model model) {
        model.addAttribute("items", productStore.findBySellerIdOrderByIdDesc(accounts.current(principal.getName()).getId()));
        return "selling";
    }

    @GetMapping("/seller/edit")
    String edit(@RequestParam(required = false) Long id, Principal principal, Model model) {
        Product product = id == null ? new Product() : products.detail(id, principal.getName());
        if (id != null) {
            products.owner(accounts.current(principal.getName()), product);
        }
        model.addAttribute("product", product);
        java.util.Map<String, Object> initial = new java.util.HashMap<>();
        initial.put("category", product.getCategory());
        initial.put("condition", product.getConditionLabel());
        initial.put("tags", product.getTradeTags());
        initial.put("imageUrl", product.getImageUrl());
        initial.put("extraImages", product.getExtraImages());
        initial.put("negotiable", product.getNegotiable());
        model.addAttribute("publishInitial", initial);
        return "edit";
    }

    @PostMapping("/seller/save")
    String save(Principal principal, @RequestParam(required = false) Long id, @ModelAttribute Product input) {
        products.save(principal.getName(), id, input);
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/{id}/submit")
    String submit(Principal principal, @PathVariable Long id) {
        products.submit(principal.getName(), id);
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/{id}/withdraw")
    String withdraw(Principal principal, @PathVariable Long id) {
        products.withdraw(principal.getName(), id);
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/{id}/stock")
    String inventory(Principal principal, @PathVariable Long id, @RequestParam int delta) {
        trading.inventory(principal.getName(), id, delta);
        return "redirect:/seller/products";
    }

    @GetMapping("/admin")
    String admin(Model model) {
        model.addAttribute("items", productStore.findByStatusOrderByIdAsc(ProductStatus.PENDING_REVIEW));
        return "admin";
    }

    @PostMapping("/admin/{id}/audit")
    String audit(Principal principal, @PathVariable Long id, @RequestParam boolean approve,
                 @RequestParam(defaultValue = "") String note) {
        products.audit(principal.getName(), id, approve, note);
        return "redirect:/admin";
    }

    @PostMapping("/buy/{id}")
    String buy(Principal principal, @PathVariable Long id, @RequestParam int quantity,
               @RequestParam BigDecimal displayedPrice, @RequestParam String requestKey) {
        PurchaseOrder order = trading.buy(principal.getName(), id, quantity, displayedPrice, requestKey);
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/orders")
    String orders(Principal principal, Model model) {
        Long actor = accounts.current(principal.getName()).getId();
        model.addAttribute("orders", orders.findByBuyerIdOrSellerIdOrderByIdDesc(actor, actor));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    String order(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("order", trading.detail(principal.getName(), id));
        return "order";
    }

    @PostMapping("/orders/{id}/{action}")
    String orderAction(Principal principal, @PathVariable Long id, @PathVariable String action) {
        trading.action(principal.getName(), id, action);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/reviews/{id}")
    String review(Principal principal, @PathVariable Long id, @RequestParam int rating, @RequestParam String content) {
        reviewService.submit(principal.getName(), id, rating, content);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/reserve/{id}")
    String reserve(Principal principal, @PathVariable Long id, @RequestParam int quantity) {
        trading.reserve(principal.getName(), id, quantity);
        return "redirect:/reservations";
    }

    @GetMapping("/reservations")
    String reservations(Principal principal, Model model) {
        Account actor = accounts.current(principal.getName());
        List<Reservation> items;
        if (actor.getTrader()) {
            var own = reservations.findByBuyerIdOrderByIdDesc(actor.getId());
            var incoming = productStore.findBySellerIdOrderByIdDesc(actor.getId()).stream()
                    .flatMap(p -> reservations.findByProductIdOrderByIdAsc(p.getId()).stream());
            items = java.util.stream.Stream.concat(own.stream(), incoming).distinct().toList();
        } else {
            items = reservations.findByBuyerIdOrderByIdDesc(actor.getId());
        }
        model.addAttribute("reservations", items);
        model.addAttribute("ownedProductIds", productStore.findBySellerIdOrderByIdDesc(actor.getId()).stream().map(Product::getId).toList());
        return "reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    String cancelReservation(Principal principal, @PathVariable Long id) {
        trading.cancelReservation(principal.getName(), id);
        return "redirect:/reservations";
    }

    @PostMapping("/seller/reservations/{id}/confirm")
    String confirmReservation(Principal principal, @PathVariable Long id) {
        PurchaseOrder order = trading.confirmReservation(principal.getName(), id);
        return "redirect:/orders/" + order.getId();
    }
}
