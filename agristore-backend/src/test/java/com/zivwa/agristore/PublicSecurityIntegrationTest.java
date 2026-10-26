package com.zivwa.agristore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.firebase.enabled=false", "app.auth-mode=local", "app.catalog-source=demo"})
@AutoConfigureMockMvc
class PublicSecurityIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void healthIsPublic() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void categoriesArePublic() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
    @Test void productsArePublic() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").exists());
    }
    @Test void cartAndAccountRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void adminRequiresLogin() throws Exception {
        mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
    }
}
