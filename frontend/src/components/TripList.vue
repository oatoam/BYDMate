<template>
  <div class="trip-list">
    <h2>行程数据</h2>
    <div v-if="loading">加载中...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>开始地点</th>
            <th>结束地点</th>
            <th>平均速度 (km/h)</th>
            <th>平均功率 (kW)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="trip in trips" :key="trip.id">
            <td>{{ trip.id }}</td>
            <td>{{ new Date(trip.start_time).toLocaleString() }}</td>
            <td>{{ new Date(trip.end_time).toLocaleString() }}</td>
            <td>{{ trip.start_location }}</td>
            <td>{{ trip.end_location }}</td>
            <td>{{ trip.avg_speed.toFixed(2) }}</td>
            <td>{{ trip.avg_power.toFixed(2) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

interface Trip {
  id: number;
  start_time: string;
  end_time: string;
  start_location: string;
  end_location: string;
  avg_speed: number;
  avg_power: number;
}

const trips = ref<Trip[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const fetchTrips = async () => {
  try {
    const response = await axios.get<Trip[]>('http://localhost:8081/api/trips');
    trips.value = response.data;
  } catch (err) {
    if (axios.isAxiosError(err)) {
      error.value = 'Error fetching trips: ' + (err.response?.data || err.message);
    } else {
      error.value = 'An unexpected error occurred: ' + (err as Error).message;
    }
    console.error(err);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchTrips);
</script>

<style scoped>
.trip-list {
  padding: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

th, td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

th {
  background-color: #f2f2f2;
}
</style>