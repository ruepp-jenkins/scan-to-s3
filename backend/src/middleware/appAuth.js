import { config } from '../config.js';

export function authenticateAppToken(req, res, next) {
  if (!config.app.token) {
    return res.status(503).json({ error: 'App API is not configured' });
  }

  const token = req.headers['x-app-token'];

  if (!token) {
    return res.status(401).json({ error: 'App token required' });
  }

  if (token !== config.app.token) {
    return res.status(403).json({ error: 'Invalid app token' });
  }

  next();
}
