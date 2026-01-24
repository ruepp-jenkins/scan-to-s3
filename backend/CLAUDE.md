# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Install dependencies
npm install

# Run development server (with watch mode)
npm run dev

# Run production server
npm start

# Generate bcrypt password hash
node -e "console.log(require('bcryptjs').hashSync('your-password', 12))"

# Build and run with Docker (from project root)
docker-compose up --build
```

## Architecture

This is a PDF upload application with browser-to-S3 direct uploads via presigned URLs.

**Backend (Node.js/Express with ES modules)**
- `src/index.js` - Express app entry point, serves static frontend from `../public`
- `src/config.js` - Environment variable loader with validation
- `src/middleware/auth.js` - JWT token verification middleware
- `src/routes/auth.js` - Login endpoint, validates against bcrypt hash
- `src/routes/upload.js` - Generates presigned S3 PUT URLs (protected route)
- `src/services/s3.js` - AWS SDK v3 S3 client configuration and presigned URL generation

**Frontend (plain HTML/CSS/JS)**
- Located in `../frontend/`, served as static files from `/public` in Docker
- `js/auth.js` - JWT token management in localStorage
- `js/upload.js` - XHR-based file upload with progress tracking
- `js/app.js` - UI logic for drag-drop and file queue management

**Upload Flow**
1. Frontend requests presigned URL from `/api/upload/presigned-url`
2. Backend generates time-limited S3 PUT URL with `If-None-Match: *` header
3. Frontend uploads directly to S3 using XHR (for progress tracking)
4. 412 response indicates duplicate file (if S3 supports conditional writes)

## Environment

Required variables: `AUTH_PASSWORD_HASH`, `JWT_SECRET`, `S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`

See `.env.example` in project root for full list.
