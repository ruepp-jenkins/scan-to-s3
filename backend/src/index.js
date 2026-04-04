import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { config, validateConfig } from './config.js';
import authRoutes from './routes/auth.js';
import uploadRoutes from './routes/upload.js';
import appRoutes from './routes/app.js';
import { apiRequestLogger } from './middleware/requestLogger.js';
import { getClientIp, logError, logInfo } from './logger.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

validateConfig();

const app = express();

app.set('trust proxy', 1);

app.use(cors({
  origin: config.cors.origin,
  credentials: true,
}));

app.use(apiRequestLogger);

app.use(express.json());

app.use(express.static(path.join(__dirname, '../public')));

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/auth', authRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/app', appRoutes);

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

app.use((err, req, res, next) => {
  logError('unhandled_error', {
    method: req.method,
    path: req.originalUrl,
    ip: getClientIp(req),
    message: err.message,
    stack: err.stack,
  });
  res.status(500).json({ error: 'Internal server error' });
});

app.listen(config.port, () => {
  logInfo('server_started', {
    port: config.port,
  });
});
