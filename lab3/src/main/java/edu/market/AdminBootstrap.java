package edu.market;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public AdminBootstrap(AccountRepository accounts, PasswordEncoder encoder, Environment environment) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        String superUser = environment.getProperty("MARKET_SUPER_ADMIN_USER");
        String superPassword = environment.getProperty("MARKET_SUPER_ADMIN_PASSWORD");
        if (superUser != null && superPassword != null && accounts.findByUsername(superUser).isEmpty()) {
            BusinessException.require(superUser.matches("[a-zA-Z0-9_]{3,40}"), "超级管理员用户名格式无效");
            AccountService.validatePassword(superPassword);
            Account superAdmin = new Account(superUser, encoder.encode(superPassword), "ADMIN");
            superAdmin.setSuperAdmin(true);
            accounts.save(superAdmin);
        }

        String username = environment.getProperty("MARKET_ADMIN_USER");
        String password = environment.getProperty("MARKET_ADMIN_PASSWORD");
        if (username != null && password != null && accounts.findByUsername(username).isEmpty()) {
            BusinessException.require(username.matches("[a-zA-Z0-9_]{3,40}"), "管理员用户名格式无效");
            AccountService.validatePassword(password);
            accounts.save(new Account(username, encoder.encode(password), "ADMIN"));
        }
    }
}
