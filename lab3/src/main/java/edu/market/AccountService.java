package edu.market;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public Account register(String username, String password, String role) {
        BusinessException.require(username != null && username.matches("[a-zA-Z0-9_]{3,40}"),
                "用户名需为 3–40 位字母、数字或下划线");
        validatePassword(password);
        BusinessException.require("BUYER".equals(role) || "SELLER".equals(role), "不能注册管理员");
        BusinessException.require(accounts.findByUsername(username).isEmpty(), "用户名已存在");
        return accounts.save(new Account(username, encoder.encode(password), role));
    }

    public Account current(String username) {
        return accounts.findByUsername(username).orElseThrow(() -> new BusinessException("请先登录"));
    }

    public void profile(String username, String name, String address) {
        BusinessException.require(name != null && !name.isBlank() && name.length() <= 60, "昵称为 1–60 字");
        BusinessException.require(address != null && address.length() <= 200, "地址不超过 200 字");
        current(username).updateProfile(name.strip(), address.strip());
    }

    public void password(String username, String oldPassword, String newPassword) {
        Account account = current(username);
        BusinessException.require(encoder.matches(oldPassword, account.getPasswordHash()), "原密码不正确");
        validatePassword(newPassword);
        account.changePassword(encoder.encode(newPassword));
    }

    public void requireRole(Account account, String role) {
        BusinessException.require(!account.getBanned(), "账户已封禁，可通过申诉入口申请复核");
        BusinessException.require(account.getRole().equals(role), "无权执行此操作");
    }

    public static void validatePassword(String password) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        String value = password == null ? "" : password;
        if (value.length() < 8) {
            missing.add("至少8个字符");
        }
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            missing.add("UTF-8编码不超过72字节");
        }
        if (!value.matches("(?s).*[A-Z].*")) {
            missing.add("大写字母");
        }
        if (!value.matches("(?s).*[a-z].*")) {
            missing.add("小写字母");
        }
        if (!value.matches("(?s).*[0-9].*")) {
            missing.add("数字");
        }
        if (!value.matches("(?s).*[^a-zA-Z0-9].*")) {
            missing.add("特殊字符");
        }
        BusinessException.require(missing.isEmpty(), "密码要求未满足：" + String.join("、", missing));
    }
}
