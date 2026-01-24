const Config = {
  // Backend API base URL (empty string = same origin)
  // Examples: '', 'http://localhost:3000', 'https://api.example.com'
	API_BASE_URL: 'http://10.10.50.120:3001',

  getApiUrl(path) {
    return `${this.API_BASE_URL}${path}`;
  },
};

export default Config;
