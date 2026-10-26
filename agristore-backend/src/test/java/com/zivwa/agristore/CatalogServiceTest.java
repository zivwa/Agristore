package com.zivwa.agristore;
import com.zivwa.agristore.api.CatalogService;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import static org.junit.jupiter.api.Assertions.*;
class CatalogServiceTest {
 @Test void demoMatchesReactCatalogueContract(){CatalogService c=new CatalogService(new DefaultListableBeanFactory().getBeanProvider(Firestore.class),"demo");assertTrue(c.categories(false).size() >= 12);assertFalse(c.page(null,"products",null,12).items().isEmpty());assertNull(c.page(null,"products",null,12).nextCursor());}
}
