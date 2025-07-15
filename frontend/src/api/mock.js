// frontend/src/api/mock.js

const mockData = {
  // 1. 汽车状态信息
  '/api/vehicle/status': {
    vehicle_id: 'mock-vehicle-1',
    timestamp: new Date().toISOString(),
    latitude: 34.0522 + Math.random() * 0.1,
    longitude: -118.2437 + Math.random() * 0.1,
    speed: Math.floor(Math.random() * 120),
    mileage: Math.floor(Math.random() * 100000),
    fuel_level: Math.floor(Math.random() * 100),
    soc: Math.floor(Math.random() * 100),
    battery_temp: Math.floor(Math.random() * 50),
    charge_status: Math.random() > 0.5 ? 'charging' : 'idle',
    power: Math.floor(Math.random() * 100)
  },

  // 2. 行程列表
  '/api/trips': (params) => {
    const page = parseInt(params.page || 1);
    const page_size = parseInt(params.page_size || 10);
    const total = 100;
    const data = [];
    for (let i = 0; i < page_size; i++) {
      const id = `mock-trip-${(page - 1) * page_size + i + 1}`;
      const startTime = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000);
      const endTime = new Date(startTime.getTime() + Math.random() * 3 * 60 * 60 * 1000);
      data.push({
        id: id,
        vehicle_id: 'mock-vehicle-1',
        start_time: startTime.toISOString(),
        end_time: endTime.toISOString(),
        start_location: `Mock Location A ${id}`,
        end_location: `Mock Location B ${id}`,
        total_mileage: parseFloat((Math.random() * 200).toFixed(2)),
        total_fuel_consumption: parseFloat((Math.random() * 20).toFixed(2)),
        total_electric_consumption: parseFloat((Math.random() * 30).toFixed(2))
      });
    }
    return { total, page, page_size, data };
  },

  // 3. 单次行程详情
  '/api/trips/:trip_id/details': (params) => {
    const tripId = params.trip_id;
    const details = [];
    const startTime = new Date(Date.now() - 2 * 60 * 60 * 1000);
    for (let i = 0; i < 300; i++) { // 增加数据点数量
      details.push({
        timestamp: new Date(startTime.getTime() + i * 60 * 1000).toISOString(),
        drive_power: parseFloat((Math.random() * 100).toFixed(2)),
        discharge_power: parseFloat((Math.random() * 50).toFixed(2)),
        speed: parseFloat((Math.random() * 120).toFixed(2)),
        altitude: parseFloat((Math.random() * 500).toFixed(2)),
        soc: parseFloat((Math.random() * 100).toFixed(2)),
        fuel_level: parseFloat((Math.random() * 100).toFixed(2)),
        latitude: 34.0522 + Math.random() * 0.01,
        longitude: -118.2437 + Math.random() * 0.01
      });
    }
    return details;
  },

  // 4. 充电记录列表
  '/api/charges': (params) => {
    const page = parseInt(params.page || 1);
    const page_size = parseInt(params.page_size || 10);
    const total = 50;
    const data = [];
    for (let i = 0; i < page_size; i++) {
      const id = `mock-charge-${(page - 1) * page_size + i + 1}`;
      const startTime = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000);
      const endTime = new Date(startTime.getTime() + Math.random() * 2 * 60 * 60 * 1000);
      data.push({
        id: id,
        vehicle_id: 'mock-vehicle-1',
        start_time: startTime.toISOString(),
        end_time: endTime.toISOString(),
        charge_location: `Mock Charging Station ${id}`,
        start_soc: parseFloat((Math.random() * 30).toFixed(2)),
        end_soc: parseFloat((70 + Math.random() * 30).toFixed(2)),
        charged_kwh: parseFloat((Math.random() * 60).toFixed(2)),
        charge_type: Math.random() > 0.5 ? 'DC' : 'AC'
      });
    }
    return { total, page, page_size, data };
  },

  // 5. 单次充电详情
  '/api/charges/:charge_id/details': (params) => {
    const chargeId = params.charge_id;
    const details = [];
    const startTime = new Date(Date.now() - 1 * 60 * 60 * 1000);
    for (let i = 0; i < 150; i++) { // 增加数据点数量
      details.push({
        timestamp: new Date(startTime.getTime() + i * 2 * 60 * 1000).toISOString(),
        soc: parseFloat((Math.random() * 100).toFixed(2)),
        charge_power: parseFloat((Math.random() * 100).toFixed(2)),
        battery_temp: parseFloat((Math.random() * 40).toFixed(2)),
        battery_voltage: parseFloat((300 + Math.random() * 100).toFixed(2)),
        charge_voltage: parseFloat((350 + Math.random() * 100).toFixed(2))
      });
    }
    return details;
  },

  // 6. 驾驶行为统计数据
  '/api/driving-stats': (params) => {
    const data = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(Date.now() - i * 24 * 60 * 60 * 1000);
      data.push({
        stat_date: date.toISOString().split('T')[0],
        driving_count: Math.floor(Math.random() * 5),
        total_mileage: parseFloat((Math.random() * 300).toFixed(2)),
        total_fuel_consumption: parseFloat((Math.random() * 30).toFixed(2)),
        total_electric_consumption: parseFloat((Math.random() * 50).toFixed(2)),
        avg_mileage_per_drive: parseFloat((Math.random() * 60).toFixed(2)),
        avg_fuel_consumption_per_drive: parseFloat((Math.random() * 6).toFixed(2)),
        avg_electric_consumption_per_drive: parseFloat((Math.random() * 10).toFixed(2))
      });
    }
    return data;
  },

  // 7. 轨迹统计数据
  '/api/location-stats': (params) => {
    return [
      { location_type: 'current_stop', name: 'Current Location', latitude: 34.0522, longitude: -118.2437, count: 1 },
      { location_type: 'top_destination', name: 'Home', latitude: 34.06, longitude: -118.25, count: 15 },
      { location_type: 'top_destination', name: 'Work', latitude: 34.04, longitude: -118.23, count: 12 },
      { location_type: 'top_stay', name: 'Shopping Mall', latitude: 34.055, longitude: -118.245, count: 8 },
      { location_type: 'charge_location', name: 'Public Charger', latitude: 34.05, longitude: -118.24, count: 5 }
    ];
  },

  // 8. 数据特性分析结果
  '/api/data-features': (params) => {
    return [
      { feature_type: 'avg_speed_in_area', feature_value: 45.5, area_info: { name: 'Downtown', geojson: '...' } },
      { feature_type: 'avg_speed_in_area', feature_value: 70.2, area_info: { name: 'Highway', geojson: '...' } }
    ];
  }
};

export default function mockApi(url, params = {}) {
  // Simple router for mock API
  for (const path in mockData) {
    if (path.includes(':')) {
      const regexPath = path.replace(/:(\w+)/g, '(?<$1>[^/]+)');
      const match = url.match(new RegExp(`^${regexPath}$`));
      if (match) {
        const pathParams = match.groups;
        if (typeof mockData[path] === 'function') {
          return Promise.resolve(mockData[path]({ ...params, ...pathParams }));
        }
      }
    } else if (url === path) {
      if (typeof mockData[path] === 'function') {
        return Promise.resolve(mockData[path](params));
      }
      return Promise.resolve(mockData[path]);
    }
  }
  return Promise.reject(new Error(`Mock API not found for ${url}`));
}