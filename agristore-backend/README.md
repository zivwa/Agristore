# AgriStore — Firebase-enabled combined package

Project: agristore-dev
Firestore database ID: default (named database, no parentheses)
Backend: http://127.0.0.1:8080
Frontend: http://localhost:5173

## Start

1. Extract this ZIP fully.
2. Keep the existing service-account JSON file on your computer. The private key is NOT included in this ZIP. agristore-backend/firebase.local.cmd references its existing external path. If moving to another computer, edit only that path or use your own secure credential environment.
3. Run SETUP-FRONTEND.cmd once.
4. Free port 8080 yourself, then run START-BACKEND.cmd.
5. In another terminal run START-FRONTEND.cmd.
6. Sign in or register through the website using Firebase Authentication.

Requirements: Java 21 and Node.js 20.19+/22+ with npm. Maven 3.9+ is needed only to rebuild the backend. A built backend JAR and frontend dist are included. No backend server was started during this configuration work.

## Configuration

Frontend .env uses VITE_AUTH_MODE=firebase, the existing agristore-dev Web app configuration, VITE_API_BASE_URL=/api/v1, and VITE_CATALOG_MODE=api. Vite proxies /api to 8080.

Backend firebase.local.cmd sets:
- AUTH_MODE=firebase-commerce
- FIREBASE_ENABLED=true
- FIREBASE_PROJECT_ID=agristore-dev
- FIRESTORE_DATABASE_ID=default
- FIRESTORE_STORE=storefront-v2
- CATALOG_SOURCE=demo
- GOOGLE_APPLICATION_CREDENTIALS=external JSON path

CATALOG_SOURCE=demo selects the in-memory catalogue adapter used by the matched commerce layer; CloudPersistence persists the entire matched storefront catalogue and commerce state to Firestore. It does NOT mean orders/users are stored in a local file in firebase-commerce mode. Do not switch to the older AUTH_MODE=firebase route set; that route set has legacy API contracts.

Data is stored under:

    agristoreStores/storefront-v2/{users,addresses,carts,orders,contacts,settings,checkoutKeys,products,categories}/{encoded-record-id}

The root store document maintains a transaction revision. Writes atomically update changed records and that revision. Concurrent conflicting writes return a retryable conflict instead of overwriting another session. Reads refresh when the cloud revision changes. This is a bounded development storefront implementation; it reads the namespaced dataset on refresh and is not designed for a very large production catalogue. Existing unrelated Firestore collections are not migrated or overwritten. The first authenticated profile save seeds the namespaced catalogue with the sample records supplied in the package.

Existing local passwords/accounts/orders from the earlier local-only ZIP are not automatically uploaded or migrated. Use a Firebase account. New cloud profiles are linked to the verified Firebase UID, with no local password hashes. Prices and stock in the seed catalogue remain fictional demonstration data. Orders are recorded, not fulfilled. Real payments and delivery booking remain unintegrated; test payments remain disabled by default.

## Authentication and administration

Frontend Firebase sign-in sends Firebase ID tokens to the backend. The backend verifies tokens and revocation status. Profile/cart/address/order endpoints retain the current frontend contracts. Administrator permission comes only from the verified admin=true Firebase custom claim, not a client request or editable profile field.

To intentionally grant an existing Firebase user administrator access, copy their UID from Firebase Authentication and explicitly run:

    .\SET-FIREBASE-ADMIN.cmd THE_FIREBASE_USER_UID

This changes that account's custom claim; it is never run automatically. Sign out and in again afterward. No admin claim was granted while preparing this package. Customer disabling in the store blocks commerce access without deleting the underlying Firebase identity.

## Verification

Verified against the live project using read-only Google REST APIs:
- Credential file belongs to agristore-dev
- Firestore database default exists and allows reads
- Firebase Authentication Admin API allows reads
- Email/password sign-in is enabled
- localhost is authorized

No Firebase documents or users were created, modified or deleted by these checks. A real user sign-in and a live Firestore write were not performed. Automated tests use mocks for Firebase verification and cloud transactions, plus local commerce integration tests. Frontend production build passes.

CHECK-FIREBASE.cmd runs a read-only Java SDK diagnostic. On this computer that diagnostic encountered a Java/Windows local socket error (Unable to establish loopback connection / UnixDomainSockets Invalid argument); therefore live Java SDK connectivity has NOT been verified. The equivalent read-only REST checks succeeded. CHECK-FIREBASE-REST.ps1 provides that fallback and requires PowerShell 7. If Java startup reports the same socket error in your own terminal, the local Java/network runtime must be repaired before the backend can run; do not confuse it with invalid Firebase credentials.

## Build

Frontend: npm ci, then npm run build in agristore-frontend.
Backend: mvn -B test package in agristore-backend. Copy target/agristore-backend-0.1.0.jar over agristore-backend.jar after rebuilding.

Firebase Admin credentials setup: https://firebase.google.com/docs/admin/setup
ID token verification: https://firebase.google.com/docs/auth/admin/verify-id-tokens

The service-account JSON, dependency folders, Maven target output and runtime local data are excluded from this delivery ZIP. Web app configuration is included so the frontend is configured for your project; it does not contain the backend private key.
