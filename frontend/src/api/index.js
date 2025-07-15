// frontend/src/api/index.js
import mockApi from './mock';
import axios from 'axios';

const USE_MOCK_API = import.meta.env.VITE_USE_MOCK_API === 'true';

const request = axios.create({
  baseURL: 'http://localhost:8081/api', // 后端API的基础路径
  timeout: 5000
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 在发送请求之前做些什么
    return config;
  },
  error => {
    // 对请求错误做些什么
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 对响应数据做些什么
    return response.data;
  },
  error => {
    // 对响应错误做些什么
    return Promise.reject(error);
  }
);

const api = {
  getVehicleStatus(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/vehicle/status', params);
    }
    return request.get('/vehicle/status', { params });
  },
  getTrips(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/trips', params);
    }
    return request.get('/trips', { params });
  },
  getTripDetails(tripId) {
    if (USE_MOCK_API) {
      return mockApi(`/api/trips/${tripId}/details`);
    }
    return request.get(`/trips/${tripId}/details`);
  },
  getCharges(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/charges', params);
    }
    return request.get('/charges', { params });
  },
  getChargeDetails(chargeId) {
    if (USE_MOCK_API) {
      return mockApi(`/api/charges/${chargeId}/details`);
    }
    return request.get(`/charges/${chargeId}/details`);
  },
  getDrivingStats(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/driving-stats', params);
    }
    return request.get('/driving-stats', { params });
  },
  getLocationStats(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/location-stats', params);
    }
    return request.get('/location-stats', { params });
  },
  getDataFeatures(params) {
    if (USE_MOCK_API) {
      return mockApi('/api/data-features', params);
    }
    return request.get('/data-features', { params });
  }
};

export default api;