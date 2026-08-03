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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LetsplayApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userToken;
    private String adminToken;
    private User testUser;
    private User testAdmin;
    private Product testProduct;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create a standard User
        RegisterRequest userRegister = RegisterRequest.builder()
                .name("Standard User")
                .email("user@letsplay.com")
                .password("password123")
                .role("USER")
                .build();

        MvcResult userResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRegister)))
                .andExpect(status().isCreated())
                .andReturn();

        String userResponseString = userResult.getResponse().getContentAsString();
        Map<?, ?> userResponseMap = objectMapper.readValue(userResponseString, Map.class);
        userToken = "Bearer " + userResponseMap.get("token");
        
        testUser = userRepository.findByEmail("user@letsplay.com").orElseThrow();

        // 2. Create an Admin
        RegisterRequest adminRegister = RegisterRequest.builder()
                .name("Admin User")
                .email("admin@letsplay.com")
                .password("password123")
                .role("ADMIN")
                .build();

        MvcResult adminResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegister)))
                .andExpect(status().isCreated())
                .andReturn();

        String adminResponseString = adminResult.getResponse().getContentAsString();
        Map<?, ?> adminResponseMap = objectMapper.readValue(adminResponseString, Map.class);
        adminToken = "Bearer " + adminResponseMap.get("token");

        testAdmin = userRepository.findByEmail("admin@letsplay.com").orElseThrow();

        // 3. Create a test product owned by standard user
        Product product = Product.builder()
                .name("PS5 Console")
                .description("Next-gen gaming console")
                .price(499.99)
                .userId(testUser.getId())
                .build();
        testProduct = productRepository.save(product);
    }

    @Test
    void testPublicGetProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("PS5 Console")))
                .andExpect(jsonPath("$[0].price", is(499.99)))
                .andExpect(jsonPath("$[0].userId", is(testUser.getId())));
    }

    @Test
    void testCreateProductUnauthorized() throws Exception {
        ProductRequest newProduct = ProductRequest.builder()
                .name("Xbox Series X")
                .description("Microsoft console")
                .price(499.99)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    void testCreateProductSuccess() throws Exception {
        ProductRequest newProduct = ProductRequest.builder()
                .name("Xbox Series X")
                .description("Microsoft console")
                .price(499.99)
                .build();

        mockMvc.perform(post("/products")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Xbox Series X")))
                .andExpect(jsonPath("$.userId", is(testUser.getId())));
    }

    @Test
    void testUpdateProductByOwnerSuccess() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("PS5 Console Slim")
                .description("Sleeker version")
                .price(449.99)
                .build();

        mockMvc.perform(put("/products/" + testProduct.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PS5 Console Slim")))
                .andExpect(jsonPath("$.price", is(449.99)));
    }

    @Test
    void testUpdateProductByNonOwnerForbidden() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("PS5 Console Slim")
                .description("Sleeker version")
                .price(449.99)
                .build();

        // Let's register another user
        RegisterRequest user2Register = RegisterRequest.builder()
                .name("Other User")
                .email("other@letsplay.com")
                .password("password123")
                .role("USER")
                .build();

        MvcResult user2Result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Register)))
                .andExpect(status().isCreated())
                .andReturn();

        String user2ResponseString = user2Result.getResponse().getContentAsString();
        Map<?, ?> user2ResponseMap = objectMapper.readValue(user2ResponseString, Map.class);
        String user2Token = "Bearer " + user2ResponseMap.get("token");

        mockMvc.perform(put("/products/" + testProduct.getId())
                        .header("Authorization", user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("You do not have permission")));
    }

    @Test
    void testUpdateProductByAdminSuccess() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("PS5 Console Pro")
                .description("Faster console")
                .price(599.99)
                .build();

        mockMvc.perform(put("/products/" + testProduct.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PS5 Console Pro")));
    }

    @Test
    void testDeleteProductByOwnerSuccess() throws Exception {
        mockMvc.perform(delete("/products/" + testProduct.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        assertTrue(productRepository.findById(testProduct.getId()).isEmpty());
    }

    @Test
    void testDeleteProductByAdminSuccess() throws Exception {
        mockMvc.perform(delete("/products/" + testProduct.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        assertTrue(productRepository.findById(testProduct.getId()).isEmpty());
    }

    @Test
    void testGetUsersByAdminSuccess() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // Standard User and Admin User
    }

    @Test
    void testGetUsersByUserForbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("You do not have permission")));
    }

    @Test
    void testDeleteUserCascadeDeletesProducts() throws Exception {
        // Assert product exists beforehand
        assertTrue(productRepository.findById(testProduct.getId()).isPresent());

        // Delete the user via Admin request
        mockMvc.perform(delete("/users/" + testUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        assertTrue(userRepository.findById(testUser.getId()).isEmpty());

        // Verify user's product is cascade deleted (via event publishing)
        assertTrue(productRepository.findById(testProduct.getId()).isEmpty());
    }
}
