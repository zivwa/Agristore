package com.zivwa.agristore;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.nio.file.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.auth-mode=local","app.firebase.enabled=false","app.admin-email=owner@example.test","app.admin-password=Test-Admin-Password-2026"})
@AutoConfigureMockMvc
class LocalCommerceIntegrationTest {
 static Path directory;
 static {try{directory=Files.createTempDirectory("agristore-test-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("app.local-data",()->directory.resolve("store.json").toString());}
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 JsonNode call(MockHttpServletRequestBuilder request,int status) throws Exception {return json.readTree(mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse().getContentAsString());}
 String register(String email) throws Exception {return call(post("/api/v1/auth/register").contentType("application/json").content("{\"name\":\"Test Farmer\",\"email\":\""+email+"\",\"password\":\"Strong-Password-2026\"}"),200).get("token").asText();}
 @Test void checkoutPersistsAndIsIdempotentAndOwned() throws Exception {
  String token=register("farmer@example.test"),other=register("other@example.test");
  String auth="Bearer "+token;
  call(put("/api/v1/cart/items").header("Authorization",auth).contentType("application/json").content("{\"productId\":\"sample-seeds\",\"quantity\":2}"),200);
  var cart=call(get("/api/v1/cart").header("Authorization",auth),200);assertEquals("sample-seeds",cart.at("/items/0/product/id").asText());assertEquals(39800,cart.get("totalPaise").asLong());
  var addr=call(post("/api/v1/addresses").header("Authorization",auth).contentType("application/json").content("{\"name\":\"Farmer\",\"phone\":\"9876543210\",\"line1\":\"Farm Road\",\"city\":\"Pune\",\"state\":\"MH\",\"postalCode\":\"411001\"}"),200);
  String body="{\"addressId\":\""+addr.get("id").asText()+"\",\"paymentMethod\":\"COD\"}";
  var order=call(post("/api/v1/orders").header("Authorization",auth).header("Idempotency-Key","checkout-one").contentType("application/json").content(body),200);
  var again=call(post("/api/v1/orders").header("Authorization",auth).header("Idempotency-Key","checkout-one").contentType("application/json").content(body),200);assertEquals(order.get("id"),again.get("id"));
  mvc.perform(get("/api/v1/orders/"+order.get("id").asText()).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/orders/"+order.get("id").asText()+"/test-payment").header("Authorization",auth).contentType("application/json").content("{\"success\":true}")).andExpect(status().isNotFound());
  assertEquals(23,call(get("/api/v1/products/sample-seeds"),200).get("availableStock").asInt());
  assertTrue(Files.readString(directory.resolve("store.json")).contains(order.get("id").asText()));
  mvc.perform(post("/api/v1/auth/logout").header("Authorization",auth)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/profile").header("Authorization",auth)).andExpect(status().isUnauthorized());
 }
 @Test void adminAndRegistrationRolesAreEnforced() throws Exception {
  String token=register("customer@example.test");mvc.perform(get("/api/v1/admin/reports").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  String admin=call(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"owner@example.test\",\"password\":\"Test-Admin-Password-2026\"}"),200).get("token").asText();
  call(get("/api/v1/admin/reports").header("Authorization","Bearer "+admin),200);
  call(get("/api/v1/admin/customers").header("Authorization","Bearer "+admin),200);
  var persisted=json.readTree(Files.readString(directory.resolve("store.json")));assertFalse(persisted.toString().contains("Strong-Password-2026"));
 }
}
