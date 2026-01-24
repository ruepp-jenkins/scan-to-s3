import Auth from './auth.js';
import Upload from './upload.js';

const app = {
  fileQueue: [],
  isUploading: false,

  init() {
    this.bindElements();
    this.bindEvents();
    this.checkAuth();
  },

  bindElements() {
    this.loginSection = document.getElementById('login-section');
    this.uploadSection = document.getElementById('upload-section');
    this.loginForm = document.getElementById('login-form');
    this.loginError = document.getElementById('login-error');
    this.logoutBtn = document.getElementById('logout-btn');
    this.dropZone = document.getElementById('drop-zone');
    this.fileInput = document.getElementById('file-input');
    this.fileList = document.getElementById('file-list');
    this.clearAllBtn = document.getElementById('clear-all-btn');
    this.resultsSummary = document.getElementById('results-summary');
  },

  bindEvents() {
    this.loginForm.addEventListener('submit', (e) => this.handleLogin(e));
    this.logoutBtn.addEventListener('click', () => this.handleLogout());

    this.dropZone.addEventListener('click', () => this.fileInput.click());
    this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e.target.files));

    this.dropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      this.dropZone.classList.add('drag-over');
    });

    this.dropZone.addEventListener('dragleave', () => {
      this.dropZone.classList.remove('drag-over');
    });

    this.dropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      this.dropZone.classList.remove('drag-over');
      this.handleFileSelect(e.dataTransfer.files);
    });

    this.clearAllBtn.addEventListener('click', () => this.clearAll());
  },

  checkAuth() {
    if (Auth.isAuthenticated()) {
      this.showUploadSection();
    } else {
      this.showLoginSection();
    }
  },

  showLoginSection() {
    this.loginSection.classList.remove('hidden');
    this.uploadSection.classList.add('hidden');
  },

  showUploadSection() {
    this.loginSection.classList.add('hidden');
    this.uploadSection.classList.remove('hidden');
  },

  async handleLogin(e) {
    e.preventDefault();
    this.loginError.textContent = '';

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
      await Auth.login(username, password);
      this.showUploadSection();
      this.loginForm.reset();
    } catch (err) {
      this.loginError.textContent = err.message;
    }
  },

  handleLogout() {
    Auth.logout();
    this.clearAll();
    this.showLoginSection();
  },

  handleFileSelect(files) {
    const pdfFiles = Array.from(files).filter(
      (f) => f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf')
    );

    if (pdfFiles.length === 0) {
      alert('Please select PDF files only');
      return;
    }

    pdfFiles.forEach((file) => {
      const id = `file-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      this.fileQueue.push({ id, file, status: 'pending', progress: 0 });
    });

    this.renderFileList();
    this.fileInput.value = '';
    this.startUploadIfNeeded();
  },

  startUploadIfNeeded() {
    if (this.isUploading) return;
    const hasPending = this.fileQueue.some((f) => f.status === 'pending');
    if (hasPending) {
      this.uploadAll();
    }
  },

  getStatusEmoji(status) {
    switch (status) {
      case 'pending': return '⏳';
      case 'uploading': return '⬆️';
      case 'success': return '✅';
      case 'error': return '❌';
      default: return '📄';
    }
  },

  renderFileList() {
    this.fileList.innerHTML = this.fileQueue
      .map(
        (item) => `
      <div class="file-item" id="${item.id}">
        <div class="file-info">
          <span class="file-name">${this.getStatusEmoji(item.status)} ${this.escapeHtml(item.file.name)}</span>
          <span class="file-size">${this.formatSize(item.file.size)}</span>
        </div>
        <div class="file-status ${item.status}">
          ${this.getStatusHtml(item)}
        </div>
        <button class="remove-btn" onclick="app.removeFile('${item.id}')" ${
          item.status === 'uploading' ? 'disabled' : ''
        }>x</button>
      </div>
    `
      )
      .join('');

    this.clearAllBtn.disabled = this.fileQueue.length === 0;
  },

  getStatusHtml(item) {
    switch (item.status) {
      case 'pending':
        return '<span class="status-text">Ready</span>';
      case 'uploading':
        return `<div class="progress-bar"><div class="progress-fill" style="width: ${item.progress}%"></div></div>
                <span class="progress-text">${item.progress}%</span>`;
      case 'success':
        return '<span class="status-text success">Uploaded</span>';
      case 'error':
        return `<span class="status-text error" title="${this.escapeHtml(item.error || '')}">${this.escapeHtml(item.error || 'Failed')}</span>`;
      default:
        return '';
    }
  },

  removeFile(id) {
    this.fileQueue = this.fileQueue.filter((f) => f.id !== id);
    this.renderFileList();
  },

  clearAll() {
    this.fileQueue = [];
    this.renderFileList();
    this.resultsSummary.classList.add('hidden');
  },

  async uploadAll() {
    if (this.isUploading) return;
    this.isUploading = true;
    this.resultsSummary.classList.add('hidden');

    let successCount = 0;
    let errorCount = 0;

    while (true) {
      const item = this.fileQueue.find((f) => f.status === 'pending');
      if (!item) break;

      item.status = 'uploading';
      item.progress = 0;
      this.renderFileList();

      try {
        const presignedData = await Upload.getPresignedUrl(item.file.name);

        await Upload.uploadFile(item.file, presignedData, (progress) => {
          item.progress = progress;
          this.renderFileList();
        });

        item.status = 'success';
        successCount++;
      } catch (err) {
        item.status = 'error';
        item.error = err.message;
        errorCount++;
      }

      this.renderFileList();
    }

    this.isUploading = false;
    if (successCount > 0 || errorCount > 0) {
      this.showSummary(successCount, errorCount);
    }
  },

  showSummary(successCount, errorCount) {
    const icon = errorCount > 0 ? '⚠️' : '🎉';
    this.resultsSummary.innerHTML = `
      <strong>${icon} Upload Complete:</strong>
      ✅ ${successCount} succeeded, ❌ ${errorCount} failed
    `;
    this.resultsSummary.classList.remove('hidden');
    this.resultsSummary.className = `results-summary ${errorCount > 0 ? 'has-errors' : 'all-success'}`;
  },

  escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  },

  formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  },
};

window.app = app;
document.addEventListener('DOMContentLoaded', () => app.init());
