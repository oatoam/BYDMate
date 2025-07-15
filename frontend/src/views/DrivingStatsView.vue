<template>
  <div class="driving-stats-view">
    <h1>驾驶行为统计页面</h1>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>驾驶行为统计</span>
        </div>
      </template>
      <div v-if="drivingStats.length > 0">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="驾驶次数" :value="totalDrivingCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="总里程 (km)" :value="totalMileage" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="总油耗 (L)" :value="totalFuelConsumption" />
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="8">
            <el-statistic title="总电耗 (kWh)" :value="totalElectricConsumption" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="平均里程/次 (km)" :value="avgMileagePerDrive" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="平均油耗/次 (L)" :value="avgFuelConsumptionPerDrive" />
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="8">
            <el-statistic title="平均电耗/次 (kWh)" :value="avgElectricConsumptionPerDrive" />
          </el-col>
        </el-row>

        <el-divider />

        <h3>每日驾驶统计趋势</h3>
        <LineChart :option="drivingStatsChartOption" v-if="drivingStatsChartOption" />
      </div>
      <div v-else>
        <p>加载中...</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '../api';
import LineChart from '../components/LineChart.vue';
import { ElCard, ElRow, ElCol, ElStatistic, ElDivider } from 'element-plus';

const drivingStats = ref<any[]>([]);

const totalDrivingCount = computed(() => drivingStats.value.reduce((sum, item) => sum + item.driving_count, 0));
const totalMileage = computed(() => drivingStats.value.reduce((sum, item) => sum + item.total_mileage, 0).toFixed(2));
const totalFuelConsumption = computed(() => drivingStats.value.reduce((sum, item) => sum + item.total_fuel_consumption, 0).toFixed(2));
const totalElectricConsumption = computed(() => drivingStats.value.reduce((sum, item) => sum + item.total_electric_consumption, 0).toFixed(2));
const avgMileagePerDrive = computed(() => (totalMileage.value / totalDrivingCount.value).toFixed(2));
const avgFuelConsumptionPerDrive = computed(() => (totalFuelConsumption.value / totalDrivingCount.value).toFixed(2));
const avgElectricConsumptionPerDrive = computed(() => (totalElectricConsumption.value / totalDrivingCount.value).toFixed(2));

const drivingStatsChartOption = computed(() => {
  const dates = drivingStats.value.map(item => item.stat_date);
  const drivingCounts = drivingStats.value.map(item => item.driving_count);
  const totalMileages = drivingStats.value.map(item => item.total_mileage);
  const totalFuelConsumptions = drivingStats.value.map(item => item.total_fuel_consumption);
  const totalElectricConsumptions = drivingStats.value.map(item => item.total_electric_consumption);

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['驾驶次数', '总里程', '总油耗', '总电耗'] },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '次数' },
      { type: 'value', name: '里程/油耗/电耗' }
    ],
    series: [
      { name: '驾驶次数', type: 'line', data: drivingCounts, yAxisIndex: 0 },
      { name: '总里程', type: 'line', data: totalMileages, yAxisIndex: 1 },
      { name: '总油耗', type: 'line', data: totalFuelConsumptions, yAxisIndex: 1 },
      { name: '总电耗', type: 'line', data: totalElectricConsumptions, yAxisIndex: 1 }
    ],
  };
});

const fetchDrivingStats = async () => {
  try {
    const response = await api.getDrivingStats({
      start_date: '2025-07-01', // 示例日期
      end_date: '2025-07-07',   // 示例日期
      period_type: 'daily',
    });
    drivingStats.value = response;
  } catch (error) {
    console.error('获取驾驶行为统计失败:', error);
  }
};

onMounted(() => {
  fetchDrivingStats();
});
</script>

<style scoped>
.driving-stats-view {
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