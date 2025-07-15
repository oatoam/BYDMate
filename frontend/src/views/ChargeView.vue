<template>
  <div class="charge-view">
    <h1>充电页面</h1>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>充电记录列表</span>
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
        :data="charges"
        :columns="chargeColumns"
        :loading="loading"
        :pagination="true"
        :total="totalCharges"
        :current-page="currentPage"
        :page-size="pageSize"
        @size-change="handlePageSizeChange"
        @current-change="handleCurrentPageChange"
        @row-click="showChargeDetails"
      >
        <template #actions="{ row }">
          <el-button type="primary" size="small">详情</el-button>
        </template>
      </DataTable>
    </el-card>

    <el-dialog v-model="dialogVisible" title="充电详情" width="80%">
      <div v-if="selectedChargeDetails">
        <h3>充电ID: {{ selectedChargeDetails.id }}</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>电量折线图</span>
              </template>
              <LineChart :option="socChartOption" v-if="socChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>充电功率折线图</span>
              </template>
              <LineChart :option="chargePowerChartOption" v-if="chargePowerChartOption" />
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>电池温度折线图</span>
              </template>
              <LineChart :option="batteryTempChartOption" v-if="batteryTempChartOption" />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>电池电压折线图</span>
              </template>
              <LineChart :option="batteryVoltageChartOption" v-if="batteryVoltageChartOption" />
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card class="box-card">
              <template #header>
                <span>充电电压折线图</span>
              </template>
              <LineChart :option="chargeVoltageChartOption" v-if="chargeVoltageChartOption" />
            </el-card>
          </el-col>
        </el-row>
      </div>
      <div v-else>
        <p>加载充电详情中...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../api';
import DataTable from '../components/DataTable.vue';
import LineChart from '../components/LineChart.vue';
import { ElCard, ElButton, ElDialog, ElRow, ElCol, ElSelect, ElOption } from 'element-plus';

const charges = ref<any[]>([]);
const loading = ref(false);
const totalCharges = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const dialogVisible = ref(false);
const selectedChargeDetails = ref<any>(null);
const socChartOption = ref<any>(null);
const chargePowerChartOption = ref<any>(null);
const batteryTempChartOption = ref<any>(null);
const batteryVoltageChartOption = ref<any>(null);
const chargeVoltageChartOption = ref<any>(null);

const chargeColumns = [
  { prop: 'start_time', label: '开始时间', formatter: (row: any) => new Date(row.start_time).toLocaleString() },
  { prop: 'end_time', label: '结束时间', formatter: (row: any) => new Date(row.end_time).toLocaleString() },
  { prop: 'charge_location', label: '充电位置' },
  { prop: 'start_soc', label: '开始电量 (%)' },
  { prop: 'end_soc', label: '结束电量 (%)' },
  { prop: 'charged_kwh', label: '充入电量 (kWh)' },
  { prop: 'charge_type', label: '类型' },
  { prop: 'actions', label: '操作', slot: 'actions' },
];

const fetchCharges = async () => {
  loading.value = true;
  try {
    const response = await api.getCharges({
      page: currentPage.value,
      page_size: pageSize.value,
      sort_by: 'start_time',
      sort_order: 'desc',
    });
    charges.value = response.data;
    totalCharges.value = response.total;
  } catch (error) {
    console.error('获取充电记录失败:', error);
  } finally {
    loading.value = false;
  }
};

const handlePageSizeChange = (val: number) => {
  pageSize.value = val;
  currentPage.value = 1;
  fetchCharges();
};

const handleCurrentPageChange = (val: number) => {
  currentPage.value = val;
  fetchCharges();
};

const showChargeDetails = async (charge: any) => {
  dialogVisible.value = true;
  selectedChargeDetails.value = null;
  socChartOption.value = null;
  chargePowerChartOption.value = null;
  batteryTempChartOption.value = null;
  batteryVoltageChartOption.value = null;
  chargeVoltageChartOption.value = null;

  try {
    const details = await api.getChargeDetails(charge.id);
    selectedChargeDetails.value = { ...charge, details };

    const timestamps = details.map((d: any) => new Date(d.timestamp).toLocaleTimeString());
    const soc = details.map((d: any) => d.soc);
    const chargePower = details.map((d: any) => d.charge_power);
    const batteryTemp = details.map((d: any) => d.battery_temp);
    const batteryVoltage = details.map((d: any) => d.battery_voltage);
    const chargeVoltage = details.map((d: any) => d.charge_voltage);

    socChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '电量 (%)' },
      series: [{ name: '电量', type: 'line', data: soc }],
    };

    chargePowerChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '充电功率 (kW)' },
      series: [{ name: '充电功率', type: 'line', data: chargePower }],
    };

    batteryTempChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '电池温度 (°C)' },
      series: [{ name: '电池温度', type: 'line', data: batteryTemp }],
    };

    batteryVoltageChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '电池电压 (V)' },
      series: [{ name: '电池电压', type: 'line', data: batteryVoltage }],
    };

    chargeVoltageChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: timestamps },
      yAxis: { type: 'value', name: '充电电压 (V)' },
      series: [{ name: '充电电压', type: 'line', data: chargeVoltage }],
    };

  } catch (error) {
    console.error('获取充电详情失败:', error);
    selectedChargeDetails.value = null;
  }
};

onMounted(() => {
  fetchCharges();
});
</script>

<style scoped>
.charge-view {
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