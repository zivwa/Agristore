package com.zivwa.agristore;

import com.google.cloud.firestore.Firestore;
import com.zivwa.agristore.api.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StorefrontContractTest {
 private CatalogService catalog() {
  return new CatalogService(new DefaultListableBeanFactory().getBeanProvider(Firestore.class), "demo");
 }
 @Test void homepageCategoriesResolveToProducts() {
  var c = catalog();
  for (String category : List.of("seeds","insecticides","fungicides","herbicides","growth-promoters","nutrients","npk")) {
   assertTrue(c.categories(false).stream().anyMatch(item -> item.id().equals(category)));
   assertFalse(c.page(null, category, null, 12).items().isEmpty(), category);
  }
 }
 @Test void cropAndPestSearchesResolve() {
  var c = catalog();
  for (String query : List.of("cotton", "chilli", "rice", "groundnut", "sugarcane", "wheat", "maize", "soybean", "tomato", "whitefly", "bollworms", "aphids", "thrips")) {
   assertFalse(c.page(query, null, null, 12).items().isEmpty(), query);
  }
 }
 @Test void paginationHasNoDuplicatesAndRejectsInvalidCursor() {
  var c = catalog();
  var first = c.page(null, null, null, 3);
  var second = c.page(null, null, first.nextCursor(), 3);
  assertEquals(3, first.items().size());
  assertTrue(second.items().stream().noneMatch(first.items()::contains));
  assertThrows(ResponseStatusException.class, () -> c.page(null, null, "invalid", 12));
 }
}
