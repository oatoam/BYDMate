<template>
  <el-table :data="data" style="width: 100%" v-loading="loading" @row-click="handleRowClick">
    <el-table-column
      v-for="column in columns"
      :key="column.prop"
      :prop="column.prop"
      :label="column.label"
      :width="column.width"
      :formatter="column.formatter"
    >
      <template v-if="column.slot" #default="scope">
        <slot :name="column.slot" :row="scope.row"></slot>
      </template>
    </el-table-column>
  </el-table>
  <el-pagination
    v-if="pagination"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
    :current-page="currentPage"
    :page-sizes="pageSizes"
    :page-size="pageSize"
    :layout="paginationLayout"
    :total="total"
  ></el-pagination>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, ref, watch, type PropType } from 'vue';
import { ElTable, ElTableColumn, ElPagination } from 'element-plus';

interface TableColumn {
  prop: string;
  label: string;
  width?: string | number;
  formatter?: (row: any, column: any, cellValue: any, index: number) => string;
  slot?: string;
}

const props = defineProps({
  data: {
    type: Array as PropType<any[]>,
    default: () => [],
  },
  columns: {
    type: Array as PropType<TableColumn[]>,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  pagination: {
    type: Boolean,
    default: false,
  },
  total: {
    type: Number,
    default: 0,
  },
  currentPage: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  pageSizes: {
    type: Array as PropType<number[]>,
    default: () => [10, 20, 50, 100],
  },
  paginationLayout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper',
  },
});

const emit = defineEmits(['size-change', 'current-change', 'row-click']);

const handleSizeChange = (val: number) => {
  emit('size-change', val);
};

const handleCurrentChange = (val: number) => {
  emit('current-change', val);
};

const handleRowClick = (row: any, column: any, event: Event) => {
  emit('row-click', row);
};

// 确保 Element Plus 已安装并正确配置
// 如果没有，需要通过 npm install element-plus 或 yarn add element-plus 安装
// 并在 main.ts 中引入：
// import ElementPlus from 'element-plus'
// import 'element-plus/dist/index.css'
// app.use(ElementPlus)
</script>

<style scoped>
.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>