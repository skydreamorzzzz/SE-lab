package edu.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class WebTest {
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/login", "/register"})
    void publicPagesRenderWithoutTemplateErrors(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(containsString("拾物集")));
    }

    @Test
    void anonymousProtectedPageRedirectsAndPostNeedsCsrf() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/register").param("username", "csrfcheck").param("password", "Valid123!")
                .param("role", "ADMIN")).andExpect(status().isForbidden());
    }
}
