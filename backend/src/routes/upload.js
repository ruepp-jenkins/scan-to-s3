import { Router } from 'express';
import { authenticateToken } from '../middleware/auth.js';
import { generatePresignedUploadUrl } from '../services/s3.js';

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

    res.json({
      uploadUrl: presignedData.url,
      key: presignedData.key,
      headers: presignedData.headers,
    });
  } catch (err) {
    console.error('Presigned URL error:', err);
    res.status(500).json({ error: 'Failed to generate upload URL' });
  }
});

export default router;
