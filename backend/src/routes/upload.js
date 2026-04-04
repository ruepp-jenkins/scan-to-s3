import { Router } from 'express';
import { config } from '../config.js';
import { authenticateToken } from '../middleware/auth.js';
import { generatePresignedUploadUrl } from '../services/s3.js';
import { getClientIp, getRequestAuth, logError, logInfo } from '../logger.js';

const router = Router();

router.post('/presigned-url', authenticateToken, async (req, res) => {
  try {
    const { filename } = req.body;

    if (!filename) {
      return res.status(400).json({ error: 'Filename is required' });
    }

    if (filename.length > 255) {
      return res.status(400).json({ error: 'Filename too long (max 255 characters)' });
    }

    const lowerFilename = filename.toLowerCase();
    if (!lowerFilename.endsWith('.pdf')) {
      return res.status(400).json({ error: 'Only PDF files are allowed' });
    }

    const presignedData = await generatePresignedUploadUrl(filename);
    const requestedAt = new Date();
    const expiresAt = new Date(requestedAt.getTime() + (config.s3.presignedUrlExpiry * 1000));
    const auth = getRequestAuth(req);

    logInfo('presigned_url_issued', {
      requestedAt: requestedAt.toISOString(),
      ip: getClientIp(req),
      path: req.originalUrl,
      filename,
      key: presignedData.key,
      expiresAt: expiresAt.toISOString(),
      uploadUrl: presignedData.url,
      authorized: Boolean(auth.authorized),
      authType: auth.type || null,
      authReason: auth.reason || null,
      username: auth.username || null,
    });

    res.json({
      uploadUrl: presignedData.url,
      key: presignedData.key,
      headers: presignedData.headers,
    });
  } catch (err) {
    logError('presigned_url_error', {
      path: req.originalUrl,
      ip: getClientIp(req),
      message: err.message,
      stack: err.stack,
    });
    res.status(500).json({ error: 'Failed to generate upload URL' });
  }
});

export default router;
