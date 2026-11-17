package edu.market;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformService {
    private final AccountService accounts;
    private final AccountRepository store;
    private final PlatformRuleRepository rules;
    private final ActivityService activities;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public PlatformService(AccountService accounts, AccountRepository store,
                           PlatformRuleRepository rules, ActivityService activities, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.accounts = accounts;
        this.store = store;
        this.rules = rules;
        this.activities = activities;
        this.encoder = encoder;
    }

    public void requireSuper(String username) {
        Account actor = accounts.current(username);
        accounts.requireRole(actor, "ADMIN");
        BusinessException.require(actor.getSuperAdmin(), "仅超级管理员可管理平台规则和管理员");
    }

    public void configure(String username, BigDecimal commission, BigDecimal promotion) {
        requireSuper(username);
        BusinessException.require(commission != null && commission.signum() >= 0
                && commission.compareTo(BigDecimal.ONE) <= 0, "抽成比例为 0–1");
        BusinessException.require(promotion != null && promotion.signum() >= 0
                && promotion.compareTo(new BigDecimal("0.9")) <= 0, "促销折扣比例为 0–0.9");
        PlatformRule rule = rules.locked().orElseGet(() -> rules.save(new PlatformRule()));
        rule.setCommissionRate(commission);
        rule.setPromotionRate(promotion);
        activities.record(username, "PLATFORM", 1L, "RULES", "UPDATED");
    }

    public void createAdmin(String username, String name, String password) {
        requireSuper(username);
        BusinessException.require(name != null && name.matches("[a-zA-Z0-9_]{3,40}"), "用户名格式无效");
        AccountService.validatePassword(password);
        BusinessException.require(store.findByUsername(name).isEmpty(), "账户已存在");
        // Reuse the registration encoder without exposing its hash configuration to the controller.
        store.save(new Account(name, encoder.encode(password), "ADMIN"));
        activities.record(username, "PLATFORM", 1L, "CREATE_ADMIN", name);
    }

    public void adminStatus(String username, String target, boolean disabled) {
        requireSuper(username);
        Account admin = accounts.current(target);
        BusinessException.require(admin.getRole().equals("ADMIN") && !admin.getSuperAdmin(), "只能调整普通管理员");
        admin.setBanned(disabled);
        activities.record(username, "ACCOUNT", admin.getId(), "ADMIN_STATUS", disabled ? "DISABLED" : "ENABLED");
    }
}
