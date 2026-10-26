package com.zivwa.agristore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class AgriStoreApplication {
 public static void main(String[] args) throws Exception {
  if(java.util.Arrays.asList(args).contains("--check-firebase")){FirebaseCheck.run();return;}
  if(args.length>0&&args[0].equals("--grant-admin")){if(args.length!=2)throw new IllegalArgumentException("Usage: --grant-admin USER_UID");FirebaseAdminSetup.grant(args[1]);return;}
  SpringApplication.run(AgriStoreApplication.class,args);
 }
}
