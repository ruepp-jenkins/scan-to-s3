import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import rateLimit from 'express-rate-limit';
import { config } from '../config.js';
import { getClientIp, logError, setRequestAuth } from '../logger.js';

const router = Router();

const loginLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many login attempts, please try again later' },
  handler: (req, res, _next, options) => {
    setRequestAuth(req, {
      authorized: false,
      type: 'login',
      reason: 'rate_limited',
    });
    res.status(options.statusCode).json(options.message);
  },
});

router.post('/login', loginLimiter, async (req, res) => {
  try {
    setRequestAuth(req, {
      authorized: false,
      type: 'login',
      reason: 'pending_login_validation',
    });

    const { username, password } = req.body;

    if (!username || !password) {
      setRequestAuth(req, {
        authorized: false,
        type: 'login',
        reason: 'missing_credentials',
      });
      return res.status(400).json({ error: 'Username and password are required' });
    }

    if (username !== config.auth.username) {
      setRequestAuth(req, {
        authorized: false,
        type: 'login',
        reason: 'invalid_credentials',
      });
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const validPassword = await bcrypt.compare(password, config.auth.passwordHash);
    if (!validPassword) {
      setRequestAuth(req, {
        authorized: false,
        type: 'login',
        reason: 'invalid_credentials',
      });
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign(
      { username },
      config.auth.jwtSecret,
      { expiresIn: config.auth.jwtExpiresIn }
    );

    setRequestAuth(req, {
      authorized: true,
      type: 'login',
      reason: 'valid_credentials',
      username,
    });

    res.json({ token });
  } catch (err) {
    setRequestAuth(req, {
      authorized: false,
      type: 'login',
      reason: 'login_internal_error',
    });
    logError('login_error', {
      path: req.originalUrl,
      ip: getClientIp(req),
      message: err.message,
      stack: err.stack,
    });
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.post('/hash-password', async (req, res) => {
  if (process.env.NODE_ENV === 'production') {
    setRequestAuth(req, {
      authorized: false,
      type: 'hash_password',
      reason: 'endpoint_disabled_in_production',
    });
    return res.status(404).json({ error: 'Not found' });
  }

  const { password } = req.body;
  if (!password) {
    setRequestAuth(req, {
      authorized: false,
      type: 'hash_password',
      reason: 'missing_password',
    });
    return res.status(400).json({ error: 'Password is required' });
  }

  const hash = await bcrypt.hash(password, 12);
  setRequestAuth(req, {
    authorized: true,
    type: 'hash_password',
    reason: 'password_hashed',
  });
  res.json({ hash });
});

export default router;
