package com.zivwa.agristore;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.TimeUnit;

/** Connectivity checks only: no server and no cloud writes. */
public final class FirebaseCheck {
 public static void run() throws Exception {
  String project=System.getenv().getOrDefault("FIREBASE_PROJECT_ID","agristore-dev");
  String database=System.getenv().getOrDefault("FIRESTORE_DATABASE_ID","default");
  GoogleCredentials credentials=GoogleCredentials.getApplicationDefault();
  FirebaseApp app=FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).setProjectId(project).build());
  try(Firestore db=FirestoreOptions.newBuilder().setProjectId(project).setDatabaseId(database).setCredentials(credentials).build().getService()){
   db.collection("agristoreStores").document(System.getenv().getOrDefault("FIRESTORE_STORE","storefront-v2")).get().get(20,TimeUnit.SECONDS);
   System.out.println("CHECK: Firestore read access OK (project="+project+", database="+database+")");
   FirebaseAuth.getInstance(app).listUsers(null,1);
   System.out.println("CHECK: Firebase Authentication Admin access OK");
   System.out.println("CHECK: No server started; no Firebase data written.");
  }finally{app.delete();}
 }
}
