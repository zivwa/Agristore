package com.zivwa.agristore.api;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.google.firebase.auth.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.zivwa.agristore.api.Models.*;
@ConditionalOnProperty(name="app.auth-mode", havingValue="firebase")
@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
 private final CatalogService catalog;private final ObjectProvider<FirebaseAuth> firebase;
 public AdminController(CatalogService catalog,ObjectProvider<FirebaseAuth> firebase){this.catalog=catalog;this.firebase=firebase;}
 @GetMapping("/products") public List<Product> products(){return catalog.products(true);}
 @PostMapping("/products") public Product create(@RequestBody Product p){return catalog.saveProduct(null,p);}
 @PutMapping("/products/{id}") public Product update(@PathVariable String id,@RequestBody Product p){catalog.detail(id,true);return catalog.saveProduct(id,p);}
 @DeleteMapping("/products/{id}") public ResponseEntity<Void> delete(@PathVariable String id){catalog.deactivate(id);return ResponseEntity.noContent().build();}
 @GetMapping("/categories") public List<Category> categories(){return catalog.categories(true);}
 @PostMapping("/categories") public Category createCategory(@RequestBody Category c){return catalog.saveCategory(null,c);}
 @PutMapping("/categories/{id}") public Category updateCategory(@PathVariable String id,@RequestBody Category c){return catalog.saveCategory(id,c);}
 @DeleteMapping("/categories/{id}") public ResponseEntity<Void> deleteCategory(@PathVariable String id){catalog.deleteCategory(id);return ResponseEntity.noContent().build();}
 public record Stock(String productId,int availableStock){}
 public record StockUpdate(int availableStock){}
 @GetMapping("/inventory") public List<Stock> inventory(){return catalog.products(true).stream().map(p->new Stock(p.id(),p.availableStock())).toList();}
 @PutMapping("/inventory/{id}") public Stock setStock(@PathVariable String id,@RequestBody StockUpdate change){Product p=catalog.updateStock(id,change.availableStock());return new Stock(id,p.availableStock());}
 @GetMapping("/orders") public List<Object> orders(){return List.of();}
 @PatchMapping("/orders/{id}/status") public ResponseEntity<?> orderStatus(@PathVariable String id,@RequestBody Map<String,Object> change){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Order persistence and fulfilment not integrated");}
 @GetMapping("/customers") public Map<String,Object> customers(@RequestParam(required=false) String pageToken,@RequestParam(defaultValue="20") int pageSize){
  FirebaseAuth auth=firebase.getIfAvailable();if(auth==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firebase Auth unavailable");
  try{ListUsersPage page=auth.listUsers(pageToken, Math.min(100, Math.max(1, pageSize)));List<Map<String,Object>> users=new ArrayList<>();for(ExportedUserRecord user:page.getValues())users.add(Map.of("uid",user.getUid(),"name",user.getDisplayName()==null?"":user.getDisplayName(),"email",user.getEmail()==null?"":user.getEmail(),"disabled",user.isDisabled(),"createdAt",user.getUserMetadata().getCreationTimestamp()));return Map.of("items",users,"nextPageToken",page.getNextPageToken()==null?"":page.getNextPageToken());}
  catch(FirebaseAuthException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firebase user lookup failed");}
 }
 @PatchMapping("/customers/{uid}") public ResponseEntity<?> customerStatus(@PathVariable String uid,@RequestBody Map<String,Object> body){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Customer enable/disable requires additional admin safeguards");}
 @GetMapping("/settings") public Map<String,Object> settings(){return Map.of("catalogSource",catalog.firestoreMode()?"firestore":"demo","commerceStore","firestore","currency","INR","customerManagement",false,"note","Razorpay, Shiprocket and orders are not configured");}
}
