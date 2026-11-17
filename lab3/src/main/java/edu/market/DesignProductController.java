package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DesignProductController {
    private final CommentService comments;
    private final PublishingService publishing;
    private final TradingService trading;
    private final BargainService bargains;
    private final MediaService media;

    public DesignProductController(CommentService comments, PublishingService publishing, TradingService trading,
                                   BargainService bargains, MediaService media) {
        this.comments = comments;
        this.publishing = publishing;
        this.trading = trading;
        this.bargains = bargains;
        this.media = media;
    }

    @GetMapping("/api/products/{id}/comments")
    List<CommentService.CommentView> comments(@PathVariable Long id, Principal principal) {
        return comments.list(id, principal == null ? null : principal.getName());
    }

    @PostMapping("/api/products/{id}/comments")
    Map<String, Boolean> comment(@PathVariable Long id, Principal principal, @RequestParam String content) {
        comments.add(principal.getName(), id, content);
        return Map.of("saved", true);
    }

    @PostMapping("/api/products/{id}/reply")
    Map<String, Boolean> reply(@PathVariable Long id, Principal principal, @RequestParam Long commentId, @RequestParam String content) {
        comments.reply(principal.getName(), id, commentId, content);
        return Map.of("saved", true);
    }

    @PostMapping("/api/seller/save")
    Map<String, Object> save(Principal principal, @RequestParam(required = false) Long id, @ModelAttribute Product input,
                             @RequestParam int quantity, @RequestParam(defaultValue = "0") int expectedStock,
                             @RequestParam(defaultValue = "0") long expectedRevision, @RequestParam boolean publish) {
        Product product = publishing.save(principal.getName(), id, input, quantity, expectedStock, expectedRevision, publish);
        return Map.of("id", product.getId(), "status", product.getStatus().name(), "stock", product.getStock(), "revision", product.getListingRevision());
    }

    @PostMapping("/api/media")
    Map<String, String> upload(Principal principal, @RequestParam MultipartFile file) throws IOException {
        return Map.of("url", media.upload(principal.getName(), file));
    }

    @PostMapping("/api/products/{id}/buy")
    Map<String, Object> buy(Principal principal, @PathVariable Long id, @RequestParam int quantity,
                            @RequestParam BigDecimal displayedPrice, @RequestParam String requestKey) {
        PurchaseOrder order = trading.buy(principal.getName(), id, quantity, displayedPrice, requestKey);
        return Map.of("id", order.getId(), "url", "/orders/" + order.getId());
    }

    @PostMapping("/api/products/{id}/reserve")
    Map<String, String> reserve(Principal principal, @PathVariable Long id, @RequestParam int quantity) {
        trading.reserve(principal.getName(), id, quantity);
        return Map.of("url", "/reservations");
    }

    @PostMapping("/api/products/{id}/bargain")
    Map<String, String> bargain(Principal principal, @PathVariable Long id, @RequestParam int quantity, @RequestParam BigDecimal price, @RequestParam(defaultValue = "") String message) {
        bargains.propose(principal.getName(), id, quantity, price, message);
        return Map.of("url", "/bargains");
    }
}
