package com.zivwa.agristore.api;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static com.zivwa.agristore.api.Models.*;
@ConditionalOnProperty(name="app.auth-mode", havingValue="firebase")
@RestController @RequestMapping("/api/v1")
public class CustomerController {
 private final ObjectProvider<Firestore> database;private final CatalogService catalog;
 public CustomerController(ObjectProvider<Firestore> database,CatalogService catalog){this.database=database;this.catalog=catalog;}
 private Firestore db(){Firestore x=database.getIfAvailable();if(x==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firestore unavailable; enable Firebase");return x;}
 // Development diagnostic: the underlying Firestore cause is logged only on the server.
 private <T> T await(ApiFuture<T> future) {
  try {
   return future.get(15, TimeUnit.SECONDS);
  } catch (InterruptedException e) {
   Thread.currentThread().interrupt();
   System.err.println("=== AGRISTORE FIRESTORE ERROR: INTERRUPTED ===");
   e.printStackTrace(System.err);
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firestore operation interrupted", e);
  } catch (TimeoutException e) {
   System.err.println("=== AGRISTORE FIRESTORE ERROR: TIMEOUT ===");
   e.printStackTrace(System.err);
   throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Firestore operation timed out", e);
  } catch (ExecutionException e) {
   Throwable cause = e.getCause() != null ? e.getCause() : e;
   System.err.println("=== AGRISTORE FIRESTORE ERROR: EXECUTION FAILED ===");
   cause.printStackTrace(System.err);
   if (cause instanceof ResponseStatusException statusException) {
    throw statusException;
   }
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firestore operation failed", cause);
  }
 }
 @GetMapping("/auth/me") public Map<String,Object> me(Authentication a){return Map.of("authenticated",true,"uid",a.getName(),"role",a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN"))?"ADMIN":"CUSTOMER");}
 private DocumentReference cartRef(String uid){return db().collection("carts").document(uid);}
 @SuppressWarnings("unchecked") private Map<String,Integer> quantities(DocumentSnapshot doc){Map<String,Integer> result=new HashMap<>();if(doc.exists()&&doc.get("items") instanceof Map<?,?> raw){for(var e:raw.entrySet())if(e.getKey() instanceof String k&&e.getValue() instanceof Number n)result.put(k,n.intValue());}return result;}
 private Cart enriched(Map<String,Integer> rows){List<CartItem> items=new ArrayList<>();long total=0;for(var e:rows.entrySet()){
  Product p;try{p=catalog.detail(e.getKey(),true);}catch(ResponseStatusException ex){p=new Product(e.getKey(),"", "Unavailable product","","",0,"INR",0,false,List.of(),List.of(),List.of(),List.of());}
  long line=p.pricePaise()*e.getValue();total+=line;items.add(new CartItem(p.id(),e.getValue(),p.name(),p.pricePaise(),line,p.availableStock(),p.active(),p.imageUrls().isEmpty()?null:p.imageUrls().get(0)));
 }items.sort(Comparator.comparing(CartItem::name));return new Cart(items,total,"INR");}
 @GetMapping("/cart") public Cart cart(Authentication a){return enriched(quantities(await(cartRef(a.getName()).get())));}
 public record ItemInput(String productId,Integer quantity){}
 public record QuantityInput(Integer quantity){}
 private Cart updateCart(String uid,String id,Integer n,boolean add){
  if(id==null||id.isBlank())throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"productId required");
  if(n==null||(n<1&&!(!add&&n==0))||n>99)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"quantity must be 1 to 99");
  DocumentReference ref=cartRef(uid);
  Map<String,Integer> result=await(db().runTransaction(tx->{Map<String,Integer> q=quantities(tx.get(ref).get());
   if(n==0){q.remove(id);}else{Product p=catalog.detail(id,false);int next=add?q.getOrDefault(id,0)+n:n;
    if(next<1||next>99||next>p.availableStock())throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock or invalid quantity");
    if(q.size()>=50&&!q.containsKey(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Cart limit reached");q.put(id,next);}
   tx.set(ref,Map.of("items",q,"updatedAt",FieldValue.serverTimestamp()));return q;
  }));return enriched(result);
 }
 @PostMapping("/cart/items") public Cart add(@RequestBody ItemInput input,Authentication a){return updateCart(a.getName(),input.productId(),input.quantity(),true);}
 @PatchMapping("/cart/items/{id}") public Cart quantity(@PathVariable String id,@RequestBody QuantityInput input,Authentication a){return updateCart(a.getName(),id,input.quantity(),false);}
 @DeleteMapping("/cart/items/{id}") public Cart remove(@PathVariable String id,Authentication a){return updateCart(a.getName(),id,0,false);}
 private CollectionReference addresses(String uid){return db().collection("users").document(uid).collection("addresses");}
 private Address convert(DocumentSnapshot d){Map<String,Object> m=d.getData();if(m==null)m=Map.of();return new Address(d.getId(),String.valueOf(m.getOrDefault("recipientName","")),String.valueOf(m.getOrDefault("phone","")),String.valueOf(m.getOrDefault("line1","")),String.valueOf(m.getOrDefault("line2","")),String.valueOf(m.getOrDefault("city","")),String.valueOf(m.getOrDefault("stateCode","")),String.valueOf(m.getOrDefault("pincode","")),String.valueOf(m.getOrDefault("countryCode","IN")));}
 private void validate(AddressInput a){if(a==null||a.recipientName()==null||a.recipientName().isBlank()||a.phone()==null||!a.phone().matches("[6-9][0-9]{9}")||a.line1()==null||a.line1().isBlank()||a.city()==null||a.city().isBlank()||a.stateCode()==null||a.stateCode().isBlank()||a.pincode()==null||!a.pincode().matches("[1-9][0-9]{5}")||!"IN".equals(a.countryCode()))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid address fields");}
 private Map<String,Object> fields(AddressInput a){Map<String,Object> m=new HashMap<>();m.put("recipientName",a.recipientName());m.put("phone",a.phone());m.put("line1",a.line1());m.put("line2",a.line2()==null?"":a.line2());m.put("city",a.city());m.put("stateCode",a.stateCode());m.put("pincode",a.pincode());m.put("countryCode","IN");return m;}
 @GetMapping("/addresses") public List<Address> addresses(Authentication a){List<Address> out=new ArrayList<>();for(DocumentSnapshot d:await(addresses(a.getName()).get()).getDocuments())out.add(convert(d));return out;}
 @PostMapping("/addresses") public Address create(@RequestBody AddressInput input,Authentication a){validate(input);DocumentReference r=addresses(a.getName()).document();await(r.set(fields(input)));return convert(await(r.get()));}
 @PatchMapping("/addresses/{id}") public Address updateAddress(@PathVariable String id,@RequestBody AddressInput input,Authentication a){validate(input);DocumentReference r=addresses(a.getName()).document(id);if(!await(r.get()).exists())throw new ResponseStatusException(HttpStatus.NOT_FOUND);await(r.set(fields(input)));return convert(await(r.get()));}
 @DeleteMapping("/addresses/{id}") public ResponseEntity<Void> deleteAddress(@PathVariable String id,Authentication a){DocumentReference r=addresses(a.getName()).document(id);if(!await(r.get()).exists())throw new ResponseStatusException(HttpStatus.NOT_FOUND);await(r.delete());return ResponseEntity.noContent().build();}
}
