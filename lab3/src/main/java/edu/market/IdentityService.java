package edu.market;

import java.time.Instant;
import java.time.Duration;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class IdentityService {
    private final AccountService accounts;
    private final AccountRepository store;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    public IdentityService(AccountService accounts, AccountRepository store, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.store = store;
        this.encoder = encoder;
    }
    public String recoveryCode(String username, String password) {
        Account actor = lock(username);
        BusinessException.require(encoder.matches(password, actor.getPasswordHash()), "密码验证失败");
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String code = HexFormat.of().formatHex(bytes);
        actor.setRecoveryHash(encoder.encode(code));
        return code;
    }
    public void recover(String username, String code, String password) {
        Account actor = lock(username);
        BusinessException.require(actor.getRecoveryHash() != null && code != null
                && encoder.matches(code, actor.getRecoveryHash()), "恢复凭证无效");
        AccountService.validatePassword(password);
        actor.changePassword(encoder.encode(password));
        actor.setRecoveryHash(null);
    }
    public String phoneChallenge(String username, String password, String phone) {
        Account actor = lock(username);
        BusinessException.require(encoder.matches(password, actor.getPasswordHash()), "密码验证失败");
        BusinessException.require(phone != null && phone.matches("1[3-9][0-9]{9}"), "手机号格式无效");
        BusinessException.require(store.findByPhone(phone).map(a -> a.getId().equals(actor.getId())).orElse(true), "手机号已绑定其他账户");
        String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1000000));
        actor.setPendingPhone(phone);
        actor.setPhoneChallengeHash(encoder.encode(code));
        actor.setPhoneChallengeExpires(Instant.now().plus(Duration.ofMinutes(5)));
        actor.setPhoneAttempts(0);
        return code;
    }
    // An invalid attempt returns false so its counter commits instead of rolling back with an exception.
    public boolean bindPhone(String username, String code) {
        Account actor = lock(username);
        BusinessException.require(actor.getPhoneChallengeHash() != null && actor.getPhoneAttempts() < 5
                && actor.getPhoneChallengeExpires().isAfter(Instant.now()), "验证码已失效或尝试次数过多");
        actor.setPhoneAttempts(actor.getPhoneAttempts() + 1);
        if (code == null || !encoder.matches(code, actor.getPhoneChallengeHash())) {
            return false;
        }
        actor.setPhone(actor.getPendingPhone());
        actor.setPendingPhone(null);
        actor.setPhoneChallengeHash(null);
        return true;
    }
    private Account lock(String username) {
        Long id = accounts.current(username).getId();
        return store.locked(id).orElseThrow();
    }
}
