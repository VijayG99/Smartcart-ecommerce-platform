package com.smartcart.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.entity.Category;
import com.smartcart.entity.Role;
import com.smartcart.entity.User;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {
    private static final String PASSWORD = "Password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired PasswordEncoder encoder;

    private String json(Object o) throws Exception { return om.writeValueAsString(o); }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String adminToken() throws Exception {
        String email = TestData.email();
        users.save(User.builder().name("Admin").email(email).password(encoder.encode(PASSWORD)).role(Role.ADMIN).build());
        return login(email);
    }

    private Map<String, Object> productBody(Long categoryId, String name, String price) {
        return Map.of("name", name, "description", "test product", "price", price,
                "sku", TestData.sku(), "categoryId", categoryId, "initialQuantity", 10);
    }

    @Test
    void registerThenLoginReturnsJwtAndNeverExposesPassword() throws Exception {
        String email = TestData.email();
        register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejectedWithConflict() throws Exception {
        String email = TestData.email();
        register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Again", "email", email, "password", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationValidationErrorsAreReportedPerField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = TestData.email();
        register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointNeedsAValidJwt() throws Exception {
        String email = TestData.email();
        register(email);
        String token = login(email);

        mvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        mvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void customerCannotCreateProductButAdminCan() throws Exception {
        Category cat = categories.save(Category.builder().name(TestData.name("cat")).build());
        String email = TestData.email();
        register(email);
        String customerToken = login(email);
        String body = json(productBody(cat.getId(), "Desk Lamp", "25.50"));

        mvc.perform(post("/api/products").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void anonymousUserCanBrowseAndSearchProducts() throws Exception {
        Category cat = categories.save(Category.builder().name(TestData.name("cat")).build());
        String token = adminToken();
        String unique = TestData.name("gadget");
        for (String[] p : new String[][]{{unique + " cheap", "20.00"}, {unique + " pricey", "500.00"}}) {
            mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(json(productBody(cat.getId(), p[0], p[1]))))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/products").param("q", unique).param("maxPrice", "100")
                        .param("categoryId", String.valueOf(cat.getId())).param("sortBy", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value(unique + " cheap"));

        mvc.perform(get("/api/products").param("q", unique).param("sortBy", "price").param("direction", "desc"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value(unique + " pricey"));
    }

    @Test
    void unknownProductReturnsStructured404() throws Exception {
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
