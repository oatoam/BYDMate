<template>
  <div ref="chartContainer" class="line-chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, defineProps } from 'vue';
import * as echarts from 'echarts';

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
});

const chartContainer = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value);
    chartInstance.setOption(props.option);
  }
};

const resizeChart = () => {
  chartInstance?.resize();
};

watch(
  () => props.option,
  (newOption) => {
    if (chartInstance) {
      chartInstance.setOption(newOption);
    }
  },
  { deep: true }
);

onMounted(() => {
  initChart();
  window.addEventListener('resize', resizeChart);
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart);
  chartInstance?.dispose();
});
</script>

<style scoped>
.line-chart-container {
  width: 100%;
  height: 300px; /* 默认高度，可以在使用时覆盖 */
}
</style>