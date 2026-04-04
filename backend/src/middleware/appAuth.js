import { config } from '../config.js';
import { setRequestAuth } from '../logger.js';

export function authenticateAppToken(req, res, next) {
  if (!config.app.token) {
    setRequestAuth(req, {
      authorized: false,
      type: 'app_token',
      reason: 'app_api_not_configured',
    });
    return res.status(503).json({ error: 'App API is not configured' });
  }

  const token = req.headers['x-app-token'];

  if (!token) {
    setRequestAuth(req, {
      authorized: false,
      type: 'app_token',
      reason: 'missing_app_token',
    });
    return res.status(401).json({ error: 'App token required' });
  }

  if (token !== config.app.token) {
    setRequestAuth(req, {
      authorized: false,
      type: 'app_token',
      reason: 'invalid_app_token',
    });
    return res.status(403).json({ error: 'Invalid app token' });
  }

  setRequestAuth(req, {
    authorized: true,
    type: 'app_token',
    reason: 'valid_app_token',
  });

  next();
}
