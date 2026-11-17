package edu.market;

import java.io.IOException;
import java.sql.SQLException;
import java.security.Principal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BackupController {
    private final BackupService backups;

    public BackupController(BackupService backups) {
        this.backups = backups;
    }

    @PostMapping("/admin/backup")
    ResponseEntity<FileSystemResource> backup(Principal principal) throws IOException, SQLException {
        var archive = backups.create(principal.getName());
        return ResponseEntity.ok().header("Content-Type", "application/zip")
                .header("Content-Disposition", "attachment; filename=market-backup.zip")
                .body(new FileSystemResource(archive));
    }
}
