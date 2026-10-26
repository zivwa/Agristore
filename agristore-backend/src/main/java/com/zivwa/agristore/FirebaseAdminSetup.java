package com.zivwa.agristore;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import java.util.*;
/** Explicit operator command only; never run automatically at startup. */
public final class FirebaseAdminSetup {
 public static void grant(String uid) throws Exception {
  if(uid==null||uid.isBlank())throw new IllegalArgumentException("Provide a Firebase Authentication user UID");
  String project=System.getenv().getOrDefault("FIREBASE_PROJECT_ID","agristore-dev");
  FirebaseApp app=FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).setProjectId(project).build());
  try{FirebaseAuth auth=FirebaseAuth.getInstance(app);var user=auth.getUser(uid);Map<String,Object> claims=new LinkedHashMap<>(user.getCustomClaims());claims.put("admin",true);auth.setCustomUserClaims(uid,claims);System.out.println("Administrator claim applied. Sign out and sign in again.");}finally{app.delete();}
 }
}
