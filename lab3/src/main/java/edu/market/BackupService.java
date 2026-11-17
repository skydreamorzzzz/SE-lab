package edu.market;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BackupService {
    private final DataSource database;
    private final PlatformService platform;
    private final ActivityService activities;
    private final Path media;
    private final Path backups;

    public BackupService(DataSource database, PlatformService platform, ActivityService activities,
                         @Value("${market.media-dir:./data/media}") String mediaDirectory,
                         @Value("${market.backup-dir:./data/backups}") String backupDirectory) {
        this.database = database;
        this.platform = platform;
        this.activities = activities;
        media = Path.of(mediaDirectory).toAbsolutePath().normalize();
        backups = Path.of(backupDirectory).toAbsolutePath().normalize();
    }

    public synchronized Path create(String username) throws IOException, SQLException {
        platform.requireSuper(username);
        Files.createDirectories(backups);
        String key = UUID.randomUUID().toString();
        Path databaseZip = backups.resolve("database-" + key + ".zip");
        Path bundle = backups.resolve("market-backup-" + key + ".zip");
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            // H2's online BACKUP command takes a transactionally consistent database snapshot.
            statement.execute("BACKUP TO '" + databaseZip.toString().replace("'", "''") + "'");
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
            zip.putNextEntry(new ZipEntry("database.zip"));
            Files.copy(databaseZip, zip);
            zip.closeEntry();
            if (Files.isDirectory(media)) {
                try (var files = Files.walk(media)) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        zip.putNextEntry(new ZipEntry("media/" + media.relativize(file).toString().replace('\\', '/')));
                        Files.copy(file, zip);
                        zip.closeEntry();
                    }
                }
            }
        }
        Files.delete(databaseZip);
        activities.record(username, "PLATFORM", 1L, "BACKUP", "CREATED");
        return bundle;
    }
}
