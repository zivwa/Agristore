package com.zivwa.agristore.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Atomic, version-checked persistence of the matched storefront's records. */
@Service
@ConditionalOnProperty(name="app.auth-mode",havingValue="firebase-commerce")
public class CloudPersistence {
 private final Firestore db;
 private final ObjectMapper mapper;
 private final DocumentReference root;
 private long revision;
 private Map<String,Map<String,Object>> baseline=new LinkedHashMap<>();
 private static final List<String> MAPS=List.of("users","addresses","carts","orders","contacts","settings","checkoutKeys");
 public CloudPersistence(Firestore db,ObjectMapper mapper,@Value("${app.firestore-store:storefront-v2}") String storeId){
  if(!storeId.matches("[a-zA-Z0-9_-]{1,80}"))throw new IllegalArgumentException("Invalid Firestore store ID");
  this.db=db;this.mapper=mapper;root=db.collection("agristoreStores").document(storeId);
 }
 private <T>T await(com.google.api.core.ApiFuture<T> future){try{return future.get(20,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable(e);}catch(Exception e){throw unavailable(e);}}
 private ResponseStatusException unavailable(Exception e){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firestore unavailable. Check credentials, database ID and IAM permissions.",e);}
 private long version(DocumentSnapshot doc){Long n=doc.exists()?doc.getLong("revision"):null;return n==null?0:n;}
 private String key(String key){return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));}
 static boolean equivalent(Object a,Object b){
  if(a instanceof Number x&&b instanceof Number y)return new java.math.BigDecimal(x.toString()).compareTo(new java.math.BigDecimal(y.toString()))==0;
  if(a instanceof Map<?,?> x&&b instanceof Map<?,?> y)return x.keySet().equals(y.keySet())&&x.entrySet().stream().allMatch(e->equivalent(e.getValue(),y.get(e.getKey())));
  if(a instanceof List<?> x&&b instanceof List<?> y){if(x.size()!=y.size())return false;for(int i=0;i<x.size();i++)if(!equivalent(x.get(i),y.get(i)))return false;return true;}
  return Objects.equals(a,b);
 } public synchronized Map<String,Object> refreshIfChanged(){return version(await(root.get()))==revision?null:load();}
 public synchronized Map<String,Object> load(){
  for(int attempt=0;attempt<3;attempt++){
   long before=version(await(root.get()));Map<String,Map<String,Object>> rows=new LinkedHashMap<>();
   for(String group:List.of("users","addresses","carts","orders","contacts","settings","checkoutKeys","products","categories")){
    Map<String,Object> records=new LinkedHashMap<>();
    for(QueryDocumentSnapshot doc:await(root.collection(group).get()).getDocuments()){
     String id=doc.getString("key");if(id!=null)records.put(id,doc.get("value"));
    }rows.put(group,records);
   }
   if(before!=version(await(root.get())))continue;
   baseline=rows;revision=before;
   Map<String,Object> state=new LinkedHashMap<>();for(String group:MAPS)state.put(group,new LinkedHashMap<>(rows.get(group)));
   if(!rows.get("products").isEmpty())state.put("products",new ArrayList<>(rows.get("products").values()));
   if(!rows.get("categories").isEmpty())state.put("categories",new ArrayList<>(rows.get("categories").values()));
   return state;
  }throw new ResponseStatusException(HttpStatus.CONFLICT,"Store changed while loading; retry.");
 }
 @SuppressWarnings("unchecked")
 public synchronized void save(Map<String,Object> state){
  Map<String,Object> plain=mapper.convertValue(state,new TypeReference<Map<String,Object>>(){});
  Map<String,Map<String,Object>> next=new LinkedHashMap<>();
  for(String group:MAPS)next.put(group,new LinkedHashMap<>((Map<String,Object>)plain.getOrDefault(group,Map.of())));
  for(String group:List.of("products","categories")){Map<String,Object> rows=new LinkedHashMap<>();for(Object value:(List<?>)plain.getOrDefault(group,List.of())){Map<String,Object> row=(Map<String,Object>)value;rows.put(String.valueOf(row.get("id")),row);}next.put(group,rows);}
  Map<DocumentReference,Map<String,Object>> writes=new LinkedHashMap<>();List<DocumentReference> deletes=new ArrayList<>();
  for(var group:next.entrySet()){
   Map<String,Object> prior=baseline.getOrDefault(group.getKey(),Map.of());
   for(var row:group.getValue().entrySet())if(!equivalent(prior.get(row.getKey()),row.getValue()))writes.put(root.collection(group.getKey()).document(key(row.getKey())),Map.of("key",row.getKey(),"value",row.getValue()));
   for(String id:prior.keySet())if(!group.getValue().containsKey(id))deletes.add(root.collection(group.getKey()).document(key(id)));
  }
  if(writes.isEmpty()&&deletes.isEmpty())return;
  if(writes.size()+deletes.size()>450)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Too many records in one change; split the operation.");
  final long expected=revision;
  try{
   await(db.runTransaction(tx->{long actual=version(tx.get(root).get());if(actual!=expected)throw new ResponseStatusException(HttpStatus.CONFLICT,"Store changed in another session. Refresh and retry.");for(var w:writes.entrySet())tx.set(w.getKey(),w.getValue());for(var ref:deletes)tx.delete(ref);tx.set(root,Map.of("revision",expected+1,"updatedAt",FieldValue.serverTimestamp()));return expected+1;}));
  }catch(ResponseStatusException e){Throwable cause=e;while(cause!=null){if(cause instanceof ResponseStatusException status&&status.getStatusCode().value()==409)throw status;cause=cause.getCause();}throw e;}
  revision=expected+1;baseline=next;
 }
}
