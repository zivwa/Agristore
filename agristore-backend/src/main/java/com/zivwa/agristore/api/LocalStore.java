package com.zivwa.agristore.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.ObjectProvider;

@Service
@ConditionalOnExpression("'${app.auth-mode:local}' == 'local' or '${app.auth-mode:local}' == 'firebase-commerce'")
public class LocalStore {
 public final CatalogService catalog;
 public final ObjectMapper mapper;
 public final Map<String,Object> data;
 private final Path file;
 private final CloudPersistence cloud;
 public final boolean firebaseMode;
 private final List<Models.Product> seedProducts;
 private final List<Models.Category> seedCategories;
 private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
 private final Map<String,Session> sessions = new ConcurrentHashMap<>();
 private record Session(String uid, Instant expires) {}
 public LocalStore(CatalogService catalog,ObjectMapper mapper,@Value("${app.local-data:./data/store.json}") String path,@Value("${app.admin-email:}") String adminEmail,@Value("${app.admin-password:}") String adminPassword,ObjectProvider<CloudPersistence> cloudProvider,@Value("${app.auth-mode:local}") String authMode) throws Exception {
  this.catalog=catalog;this.mapper=mapper;file=Path.of(path).toAbsolutePath();
  firebaseMode=authMode.equals("firebase-commerce");cloud=cloudProvider.getIfAvailable();
  if(firebaseMode&&cloud==null)throw new IllegalStateException("Firebase commerce requires FIREBASE_ENABLED=true and valid credentials");
  if(firebaseMode&&catalog.firestoreMode())throw new IllegalStateException("Set CATALOG_SOURCE=demo; matched commerce data is persisted through CloudPersistence");
  seedProducts=catalog.products(true);seedCategories=catalog.categories(true);
  data=cloud!=null?Objects.requireNonNullElseGet(cloud.load(),LinkedHashMap::new):(Files.exists(file)?mapper.readValue(file.toFile(),new TypeReference<Map<String,Object>>(){}):new LinkedHashMap<>());
  for(String key:List.of("users","addresses","carts","orders","contacts","settings","checkoutKeys")) data.computeIfAbsent(key,k->new LinkedHashMap<String,Object>());
  if(data.get("categories") instanceof List<?> categories) for(Object c:categories) catalog.saveCategory(null,mapper.convertValue(c,Models.Category.class));
  if(data.get("products") instanceof List<?> products) for(Object p:products) {var item=mapper.convertValue(p,Models.Product.class);catalog.saveProduct(item.id(),item);catalog.updateStock(item.id(),item.availableStock());}
  map("settings").putIfAbsent("storeName","AgriStore");map("settings").putIfAbsent("contactEmail","");map("settings").putIfAbsent("supportPhone","");
  if(!firebaseMode&&!adminEmail.isBlank() && !adminPassword.isBlank() && users().stream().noneMatch(u->adminEmail.equalsIgnoreCase(text(u,"email")))) {
   if(adminPassword.length()<12) throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters");
   createUser(adminEmail,"Store administrator",adminPassword,"ADMIN");save();
  }
 }
 @SuppressWarnings("unchecked") public Map<String,Object> map(String key){return (Map<String,Object>)data.get(key);}
 @SuppressWarnings("unchecked") public Map<String,Object> obj(Object v){return v instanceof Map<?,?>?(Map<String,Object>)v:new LinkedHashMap<>();}
 public String text(Map<String,Object> m,String key){return Objects.toString(m.get(key),"").trim();}
 public void require(boolean condition,String message){if(!condition)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,message);}
 public List<Map<String,Object>> users(){return map("users").values().stream().map(this::obj).toList();}
 public Map<String,Object> profile(String uid){var user=obj(map("users").get(uid));if(user.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);var copy=new LinkedHashMap<>(user);copy.remove("passwordHash");return copy;}
 public Map<String,Object> createUser(String email,String name,String password,String role){var u=new LinkedHashMap<String,Object>();String uid=UUID.randomUUID().toString();u.put("id",uid);u.put("email",email.toLowerCase(Locale.ROOT));u.put("name",name);u.put("phone","");u.put("role",role);u.put("enabled",true);u.put("passwordHash",passwords.encode(password));map("users").put(uid,u);return u;}
 public synchronized Map<String,Object> register(Map<String,Object> input){if(firebaseMode)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Use Firebase registration");String email=text(input,"email"),password=text(input,"password"),name=text(input,"name");require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")&&email.length()<255,"Valid email required");require(password.length()>=8&&password.length()<=72,"Password must be 8 to 72 characters");require(!name.isBlank()&&name.length()<120,"Name required");if(users().stream().anyMatch(u->email.equalsIgnoreCase(text(u,"email"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered");var user=createUser(email,name,password,"CUSTOMER");save();return session(user);}
 public synchronized Map<String,Object> login(Map<String,Object> input){if(firebaseMode)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Use Firebase sign-in");String email=text(input,"email"),password=text(input,"password");var user=users().stream().filter(u->email.equalsIgnoreCase(text(u,"email"))).findFirst().orElse(null);if(user==null||!Boolean.TRUE.equals(user.get("enabled"))||!passwords.matches(password,text(user,"passwordHash")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");return session(user);}
 private Map<String,Object> session(Map<String,Object> user){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);sessions.put(token,new Session(text(user,"id"),Instant.now().plusSeconds(28800)));return Map.of("token",token,"user",profile(text(user,"id")));}
 public String verify(String token){var s=sessions.get(token);if(s==null||s.expires().isBefore(Instant.now()))return null;var u=obj(map("users").get(s.uid()));return Boolean.TRUE.equals(u.get("enabled"))?s.uid():null;}
 public void logout(String token){sessions.remove(token);}
 public synchronized void save(){try{data.put("products",catalog.products(true));data.put("categories",catalog.categories(true));if(cloud!=null){try{cloud.save(data);}catch(ResponseStatusException e){replaceCloudState(cloud.load());throw e;}return;}Files.createDirectories(file.getParent());Path temp=file.resolveSibling(file.getFileName()+".tmp");mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(),data);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to save store data",e);}}
 private void replaceCloudState(Map<String,Object> state){
  data.clear();data.putAll(state);
  for(String key:List.of("users","addresses","carts","orders","contacts","settings","checkoutKeys"))data.computeIfAbsent(key,k->new LinkedHashMap<String,Object>());
  List<Models.Category> categories=state.get("categories") instanceof List<?> rows?rows.stream().map(row->mapper.convertValue(row,Models.Category.class)).toList():seedCategories;
  List<Models.Product> products=state.get("products") instanceof List<?> rows?rows.stream().map(row->mapper.convertValue(row,Models.Product.class)).toList():seedProducts;
  catalog.restoreSnapshot(categories,products);
  map("settings").putIfAbsent("storeName","AgriStore");map("settings").putIfAbsent("contactEmail","");map("settings").putIfAbsent("supportPhone","");
 }
 public synchronized void refreshCloud(){if(cloud!=null){Map<String,Object> fresh=cloud.refreshIfChanged();if(fresh!=null)replaceCloudState(fresh);}}
 public synchronized String acceptFirebase(FirebaseToken token){
  String uid=token.getUid();var existing=obj(map("users").get(uid));
  if(Boolean.FALSE.equals(existing.get("enabled")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account disabled");
  String role=Boolean.TRUE.equals(token.getClaims().get("admin"))?"ADMIN":"CUSTOMER";
  boolean changed=existing.isEmpty()||!role.equals(existing.get("role"));
  if(existing.isEmpty()){
   existing=new LinkedHashMap<>();existing.put("id",uid);existing.put("email",Objects.toString(token.getEmail(),""));existing.put("name",Objects.toString(token.getName(),""));existing.put("phone","");existing.put("enabled",true);map("users").put(uid,existing);
  }
  existing.put("role",role);if(changed)save();return uid;
 }}
