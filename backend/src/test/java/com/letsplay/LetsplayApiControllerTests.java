package com.letsplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.letsplay.dto.LoginRequest;
import com.letsplay.dto.ProductRequest;
import com.letsplay.dto.RegisterRequest;
import com.letsplay.model.Product;
import com.letsplay.model.User;
import com.letsplay.repository.ProductRepository;
import com.letsplay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * High-priority integration test suite verifying core functional,
 * security, and role-based access control requirements per the audit specification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LetsplayApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User normalUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Seed Administrator
        adminUser = User.builder()
                .name("Admin User")
                .email("admin@letsplay.com")
                .password(passwordEncoder.encode("admin123"))
                .role("ADMIN")
                .build();
        adminUser = userRepository.save(adminUser);

        // 2. Seed Standard User
        normalUser = User.builder()
                .name("Normal User")
                .email("user@letsplay.com")
                .password(passwordEncoder.encode("user123"))
                .role("USER")
                .build();
        normalUser = userRepository.save(normalUser);

        // 3. Authenticate and retrieve JWT tokens
        adminToken = authenticateAndGetToken("admin@letsplay.com", "admin123");
        userToken = authenticateAndGetToken("user@letsplay.com", "user123");
    }

    private String authenticateAndGetToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Public Access: Unauthenticated clients can retrieve public products")
    void testPublicGetProducts() throws Exception {
        Product p = Product.builder()
                .name("Gaming Laptop")
                .description("RTX 4080 Laptop")
                .price(1899.99)
                .userId(normalUser.getId())
                .build();
        p = productRepository.save(p);

        // GET /products without Authorization header
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Gaming Laptop")));

        // GET /products/{id} without Authorization header
        mockMvc.perform(get("/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Gaming Laptop")))
                .andExpect(jsonPath("$.price", is(1899.99)));
    }

    @Test
    @DisplayName("Authentication: User registration and login return valid JWT tokens")
    void testUserRegistrationAndLogin() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Pro Player")
                .email("proplayer@letsplay.com")
                .password("securePass123")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("proplayer@letsplay.com")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("Security: Public registration ignores or disallows ADMIN role and strictly assigns USER")
    void testAdminRegistrationDisallowedAndEnforcesUserRole() throws Exception {
        // Attempt to pass "role": "ADMIN" in registration payload
        String maliciousPayload = """
                {
                    "name": "Wannabe Admin",
                    "email": "wannabe@letsplay.com",
                    "password": "password123",
                    "role": "ADMIN"
                }
                """;

        MvcResult res = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("USER"))) // Must be USER, never ADMIN
                .andReturn();

        String hackerToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();

        // Verify the user cannot access admin-only /users directory (403)
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + hackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Admin endpoints are restricted to ADMIN and forbidden for standard USER")
    void testRoleBasedAccessControl() throws Exception {
        // Standard user denied access to user directory (403)
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Admin granted access to user directory (200)
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Product Lifecycle & Ownership Isolation: Users manage own products, cannot tamper with others")
    void testProductCrudAndOwnerIsolation() throws Exception {
        // 1. Normal user creates a product
        ProductRequest createReq = ProductRequest.builder()
                .name("Mechanical Keyboard")
                .description("RGB Cherry MX Red")
                .price(129.99)
                .build();

        MvcResult createRes = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(normalUser.getId())))
                .andReturn();

        String productId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // 2. Owner updates product
        ProductRequest updateReq = ProductRequest.builder()
                .name("Custom Mechanical Keyboard")
                .description("RGB Cherry MX Red with custom keycaps")
                .price(149.99)
                .build();

        mockMvc.perform(put("/products/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Custom Mechanical Keyboard")));

        // 3. Create Admin product and verify normal user cannot delete it (403 Forbidden)
        Product adminProduct = productRepository.save(Product.builder()
                .name("Admin Ultrawide Monitor")
                .description("49 inch OLED")
                .price(1299.0)
                .userId(adminUser.getId())
                .build());

        mockMvc.perform(delete("/products/" + adminProduct.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        assertTrue(productRepository.findById(adminProduct.getId()).isPresent());

        // 4. Owner deletes own product (204 No Content)
        mockMvc.perform(delete("/products/" + productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        assertFalse(productRepository.findById(productId).isPresent());
    }

    @Test
    @DisplayName("Cascade Delete: Deleting a user cascade-removes all associated products")
    void testCascadeDeleteUserDeletesProducts() throws Exception {
        productRepository.save(Product.builder()
                .name("Product 1")
                .description("Desc 1")
                .price(10.0)
                .userId(normalUser.getId())
                .build());
        productRepository.save(Product.builder()
                .name("Product 2")
                .description("Desc 2")
                .price(20.0)
                .userId(normalUser.getId())
                .build());

        assertEquals(2, productRepository.findByUserId(normalUser.getId()).size());

        // Admin deletes normal user
        mockMvc.perform(delete("/users/" + normalUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify user and cascading products are removed
        assertFalse(userRepository.findById(normalUser.getId()).isPresent());
        assertTrue(productRepository.findByUserId(normalUser.getId()).isEmpty());
    }

    @Test
    @DisplayName("Exception Handling: Validation errors return 400 and missing resources return 404")
    void testExceptionHandlingAndValidation() throws Exception {
        // Validation failure (blank name, negative price)
        ProductRequest invalidReq = ProductRequest.builder()
                .name("")
                .description("Invalid")
                .price(-5.0)
                .build();

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors", notNullValue()));

        // Non-existing resource lookup returns structured 404
        mockMvc.perform(get("/products/nonexistent-id-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}
