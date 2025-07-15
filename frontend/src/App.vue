<template>
  <el-container class="common-layout">
    <el-header class="app-header">
      <div class="logo-container">
        <img alt="Vue logo" class="logo" src="@/assets/logo.svg" width="50" height="50" />
        <span>BYDMate</span>
      </div>
      <el-menu
        :default-active="$route.path"
        class="el-menu-demo"
        mode="horizontal"
        :router="true"
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/dashboard">主看板</el-menu-item>
        <el-menu-item index="/trips">行程列表</el-menu-item>
        <el-menu-item index="/charge">充电记录</el-menu-item>
        <el-menu-item index="/driving-stats">驾驶统计</el-menu-item>
        <el-menu-item index="/track-stats">轨迹统计</el-menu-item>
        <el-menu-item index="/data-features">数据特性</el-menu-item>
        <el-menu-item index="/about">关于</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped>
.common-layout {
  min-height: 100vh;
}

.app-header {
  background-color: #545c64;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo {
  margin-right: 10px;
}

.app-header span {
  font-size: 20px;
  font-weight: bold;
}

.el-menu-demo {
  border-bottom: none;
}

.app-main {
  padding: 20px;
  background-color: #f0f2f5;
  flex-grow: 1;
}
</style>
