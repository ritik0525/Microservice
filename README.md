```markdown
# Split Video Streaming Microservices — JWT, Signed URLs, CDN (Nginx) Example

What's new
- JWT authentication: upload-service issues JWT tokens at /auth/login (demo user admin/password).
- Signed URLs: upload-service creates signed, short-lived URLs to stream HLS/DASH assets.
- Streaming-service validates signed URLs (HMAC-SHA256) and expiry before serving files.
- Local "CDN" reverse proxy (Nginx) caches and forwards requests to the streaming-service.

Run locally (quick):
1. Edit secrets:
   - In upload-service/src/main/resources/application.properties and streaming-service/src/main/resources/application.properties
     replace security.jwt.secret and signedurl.secret with secure secrets OR set env vars in docker-compose.
2. Build and run:
   docker-compose up --build

Endpoints
- Auth:
  POST http://localhost:8081/auth/login
  Body: { "username":"admin", "password":"password" }
  Returns: { "token": "..." }

- Upload (requires Authorization: Bearer <token> header):
  POST http://localhost:8081/api/videos/upload
  form field: file=@/path/to/video.mp4

- Check status:
  GET http://localhost:8081/api/videos/{id}/status

- Request a signed URL (requires JWT):
  POST http://localhost:8081/api/videos/{id}/signed-url?type=hls
  Returns: { "url": "http://localhost:8084/stream/hls/{id}/hls_master.m3u8?expires=...&sig=..." }

- Use that signed URL in your player (the URL goes to the CDN at http://localhost:8084). The streaming-service validates the signature and expiry.

CDN / CloudFront notes
- This example ships a simple Nginx reverse-proxy (nginx:8084) which caches static streaming assets and forwards to streaming-service.
- For production-grade CDN, use a real CDN (CloudFront, Cloudflare, Fastly) in front of the streaming origin. There are two main approaches:
  1) CDN-level signed URLs (CloudFront signed URLs / signed cookies) — CloudFront validates the signature using RSA keypairs. To use CloudFront signed URLs you must:
     - Create a CloudFront distribution with your streaming-service (or S3) as origin.
     - Configure trusted key groups / key pairs.
     - Use the CloudFront SDK or utilities to generate signed URLs/cookies (CloudFront uses RSA signing, not HMAC).
  2) Application-level signed URLs (this example) — CDN caches but streaming-service validates signature. Keep the signing secret only in services that must sign/verify.

Security notes
- Do NOT use the example secrets in production.
- Move secrets into a secrets manager (AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets).
- Use HTTPS for all external endpoints (the demo runs HTTP locally).
- Replace in-memory/static user authentication with a real identity store or an OAuth2 provider.

Next improvements you may want
- Use AWS CloudFront with RSA-signed URLs for production; I'll provide an example CloudFront Terraform / AWS CLI snippet if you want.
- Replace shared volume with S3/MinIO: store originals and generated assets in object storage; use signed presigned URLs for playback.
- Move auth out to a dedicated Auth service (OAuth2 / OpenID Connect) and use short-lived access tokens for service-to-service calls.

Enjoy. If you want, I can:
- Convert signed URLs to use CloudFront RSA-signed URLs and provide Terraform for distribution.
- Replace the shared volume with S3 + CDN + presigned URL flow.
- Add user management and integrate an OAuth2 provider (Keycloak / Cognito).
```