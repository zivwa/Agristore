package com.zivwa.agristore.api;
import org.springframework.web.bind.annotation.*;
import static com.zivwa.agristore.api.Models.*;
import java.util.*;
@RestController @RequestMapping("/api/v1")
public class CatalogController {
 private final CatalogService catalog;public CatalogController(CatalogService c){catalog=c;}
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP");}
 @GetMapping("/categories") public List<Category> categories(){return catalog.categories(false);}
 @GetMapping("/products") public ProductPage products(@RequestParam(required=false) String query,@RequestParam(required=false) String categoryId,@RequestParam(required=false) String cursor,@RequestParam(defaultValue="12") int limit){return catalog.page(query,categoryId,cursor,limit);}
 @GetMapping("/products/{id}") public Product product(@PathVariable String id){return catalog.detail(id,false);}
}
