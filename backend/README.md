# Production backend template

This optional server is the secure layer for Firebase Cloud Messaging. Deploy it on Cloud Run, Render, Railway, or your own server.

1. Create a Firebase project.
2. Enable Cloud Messaging.
3. Configure Application Default Credentials / `GOOGLE_APPLICATION_CREDENTIALS` on the server.
4. Deploy `server.js`.
5. Point the Android app's notification client at your backend endpoint.

Never put Firebase Admin credentials, exchange trading secrets, or private API keys inside the Android APK.
