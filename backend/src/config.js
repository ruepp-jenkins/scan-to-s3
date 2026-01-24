import dotenv from 'dotenv';

dotenv.config();

function normalizeEndpoint(endpoint) {
  if (!endpoint) return endpoint;
  if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
    return endpoint;
  }
  return `https://${endpoint}`;
}

export const config = {
  port: process.env.PORT || 3000,

  auth: {
    username: process.env.AUTH_USERNAME || 'admin',
    passwordHash: process.env.AUTH_PASSWORD_HASH,
    jwtSecret: process.env.JWT_SECRET,
    jwtExpiresIn: process.env.JWT_EXPIRES_IN || '24h',
  },

  s3: {
    endpoint: normalizeEndpoint(process.env.S3_ENDPOINT),
    region: process.env.S3_REGION || 'eu-central-1',
    bucket: process.env.S3_BUCKET,
    accessKeyId: process.env.S3_ACCESS_KEY_ID,
    secretAccessKey: process.env.S3_SECRET_ACCESS_KEY,
    presignedUrlExpiry: parseInt(process.env.PRESIGNED_URL_EXPIRY || '300', 10),
  },

  cors: {
    origin: process.env.CORS_ORIGIN || '*',
  },
};

export function validateConfig() {
  const required = [
    ['AUTH_PASSWORD_HASH', config.auth.passwordHash],
    ['JWT_SECRET', config.auth.jwtSecret],
    ['S3_ENDPOINT', config.s3.endpoint],
    ['S3_BUCKET', config.s3.bucket],
    ['S3_ACCESS_KEY_ID', config.s3.accessKeyId],
    ['S3_SECRET_ACCESS_KEY', config.s3.secretAccessKey],
  ];

  const missing = required.filter(([name, value]) => !value).map(([name]) => name);

  if (missing.length > 0) {
    throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
  }
}
