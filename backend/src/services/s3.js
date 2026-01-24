import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { config } from '../config.js';

const s3Client = new S3Client({
  endpoint: config.s3.endpoint,
  region: config.s3.region,
  credentials: {
    accessKeyId: config.s3.accessKeyId,
    secretAccessKey: config.s3.secretAccessKey,
  },
  forcePathStyle: true,
});

function sanitizeFilename(filename) {
  return filename
    .replace(/[^a-zA-Z0-9._-]/g, '_')
    .replace(/_{2,}/g, '_')
    .substring(0, 255);
}

export async function generatePresignedUploadUrl(originalFilename) {
  const sanitized = sanitizeFilename(originalFilename);
  const key = `uploads/${sanitized}`;

  const command = new PutObjectCommand({
    Bucket: config.s3.bucket,
    Key: key,
    ContentType: 'application/pdf',
    IfNoneMatch: '*',
  });

  const url = await getSignedUrl(s3Client, command, {
    expiresIn: config.s3.presignedUrlExpiry,
    signableHeaders: new Set(['content-type', 'if-none-match']),
  });

  return {
    url,
    key,
    headers: {
      'Content-Type': 'application/pdf',
      'If-None-Match': '*',
    },
  };
}

export { s3Client };
