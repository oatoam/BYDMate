interface Api {
  getVehicleStatus(params?: any): Promise<any>;
  getTrips(params?: any): Promise<any>;
  getTripDetails(tripId: string): Promise<any>;
  getCharges(params?: any): Promise<any>;
  getChargeDetails(chargeId: string): Promise<any>;
  getDrivingStats(params?: any): Promise<any>;
  getLocationStats(params?: any): Promise<any>;
  getDataFeatures(params?: any): Promise<any>;
}

declare const api: Api;
export default api;