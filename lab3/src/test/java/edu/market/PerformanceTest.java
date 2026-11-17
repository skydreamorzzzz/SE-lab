package edu.market;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:file:./target/performance/market;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.thymeleaf.cache=true",
        "market.media-dir=./target/performance/media", "market.backup-dir=./target/performance/backups"})
class PerformanceTest {
    @LocalServerPort
    private int port;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private AccountService accountService;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BackupService backups;

    @Test
    void tenThousandProductsWithTenRealHttpClients() throws Exception {
        String password = "Perf123!";
        Account seller = accounts.save(new Account("perfseller", encoder.encode(password), "SELLER"));
        for (int i = 0; i < 10; i++) {
            accountService.register("perfuser" + i, password, "BUYER");
            accountService.profile("perfuser" + i, "性能验收用户", "性能测试虚拟地址");
        }
        jdbc.update("insert into product (id, seller_id, name, category, condition_label, description, image_url, "
                + "min_price, max_price, stock, reservation_limit, status, published_at) "
                + "select 1000000+x, ?, concat('性能商品', x), '学习/书籍', '9成新', '非空性能验收数据', "
                + "'/images/sample.svg', 10, 10, 1000, 5, 'ON_SALE', CURRENT_TIMESTAMP from system_range(1, 10000)", seller.getId());
        List<Session> clients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            clients.add(login("perfuser" + i, password));
        }
        for (int i = 0; i < 20; i++) {
            assertEquals(200, get(clients.get(i % 10), "/?keyword=%E6%80%A7%E8%83%BD").statusCode());
        }
        List<Double> listTimes = Collections.synchronizedList(new ArrayList<>());
        List<Double> searchTimes = Collections.synchronizedList(new ArrayList<>());
        List<Double> buyTimes = Collections.synchronizedList(new ArrayList<>());
        List<Double> payTimes = Collections.synchronizedList(new ArrayList<>());
        try (var pool = Executors.newFixedThreadPool(10)) {
            List<Callable<Void>> browse = new ArrayList<>();
            for (Session client : clients) {
                browse.add(() -> {
                    for (int i = 0; i < 10; i++) {
                        measureGet(client, "/", listTimes);
                        measureGet(client, "/?keyword=%E6%80%A7%E8%83%BD&category=%E5%AD%A6%E4%B9%A0", searchTimes);
                    }
                    return null;
                });
            }
            for (var future : pool.invokeAll(browse)) {
                future.get();
            }
            List<Callable<Void>> transactions = new ArrayList<>();
            for (Session client : clients) {
                transactions.add(() -> {
                    for (int i = 0; i < 10; i++) {
                        long start = System.nanoTime();
                        var created = post(client, "/buy/1000001", Map.of("quantity", "1", "displayedPrice", "10",
                                "requestKey", UUID.randomUUID().toString()));
                        buyTimes.add(millis(start));
                        assertEquals(302, created.statusCode(), created.body());
                        String order = created.headers().firstValue("Location").orElseThrow();
                        start = System.nanoTime();
                        var paid = post(client, URI.create(order).getPath() + "/pay", Map.of());
                        payTimes.add(millis(start));
                        assertEquals(302, paid.statusCode(), paid.body());
                    }
                    return null;
                });
            }
            for (var future : pool.invokeAll(transactions)) {
                future.get();
            }
        }
        String json = "{\n  \"products\": 10000, \"clients\": 10, \"requestsPerOperation\": 100,\n"
                + "  \"listP95Ms\": " + p95(listTimes) + ", \"searchP95Ms\": " + p95(searchTimes) + ",\n"
                + "  \"buyP95Ms\": " + p95(buyTimes) + ", \"payP95Ms\": " + p95(payTimes) + "\n}\n";
        Files.writeString(Path.of("target/performance-results.json"), json);
        assertTrue(p95(listTimes) <= 300, json);
        assertTrue(p95(searchTimes) <= 300, json);
        assertTrue(p95(buyTimes) <= 1000, json);
        assertTrue(p95(payTimes) <= 1000, json);
        assertEquals(100, jdbc.queryForObject("select count(*) from purchase_order where status='PAID'", Integer.class));
        assertEquals(900, jdbc.queryForObject("select stock from product where id=1000001", Integer.class));
    }

    @Test
    void onlineBackupRestoresDatabaseAndContainsMedia() throws Exception {
        Account admin = new Account("backupadmin", encoder.encode("Backup123!"), "ADMIN");
        admin.setSuperAdmin(true);
        accounts.save(admin);
        Path media = Path.of("target/performance/media");
        Files.createDirectories(media);
        Files.writeString(media.resolve("fixture.txt"), "persistent media fixture");
        Path archive = backups.create("backupadmin");
        Path restored = Path.of("target/performance/restored", UUID.randomUUID().toString()).toAbsolutePath();
        Files.createDirectories(restored);
        boolean mediaFound = false;
        Path restoredDatabase = null;
        try (var zip = new java.util.zip.ZipInputStream(Files.newInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("media/fixture.txt")) {
                    mediaFound = true;
                }
                if (entry.getName().equals("database.zip")) {
                    byte[] bytes = zip.readAllBytes();
                    try (var nested = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
                        java.util.zip.ZipEntry dbEntry;
                        while ((dbEntry = nested.getNextEntry()) != null) {
                            Path destination = restored.resolve(dbEntry.getName()).normalize();
                            assertTrue(destination.startsWith(restored));
                            Files.createDirectories(destination.getParent());
                            Files.copy(nested, destination);
                            if (destination.toString().endsWith(".mv.db")) {
                                restoredDatabase = destination;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(mediaFound);
        assertTrue(restoredDatabase != null);
        String filename = restoredDatabase.toString();
        try (var connection = java.sql.DriverManager.getConnection("jdbc:h2:file:" + filename.substring(0, filename.length() - 6), "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from accounts where username='backupadmin'")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    private Session login(String username, String password) throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();
        Session anonymous = new Session(client, "");
        String token = csrf(get(anonymous, "/login").body());
        var response = post(new Session(client, token), "/login", Map.of("username", username, "password", password));
        assertEquals(302, response.statusCode());
        return new Session(client, csrf(get(anonymous, "/profile").body()));
    }

    private String csrf(String html) {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "CSRF input not found");
        return matcher.group(1);
    }

    private void measureGet(Session client, String path, List<Double> times) throws Exception {
        long start = System.nanoTime();
        var response = get(client, path);
        times.add(millis(start));
        assertEquals(200, response.statusCode());
    }

    private HttpResponse<String> get(Session client, String path) throws Exception {
        return client.http().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(Session client, String path, Map<String, String> values) throws Exception {
        StringBuilder body = new StringBuilder("_csrf=" + encode(client.csrf()));
        values.forEach((key, value) -> body.append('&').append(encode(key)).append('=').append(encode(value)));
        return client.http().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double millis(long start) {
        return (System.nanoTime() - start) / 1000000.0;
    }

    private double p95(List<Double> values) {
        assertEquals(100, values.size());
        return values.stream().sorted().toList().get(94);
    }

    private record Session(HttpClient http, String csrf) {
    }
}
