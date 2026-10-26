package com.zivwa.agristore.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudPersistenceTest {
 @SuppressWarnings({"unchecked","rawtypes"})
 @Test void changedRecordsAndVersionAreWrittenTogether() throws Exception {
  Firestore db=mock(Firestore.class);CollectionReference stores=mock(CollectionReference.class);DocumentReference root=mock(DocumentReference.class);DocumentSnapshot meta=mock(DocumentSnapshot.class);when(meta.exists()).thenReturn(false);
  when(db.collection("agristoreStores")).thenReturn(stores);when(stores.document("test-store")).thenReturn(root);when(root.get()).thenReturn(ApiFutures.immediateFuture(meta));
  Map<String,CollectionReference> groups=new HashMap<>();Map<String,DocumentReference> documents=new HashMap<>();
  for(String group:List.of("users","addresses","carts","orders","contacts","settings","checkoutKeys","products","categories")){
   var col=mock(CollectionReference.class);var query=mock(QuerySnapshot.class);when(query.getDocuments()).thenReturn(List.of());when(col.get()).thenReturn(ApiFutures.immediateFuture(query));when(root.collection(group)).thenReturn(col);groups.put(group,col);
   when(col.document(anyString())).thenAnswer(i->documents.computeIfAbsent(group+":"+i.getArgument(0),k->mock(DocumentReference.class)));
  }
  Transaction tx=mock(Transaction.class);when(tx.get(root)).thenReturn(ApiFutures.immediateFuture(meta));
  when(db.runTransaction(any(Transaction.Function.class))).thenAnswer(i->{Transaction.Function<Long> fn=i.getArgument(0);return ApiFutures.immediateFuture(fn.updateCallback(tx));});
  var repo=new CloudPersistence(db,new ObjectMapper(),"test-store");var state=repo.load();
  ((Map<String,Object>)state.get("orders")).put("order-one",Map.of("id","order-one","totalPaise",19900));repo.save(state);
  verify(tx).set(eq(root),argThat(map->Long.valueOf(1).equals(map.get("revision"))));
  assertEquals(1,documents.size());verify(tx,times(2)).set(any(DocumentReference.class),anyMap());
 }
 @Test void integerAndFirestoreLongValuesDoNotCauseUnnecessaryWrites(){assertTrue(CloudPersistence.equivalent(Map.of("stock",12),Map.of("stock",12L)));assertFalse(CloudPersistence.equivalent(Map.of("stock",12),Map.of("stock",11L)));}
}
