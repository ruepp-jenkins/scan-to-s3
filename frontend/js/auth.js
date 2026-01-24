import Config from './config.js';

const Auth = {
  TOKEN_KEY: 'pdf_uploader_token',

  async login(username, password) {
    const response = await fetch(Config.getApiUrl('/api/auth/login'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Login failed');
    }

    localStorage.setItem(this.TOKEN_KEY, data.token);
    return data.token;
  },

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
  },

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  isAuthenticated() {
    const token = this.getToken();
    if (!token) return false;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      return Date.now() < expiry;
    } catch {
      return false;
    }
  },

  getAuthHeaders() {
    const token = this.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  },
};

export default Auth;
