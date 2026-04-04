import { getClientIp, getRequestAuth, logInfo, setRequestAuth } from '../logger.js';

export function apiRequestLogger(req, res, next) {
  if (!req.originalUrl.startsWith('/api/')) {
    return next();
  }

  const requestedAt = new Date().toISOString();
  const startedAt = process.hrtime.bigint();

  setRequestAuth(req, {
    authorized: true,
    type: 'public',
    reason: 'public_endpoint',
  });

  res.on('finish', () => {
    const durationMs = Number(process.hrtime.bigint() - startedAt) / 1_000_000;
    const auth = getRequestAuth(req);

    logInfo('api_request', {
      requestedAt,
      ip: getClientIp(req),
      method: req.method,
      path: req.originalUrl,
      statusCode: res.statusCode,
      durationMs: Number(durationMs.toFixed(2)),
      authorized: Boolean(auth.authorized),
      authType: auth.type || 'public',
      authReason: auth.reason || 'public_endpoint',
      username: auth.username || null,
    });
  });

  next();
}
