package edu.market;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:account;DB_CLOSE_DELAY=-1")
@Transactional
class AccountTest {
    @Autowired
    private AccountService service;
    @Autowired
    private PasswordEncoder encoder;

    @Test
    void registrationHashesPasswordsAndRejectsInvalidAccounts() {
        Account account = service.register("buyer", "Valid123!", "BUYER");
        assertTrue(encoder.matches("Valid123!", account.getPasswordHash()));
        assertThrows(BusinessException.class, () -> service.register("buyer", "Valid123!", "BUYER"));
        assertThrows(BusinessException.class, () -> service.register("admin", "Valid123!", "ADMIN"));
        assertThrows(BusinessException.class, () -> AccountService.validatePassword("weak"));
        service.profile("buyer", "新昵称", "学校宿舍");
        assertEquals("学校宿舍", service.current("buyer").getAddress());
        assertThrows(BusinessException.class, () -> service.password("buyer", "wrong", "Valid456!"));
    }
}
