function createLogEntry(level, event, payload = {}) {
  return {
    level,
    event,
    timestamp: new Date().toISOString(),
    ...payload,
  };
}

export function logInfo(event, payload = {}) {
  console.log(JSON.stringify(createLogEntry('info', event, payload)));
}

export function logError(event, payload = {}) {
  console.error(JSON.stringify(createLogEntry('error', event, payload)));
}

export function setRequestAuth(req, update) {
  req.requestAuth = {
    ...(req.requestAuth || {}),
    ...update,
  };

  return req.requestAuth;
}

export function getRequestAuth(req) {
  return req.requestAuth || {};
}

export function getClientIp(req) {
  if (req.ip) {
    return req.ip;
  }

  const forwardedFor = req.headers['x-forwarded-for'];
  if (typeof forwardedFor === 'string') {
    return forwardedFor.split(',')[0].trim();
  }

  return req.socket?.remoteAddress || 'unknown';
}
