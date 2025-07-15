<template>
  <div class="track-stats-view">
    <h1>轨迹统计页面</h1>

    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>位置统计地图</span>
        </div>
      </template>
      <MapView :markers="locationMarkers" />
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <span>常去TOP10目的地</span>
          </template>
          <ul class="location-list">
            <li v-for="loc in topDestinations" :key="loc.name">
              {{ loc.name }} ({{ loc.count }} 次)
            </li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <span>常停留TOP10位置</span>
          </template>
          <ul class="location-list">
            <li v-for="loc in topStayLocations" :key="loc.name">
              {{ loc.name }} ({{ loc.count }} 次)
            </li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <span>常充电位置</span>
          </template>
          <ul class="location-list">
            <li v-for="loc in chargeLocations" :key="loc.name">
              {{ loc.name }} ({{ loc.count }} 次)
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '../api';
import MapView from '../components/MapView.vue';
import { ElCard, ElRow, ElCol } from 'element-plus';

const locationStats = ref<any[]>([]);

const locationMarkers = computed(() => {
  return locationStats.value.map(loc => ({
    position: [loc.longitude, loc.latitude],
    content: `<div>${loc.name} (${loc.count}次)</div>`,
  }));
});

const topDestinations = computed(() => {
  return locationStats.value.filter(loc => loc.location_type === 'top_destination');
});

const topStayLocations = computed(() => {
  return locationStats.value.filter(loc => loc.location_type === 'top_stay');
});

const chargeLocations = computed(() => {
  return locationStats.value.filter(loc => loc.location_type === 'charge_location');
});

const fetchLocationStats = async () => {
  try {
    const response = await api.getLocationStats();
    locationStats.value = response;
  } catch (error) {
    console.error('获取轨迹统计数据失败:', error);
  }
};

onMounted(() => {
  fetchLocationStats();
});
</script>

<style scoped>
.track-stats-view {
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

.location-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.location-list li {
  padding: 5px 0;
  border-bottom: 1px solid #eee;
}

.location-list li:last-child {
  border-bottom: none;
}
</style>