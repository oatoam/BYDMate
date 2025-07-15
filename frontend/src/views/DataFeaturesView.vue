<template>
  <div class="data-features-view">
    <h1>数据特性页面</h1>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>指定区域/路段的平均速度</span>
        </div>
      </template>
      <DataTable :data="avgSpeedData" :columns="avgSpeedColumns" :pagination="false" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../api';
import DataTable from '../components/DataTable.vue';
import { ElCard } from 'element-plus';

const avgSpeedData = ref<any[]>([]);

const avgSpeedColumns = [
  { prop: 'feature_type', label: '特性类型' },
  { prop: 'feature_value', label: '平均速度 (km/h)' },
  { prop: 'area_info.name', label: '区域/路段', formatter: (row: any) => row.area_info?.name || 'N/A' },
];

const fetchDataFeatures = async () => {
  try {
    const response = await api.getDataFeatures({ feature_type: 'avg_speed_in_area' });
    avgSpeedData.value = response;
  } catch (error) {
    console.error('获取数据特性失败:', error);
  }
};

onMounted(() => {
  fetchDataFeatures();
});
</script>

<style scoped>
.data-features-view {
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