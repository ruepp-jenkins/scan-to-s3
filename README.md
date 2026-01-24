# PDF Uploader

Web application for uploading PDF files to S3-compatible storage (Mega.nz S4) using presigned URLs.

## Features

- Username/password authentication with JWT
- Drag-and-drop multi-file upload
- Direct browser-to-S3 uploads via presigned URLs
- Progress tracking for each file
- Duplicate file detection (if storage supports `If-None-Match`)
- Docker deployment ready

## Quick Start

### 1. Generate Password Hash

```bash
node -e "console.log(require('bcryptjs').hashSync('your-password', 12))"
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your values
```

### 3. Run with Docker

use pre build image (recommended):
```bash
docker compose up -d
```

build your own image:
```bash
docker compose up -f docker-compose.build.yml -d --build
```

### 4. Access the Application

Open http://localhost:3000 in your browser.

## Development

### Prerequisites

- Node.js 20+
- npm

### Setup

```bash
cd backend
npm install
npm run dev
```

The server will start on http://localhost:3000.

## API Endpoints

### Authentication

**POST /api/auth/login**
```json
{
  "username": "admin",
  "password": "your-password"
}
```

Returns:
```json
{
  "token": "jwt-token"
}
```

### Upload

**POST /api/upload/presigned-url** (requires auth)
```json
{
  "filename": "document.pdf"
}
```

Returns:
```json
{
  "uploadUrl": "https://...",
  "key": "uploads/document.pdf",
  "headers": {
    "Content-Type": "application/pdf",
    "If-None-Match": "*"
  }
}
```

### Health Check

**GET /health**
```json
{
  "status": "ok",
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

## S3 Bucket CORS Configuration

Configure your S3 bucket to allow browser uploads:

```json
{
  "CORSRules": [{
    "AllowedOrigins": ["https://your-domain.com"],
    "AllowedMethods": ["PUT"],
    "AllowedHeaders": ["Content-Type", "If-None-Match"],
    "MaxAgeSeconds": 3000
  }]
}
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| AUTH_USERNAME | Login username | admin |
| AUTH_PASSWORD_HASH | Bcrypt hash of password | (required) |
| JWT_SECRET | Secret for signing JWTs | (required) |
| JWT_EXPIRES_IN | Token expiration time | 24h |
| S3_ENDPOINT | S3-compatible endpoint URL | (required) |
| S3_REGION | S3 region | eu-central-1 |
| S3_BUCKET | Target bucket name | (required) |
| S3_ACCESS_KEY_ID | S3 access key | (required) |
| S3_SECRET_ACCESS_KEY | S3 secret key | (required) |
| PRESIGNED_URL_EXPIRY | URL validity in seconds | 300 |
| PORT | Server port | 3000 |
| CORS_ORIGIN | Allowed origins | * |

## Security Notes

- Always use HTTPS in production
- Use a strong JWT_SECRET (32+ random characters)
- Set CORS_ORIGIN to your specific domain in production
- The password hash utility endpoint is disabled in production
