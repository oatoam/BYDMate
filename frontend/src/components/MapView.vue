<template>
  <div ref="mapContainer" class="map-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, defineProps } from 'vue';

const props = defineProps({
  center: {
    type: Array,
    default: () => [116.397428, 39.90923], // 默认北京天安门
  },
  zoom: {
    type: Number,
    default: 10,
  },
  markers: {
    type: Array,
    default: () => [], // [{ position: [lng, lat], content: 'marker info' }]
  },
  paths: {
    type: Array,
    default: () => [], // [[lng, lat], [lng, lat], ...]
  },
  pathColors: {
    type: Array,
    default: () => [], // ['#FF0000', '#0000FF', ...]
  },
});

const mapContainer = ref<HTMLElement | null>(null);
let map: AMap.Map | null = null;
const polyline: AMap.Polyline[] = [];
const markers: AMap.Marker[] = [];

const initMap = () => {
  if (mapContainer.value && window.AMap) {
    map = new window.AMap.Map(mapContainer.value, {
      center: props.center as [number, number],
      zoom: props.zoom,
    });
    updateMarkers();
    updatePaths();
  }
};

const updateMarkers = () => {
  markers.forEach(m => m.setMap(null)); // 清除旧标记
  markers.length = 0; // 清空数组
  props.markers.forEach((markerData: any) => {
    const marker = new window.AMap.Marker({
      position: markerData.position as [number, number],
      map: map,
      content: markerData.content,
    });
    markers.push(marker);
  });
};

const updatePaths = () => {
  polyline.forEach(p => p.setMap(null)); // 清除旧路径
  polyline.length = 0; // 清空数组
  if (props.paths.length > 0) {
    // 如果有路径颜色，则按颜色分段绘制
    if (props.pathColors.length > 0 && props.paths.length - 1 === props.pathColors.length) {
      for (let i = 0; i < props.paths.length - 1; i++) {
        const segment = new window.AMap.Polyline({
          path: [props.paths[i] as [number, number], props.paths[i + 1] as [number, number]],
          strokeColor: props.pathColors[i],
          strokeWeight: 6,
          map: map,
        });
        polyline.push(segment);
      }
    } else {
      // 否则绘制单色路径
      const singlePolyline = new window.AMap.Polyline({
        path: props.paths as AMap.LngLat[],
        strokeColor: '#0000FF', // 默认蓝色
        strokeWeight: 6,
        map: map,
      });
      polyline.push(singlePolyline);
    }
    map?.setFitView(); // 调整地图视野以适应路径
  }
};

watch(
  () => props.markers,
  () => {
    updateMarkers();
  },
  { deep: true }
);

watch(
  () => props.paths,
  () => {
    updatePaths();
  },
  { deep: true }
);

watch(
  () => props.pathColors,
  () => {
    updatePaths();
  },
  { deep: true }
);

onMounted(() => {
  initMap();
});

onUnmounted(() => {
  map?.destroy();
});
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 400px; /* 默认高度，可以在使用时覆盖 */
}
</style>