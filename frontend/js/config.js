const Config = {
  // Backend API base URL (empty string = same origin)
  // Examples: '', 'http://localhost:3000', 'https://api.example.com'
	API_BASE_URL: '',

  getApiUrl(path) {
    return `${this.API_BASE_URL}${path}`;
  },
};

export default Config;
