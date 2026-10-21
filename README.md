Document Management System (DMS) that lets users upload, organize, search, share and collaborate on documents. The work covers three layers:
• Front-end: an admin and user dashboard (user management, folders, uploads with metadata, search, pagination, filters, favorites).
• Backend: a microservices architecture with Java Spring-boot (authentication, documents, search, notifications, workflow) behind an API Gateway.
• Cross-cutting concerns: JWT security with optional Keycloak delegation, S3-compatible storage (MinIO), automated testing, CI/CD, reactive programming and an analytics dashboard.
