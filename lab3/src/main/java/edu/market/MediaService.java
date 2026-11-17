package edu.market;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class MediaService {
    private final AccountService accounts;
    private final MediaRepository assets;
    private final ProductRepository products;
    private final ProductService productService;
    private final Path root;

    public MediaService(AccountService accounts, MediaRepository assets, ProductRepository products,
                        ProductService productService, @Value("${market.media-dir:./data/media}") String directory) {
        this.accounts = accounts;
        this.assets = assets;
        this.products = products;
        this.productService = productService;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String upload(String username, MultipartFile file) throws IOException {
        Account actor = accounts.current(username);
        BusinessException.require(!actor.getBanned() && !actor.getRole().equals("ADMIN"), "仅正常买卖双方可上传");
        BusinessException.require(!file.isEmpty() && file.getSize() <= 30L * 1024 * 1024, "文件为空或超过30MB");
        byte[] data = file.getBytes();
        String extension;
        String type;
        if (data.length > 8 && data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            extension = ".png";
            type = "image/png";
        } else if (data.length > 3 && data[0] == (byte) 0xff && data[1] == (byte) 0xd8 && data[2] == (byte) 0xff) {
            extension = ".jpg";
            type = "image/jpeg";
        } else if (data.length > 12 && ascii(data, 0, "RIFF") && ascii(data, 8, "WEBP")) {
            extension = ".webp";
            type = "image/webp";
        } else if (data.length > 12 && ascii(data, 4, "ftyp")) {
            extension = ".mp4";
            type = "video/mp4";
        } else if (data.length > 4 && data[0] == 0x1a && data[1] == 0x45 && data[2] == (byte) 0xdf && data[3] == (byte) 0xa3) {
            extension = ".webm";
            type = "video/webm";
        } else {
            throw new BusinessException("只支持PNG/JPEG/WebP图片及MP4/WebM视频；文件内容与格式须一致");
        }
        if (type.startsWith("image/")) {
            BusinessException.require(data.length <= 10L * 1024 * 1024, "图片不超过10MB");
        }
        Files.createDirectories(root);
        String name = UUID.randomUUID() + extension;
        Path destination = root.resolve(name).normalize();
        BusinessException.require(destination.startsWith(root), "媒体路径无效");
        Files.write(destination, data);
        MediaAsset asset = new MediaAsset();
        asset.setOwnerId(actor.getId());
        asset.setStoredName(name);
        asset.setContentType(type);
        asset.setByteSize(file.getSize());
        assets.save(asset);
        return "/media/" + name;
    }

    public MediaAsset metadata(String username, String name) {
        BusinessException.require(name.matches("[a-zA-Z0-9.-]+"), "路径无效");
        MediaAsset asset = assets.findByStoredName(name).orElseThrow(() -> new BusinessException("媒体不存在"));
        Account viewer = username == null ? null : accounts.current(username);
        boolean owner = viewer != null && viewer.getId().equals(asset.getOwnerId());
        boolean admin = viewer != null && viewer.getRole().equals("ADMIN");
        boolean published = products.usingMedia("/media/" + name).stream()
                .anyMatch(p -> productService.isPublic(p) && !productService.sellerBanned(p));
        BusinessException.require(owner || admin || published, "未公开商品的媒体不可访问");
        return asset;
    }

    public FileSystemResource resource(String name) {
        Path file = root.resolve(name).normalize();
        BusinessException.require(file.startsWith(root) && Files.isRegularFile(file), "媒体不存在");
        return new FileSystemResource(file);
    }

    private boolean ascii(byte[] data, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            if (data[offset + i] != value.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
