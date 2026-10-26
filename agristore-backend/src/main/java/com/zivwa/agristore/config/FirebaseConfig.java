
package com.zivwa.agristore.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(
        name = "app.firebase.enabled",
        havingValue = "true"
)
public class FirebaseConfig {

    // Initialize Firebase Admin SDK
    @Bean
    FirebaseApp firebaseApp(
            @Value("${app.firebase.project-id}") String projectId
    ) throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) {

            FirebaseApp existing = FirebaseApp.getInstance();

            if (!projectId.equals(
                    existing.getOptions().getProjectId()
            )) {
                throw new IllegalStateException(
                        "Firebase project mismatch. Expected: "
                                + projectId
                                + ", Actual: "
                                + existing.getOptions().getProjectId()
                );
            }

            return existing;
        }

        GoogleCredentials credentials =
                GoogleCredentials.getApplicationDefault();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        System.out.println(
                "Initializing Firebase project: " + projectId
        );

        return FirebaseApp.initializeApp(options);
    }

    // Firebase Authentication
    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    // Cloud Firestore
    @Bean(destroyMethod = "close")
    Firestore firestore(
            FirebaseApp app,
            @Value("${app.firebase.database-id:default}")
            String databaseId
    ) throws IOException {

        String projectId =
                app.getOptions().getProjectId();

        GoogleCredentials credentials =
                GoogleCredentials.getApplicationDefault();

        System.out.println(
                "=== AGRISTORE FIRESTORE CONFIG ==="
        );
        System.out.println("Project ID: " + projectId);
        System.out.println("Database ID: " + databaseId);

        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId(databaseId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
