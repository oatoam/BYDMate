<template>
  <div class="dashboard-view">
    <h1>主看板页面</h1>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>汽车状态信息</span>
            </div>
          </template>
          <div v-if="vehicleStatus">
            <p>车辆ID: {{ vehicleStatus.vehicle_id }}</p>
            <p>时间戳: {{ new Date(vehicleStatus.timestamp).toLocaleString() }}</p>
            <p>速度: {{ vehicleStatus.speed }} km/h</p>
            <p>里程: {{ vehicleStatus.mileage }} km</p>
            <p>油量: {{ vehicleStatus.fuel_level }} %</p>
            <p>电量: {{ vehicleStatus.soc }} %</p>
            <p>充电状态: {{ vehicleStatus.charge_status }}</p>
          </div>
          <div v-else>
            <p>加载中...</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>最近行程信息</span>
            </div>
          </template>
          <DataTable :data="recentTrips" :columns="tripColumns" :pagination="false" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>里程历史折线图</span>
            </div>
          </template>
          <LineChart :option="mileageChartOption" v-if="mileageChartOption" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>油耗折线图</span>
            </div>
          </template>
          <LineChart :option="fuelChartOption" v-if="fuelChartOption" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>最近轨迹信息</span>
            </div>
          </template>
          <MapView :paths="recentTrackPaths" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>最近充电信息</span>
            </div>
          </template>
          <DataTable :data="recentCharges" :columns="chargeColumns" :pagination="false" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../api';
import DataTable from '../components/DataTable.vue';
import LineChart from '../components/LineChart.vue';
import MapView from '../components/MapView.vue';
import { ElRow, ElCol, ElCard } from 'element-plus';

const vehicleStatus = ref<any>(null);
const recentTrips = ref<any[]>([]);
const recentCharges = ref<any[]>([]);
const mileageChartOption = ref<any>(null);
const fuelChartOption = ref<any>(null);
const recentTrackPaths = ref<any[]>([]);

const tripColumns = [
  { prop: 'start_time', label: '开始时间', formatter: (row: any) => new Date(row.start_time).toLocaleString() },
  { prop: 'end_time', label: '结束时间', formatter: (row: any) => new Date(row.end_time).toLocaleString() },
  { prop: 'total_mileage', label: '里程 (km)' },
  { prop: 'total_fuel_consumption', label: '油耗 (L)' },
];

const chargeColumns = [
  { prop: 'start_time', label: '开始时间', formatter: (row: any) => new Date(row.start_time).toLocaleString() },
  { prop: 'end_time', label: '结束时间', formatter: (row: any) => new Date(row.end_time).toLocaleString() },
  { prop: 'charged_kwh', label: '充入电量 (kWh)' },
  { prop: 'charge_type', label: '类型' },
];

const fetchVehicleStatus = async () => {
  try {
    vehicleStatus.value = await api.getVehicleStatus();
  } catch (error) {
    console.error('获取汽车状态失败:', error);
  }
};

const fetchRecentTrips = async () => {
  try {
    const response = await api.getTrips({ page_size: 5 }); // 获取最近5条行程
    recentTrips.value = response.data;
  } catch (error) {
    console.error('获取最近行程失败:', error);
  }
};

const fetchRecentCharges = async () => {
  try {
    const response = await api.getCharges({ page_size: 5 }); // 获取最近5条充电记录
    recentCharges.value = response.data;
  } catch (error) {
    console.error('获取最近充电失败:', error);
  }
};

const fetchMileageAndFuelData = async () => {
  try {
    // 模拟获取历史里程和油耗数据，实际应调用后端API
    const mileageData = [];
    const fuelData = [];
    const dates = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(Date.now() - (6 - i) * 24 * 60 * 60 * 1000);
      dates.push(date.toLocaleDateString());
      mileageData.push(Math.floor(Math.random() * 1000) + 5000); // 模拟里程
      fuelData.push(parseFloat((Math.random() * 10 + 5).toFixed(2))); // 模拟油耗
    }

    mileageChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      series: [{ name: '里程', type: 'line', data: mileageData }],
    };

    fuelChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      series: [{ name: '油耗', type: 'line', data: fuelData }],
    };
  } catch (error) {
    console.error('获取里程和油耗数据失败:', error);
  }
};

const fetchRecentTrack = async () => {
  try {
    // 模拟获取最近轨迹数据，实际应调用后端API
    const response = await api.getTripDetails('mock-trip-1'); // 假设获取第一个mock行程的详情
    if (response && response.length > 0) {
      recentTrackPaths.value = response.map((item: any) => [item.longitude, item.latitude]);
    }
  } catch (error) {
    console.error('获取最近轨迹失败:', error);
  }
};

onMounted(() => {
  fetchVehicleStatus();
  fetchRecentTrips();
  fetchRecentCharges();
  fetchMileageAndFuelData();
  fetchRecentTrack();
});
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>