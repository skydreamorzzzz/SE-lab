package edu.market;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authorization;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class AuthorizationTest {
    @Autowired
    private AccountService accounts;
    @Autowired
    private AccountRepository store;
    @Autowired
    private MockMvc mvc;

    @Test
    void existingBannedSessionCannotMutateButCanOpenAppeal() throws Exception {
        Account seller = accounts.register("bannedseller", "Valid123!", "SELLER");
        seller.setBanned(true);
        mvc.perform(post("/seller/save").with(user("bannedseller").roles("SELLER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/moderation").with(user("bannedseller").roles("SELLER")))
                .andExpect(status().isOk());
    }

    @Test
    void ordinaryAdminCannotConfigureRulesAndDisabledAdminLosesExistingSessionAccess() throws Exception {
        Account admin = store.save(new Account("plainadmin", "unused-hash", "ADMIN"));
        mvc.perform(post("/admin/platform/rules").with(user("plainadmin").roles("ADMIN")).with(csrf())
                .param("commission", "0.1").param("promotion", "0"))
                .andExpect(status().isBadRequest());
        admin.setBanned(true);
        mvc.perform(get("/admin").with(user("plainadmin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
