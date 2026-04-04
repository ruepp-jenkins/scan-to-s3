import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import { setRequestAuth } from '../logger.js';

export function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    setRequestAuth(req, {
      authorized: false,
      type: 'jwt',
      reason: 'missing_bearer_token',
    });
    return res.status(401).json({ error: 'Authentication required' });
  }

  try {
    const payload = jwt.verify(token, config.auth.jwtSecret);
    req.user = payload;
    setRequestAuth(req, {
      authorized: true,
      type: 'jwt',
      reason: 'valid_jwt',
      username: payload.username || null,
    });
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      setRequestAuth(req, {
        authorized: false,
        type: 'jwt',
        reason: 'expired_jwt',
      });
      return res.status(401).json({ error: 'Token expired' });
    }

    setRequestAuth(req, {
      authorized: false,
      type: 'jwt',
      reason: 'invalid_jwt',
    });
    return res.status(403).json({ error: 'Invalid token' });
  }
}
