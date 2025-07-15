<template>
  <div class="trip-list-view">
    <h1>行程列表页面</h1>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>行程列表</span>
          <el-select v-model="pageSize" placeholder="每页显示" @change="handlePageSizeChange" style="width: 120px;">
            <el-option
              v-for="item in [10, 20, 50, 100]"
              :key="item"
              :label="item + ' 条/页'"
              :value="item"
            ></el-option>
          </el-select>
        </div>
      </template>
      <DataTable
        :data="trips"
        :columns="tripColumns"
        :loading="loading"
        :pagination="true"
        :total="totalTrips"
        :current-page="currentPage"
        :page-size="pageSize"
        @size-change="handlePageSizeChange"
        @current-change="handleCurrentPageChange"
        @row-click="showTripDetails"
      >
        <template #actions="{ row }">
          <el-button type="primary" size="small">详情</el-button>
        </template>
      </DataTable>
    </el-card>

    <el-dialog v-model="dialogVisible" title="行程详情" width="80%">
      <div v-if="selectedTripDetails">
        <h3>行程ID: {{ selectedTripDetails.id }}</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>数据折线图</span>
              </template>
              <LineChart :option="drivePowerChartOption" v-if="drivePowerChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>电池放电功率折线图</span>
              </template>
              <LineChart :option="dischargePowerChartOption" v-if="dischargePowerChartOption" />
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>速度折线图</span>
              </template>
              <LineChart :option="speedChartOption" v-if="speedChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>海拔折线图</span>
              </template>
              <LineChart :option="altitudeChartOption" v-if="altitudeChartOption" />
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>电池电量折线图</span>
              </template>
              <LineChart :option="socChartOption" v-if="socChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>油量折线图</span>
              </template>
              <LineChart :option="fuelLevelChartOption" v-if="fuelLevelChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>轨迹地图</span>
              </template>
              <MapView :paths="tripDetailPaths" :path-colors="tripDetailPathColors" />
            </el-card>
          </el-col>
        </el-row>
      </div>
      <div v-else>
        <p>加载行程详情中...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import api from '../api';
import DataTable from '../components/DataTable.vue';
import LineChart from '../components/LineChart.vue';
import MapView from '../components/MapView.vue';
import { ElCard, ElButton, ElDialog, ElRow, ElCol, ElSelect, ElOption } from 'element-plus';

const trips = ref<any[]>([]);
const loading = ref(false);
const totalTrips = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const dialogVisible = ref(false);
const selectedTripDetails = ref<any>(null);
const drivePowerChartOption = ref<any>(null);
const dischargePowerChartOption = ref<any>(null);
const speedChartOption = ref<any>(null);
const altitudeChartOption = ref<any>(null);
const socChartOption = ref<any>(null);
const fuelLevelChartOption = ref<any>(null);
const tripDetailPaths = ref<any[]>([]);
const tripDetailPathColors = ref<string[]>([]);

const tripColumns = [
  { prop: 'start_time', label: '开始时间', formatter: (row: any) => new Date(row.start_time).toLocaleString() },
  { prop: 'end_time', label: '结束时间', formatter: (row: any) => new Date(row.end_time).toLocaleString() },
  { prop: 'start_location', label: '起始位置' },
  { prop: 'end_location', label: '结束位置' },
  { prop: 'total_mileage', label: '里程 (km)' },
  { prop: 'total_fuel_consumption', label: '油耗 (L)' },
  { prop: 'total_electric_consumption', label: '电耗 (kWh)' },
  { prop: 'actions', label: '操作', slot: 'actions' },
];

const fetchTrips = async () => {
  loading.value = true;
  try {
    const response = await api.getTrips({
      page: currentPage.value,
      page_size: pageSize.value,
      sort_by: 'start_time',
      sort_order: 'desc',
    });
    trips.value = response.data;
    totalTrips.value = response.total;
  } catch (error) {
    console.error('获取行程列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handlePageSizeChange = (val: number) => {
  pageSize.value = val;
  currentPage.value = 1; // 改变每页大小后回到第一页
  fetchTrips();
};

const handleCurrentPageChange = (val: number) => {
  currentPage.value = val;
  fetchTrips();
};

const showTripDetails = async (trip: any) => {
  dialogVisible.value = true;
  selectedTripDetails.value = null; // 清空上次详情
  drivePowerChartOption.value = null;
  dischargePowerChartOption.value = null;
  speedChartOption.value = null;
  altitudeChartOption.value = null;
  socChartOption.value = null;
  fuelLevelChartOption.value = null;
  tripDetailPaths.value = [];
  tripDetailPathColors.value = [];

  try {
    const details = await api.getTripDetails(trip.id);
    selectedTripDetails.value = { ...trip, details };

    // 准备图表数据
    const timestamps = details.map((d: any) => new Date(d.timestamp).toLocaleTimeString());
    const drivePower = details.map((d: any) => d.drive_power);
    const dischargePower = details.map((d: any) => d.discharge_power);
    const speed = details.map((d: any) => d.speed);
    const altitude = details.map((d: any) => d.altitude);
    const soc = details.map((d: any) => d.soc);
    const fuelLevel = details.map((d: any) => d.fuel_level);

    drivePowerChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '驱动功率' },
      series: [{ name: '驱动功率', type: 'line', data: drivePower }],
    };

    dischargePowerChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '电池放电功率' },
      series: [{ name: '电池放电功率', type: 'line', data: dischargePower }],
    };

    speedChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '速度' },
      series: [{ name: '速度', type: 'line', data: speed }],
    };

    altitudeChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '海拔' },
      series: [{ name: '海拔', type: 'line', data: altitude }],
    };

    socChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '电池电量' },
      series: [{ name: '电池电量', type: 'line', data: soc }],
    };

    fuelLevelChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '油量' },
      series: [{ name: '油量', type: 'line', data: fuelLevel }],
    };

    // 准备轨迹地图数据 (速度颜色标识：红快蓝慢)
    tripDetailPaths.value = details.map((d: any) => [d.longitude, d.latitude]);
    tripDetailPathColors.value = details.slice(0, -1).map((d: any) => {
      const currentSpeed = d.speed;
      if (currentSpeed > 80) return '#FF0000'; // 红色：快
      if (currentSpeed > 40) return '#FFA500'; // 橙色：中
      return '#0000FF'; // 蓝色：慢
    });

  } catch (error) {
    console.error('获取行程详情失败:', error);
    selectedTripDetails.value = null;
  }
};

onMounted(() => {
  fetchTrips();
});
</script>

<style scoped>
.trip-list-view {
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