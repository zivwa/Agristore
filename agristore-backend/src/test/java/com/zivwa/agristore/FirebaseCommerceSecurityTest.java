package com.zivwa.agristore;
import com.fasterxml.jackson.databind.*;
import com.google.firebase.auth.*;
import com.zivwa.agristore.api.CloudPersistence;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.auth-mode=firebase-commerce","app.firebase.enabled=false"})
@AutoConfigureMockMvc
class FirebaseCommerceSecurityTest {
 @MockBean FirebaseAuth firebase;
 @MockBean CloudPersistence cloud;
 @Autowired MockMvc mvc;
 @BeforeEach void prepare() throws Exception {
  when(cloud.load()).thenAnswer(i->new LinkedHashMap<String,Object>());
  when(cloud.refreshIfChanged()).thenReturn(null);
  FirebaseToken customer=mock(FirebaseToken.class);when(customer.getUid()).thenReturn("firebase-customer");when(customer.getEmail()).thenReturn("customer@example.test");when(customer.getClaims()).thenReturn(Map.of());
  FirebaseToken admin=mock(FirebaseToken.class);when(admin.getUid()).thenReturn("firebase-admin");when(admin.getClaims()).thenReturn(Map.of("admin",true));
  when(firebase.verifyIdToken("valid-customer",true)).thenReturn(customer);when(firebase.verifyIdToken("valid-admin",true)).thenReturn(admin);
  when(firebase.verifyIdToken("invalid",true)).thenThrow(new IllegalArgumentException("invalid token"));
 }
 @Test void verifiedFirebaseIdentityUsesMatchedCommerceContract() throws Exception {
  mvc.perform(get("/api/v1/profile").header("Authorization","Bearer valid-customer")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("firebase-customer")).andExpect(jsonPath("$.role").value("CUSTOMER"));
  mvc.perform(put("/api/v1/cart/items").header("Authorization","Bearer valid-customer").contentType("application/json").content("{\"productId\":\"sample-seeds\",\"quantity\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].product.id").value("sample-seeds"));
  verify(cloud,atLeastOnce()).save(anyMap());
 }
 @Test void claimsEnforceAdminAndInvalidTokensAreRejected() throws Exception {
  mvc.perform(get("/api/v1/admin/reports").header("Authorization","Bearer valid-customer")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/admin/reports").header("Authorization","Bearer valid-admin")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/profile").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
 }
 @Test void localPasswordLoginIsDisabled() throws Exception {
  mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"x@example.test\",\"password\":\"irrelevant\"}")).andExpect(status().isNotFound());
 }
}
