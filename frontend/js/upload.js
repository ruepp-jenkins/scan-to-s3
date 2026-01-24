import Auth from './auth.js';
import Config from './config.js';

const Upload = {
  async getPresignedUrl(filename) {
    const response = await fetch(Config.getApiUrl('/api/upload/presigned-url'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...Auth.getAuthHeaders(),
      },
      body: JSON.stringify({ filename }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to get upload URL');
    }

    return data;
  },

  uploadFile(file, presignedData, onProgress) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable && onProgress) {
          const percent = Math.round((event.loaded / event.total) * 100);
          onProgress(percent);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve({ success: true, key: presignedData.key });
        } else if (xhr.status === 412) {
          reject(new Error('A file with this name already exists'));
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Network error during upload'));
      });

      xhr.addEventListener('abort', () => {
        reject(new Error('Upload cancelled'));
      });

      xhr.open('PUT', presignedData.uploadUrl);

      Object.entries(presignedData.headers).forEach(([key, value]) => {
        xhr.setRequestHeader(key, value);
      });

      xhr.send(file);
    });
  },
};

export default Upload;
