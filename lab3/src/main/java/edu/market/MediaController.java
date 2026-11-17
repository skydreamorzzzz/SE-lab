package edu.market;

import java.io.IOException;
import java.security.Principal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
@Controller
public class MediaController {
    private final MediaService media;
    private final AccountService accounts;
    public MediaController(MediaService media, AccountService accounts) {
        this.media = media;
        this.accounts = accounts;
    }
    @GetMapping("/media/upload")
    String form(Principal principal, Model model) {
        BusinessException.require(principal != null, "请登录后上传");
        model.addAttribute("viewer", accounts.current(principal.getName()));
        return "upload";
    }
    @PostMapping("/media/upload")
    String upload(Principal principal, @RequestParam MultipartFile file, Model model) throws IOException {
        form(principal, model);
        model.addAttribute("url", media.upload(principal.getName(), file));
        return "upload";
    }
    @GetMapping("/media/{name:.+}")
    ResponseEntity<FileSystemResource> media(Principal principal, @PathVariable String name) {
        MediaAsset asset = media.metadata(principal == null ? null : principal.getName(), name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()))
                .header("X-Content-Type-Options", "nosniff").body(media.resource(name));
    }
}
