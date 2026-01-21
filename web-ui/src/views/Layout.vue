<template>
  <el-container class="layout-container dark-theme">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon :size="32"><Cpu /></el-icon>
        <span class="logo-text">Job Flow</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/jobs">
          <el-icon><List /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
        <el-menu-item index="/workflows">
          <el-icon><Share /></el-icon>
          <span>工作流</span>
        </el-menu-item>
        <el-menu-item index="/instances">
          <el-icon><Operation /></el-icon>
          <span>执行实例</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon>
          <span>实时监控</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-icon :size="20"><Bell /></el-icon>
          <el-icon :size="20"><Setting /></el-icon>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Cpu, List, Share, Operation, Monitor, Bell, Setting } from '@element-plus/icons-vue'

const route = useRoute()

const currentRoute = computed(() => route.path)

const currentPageTitle = computed(() => {
  const titles = {
    '/jobs': '任务管理',
    '/workflows': '工作流管理',
    '/workflow/edit': '工作流编辑器',
    '/instances': '执行实例',
    '/monitor': '实时监控'
  }
  
  for (const key in titles) {
    if (route.path.startsWith(key)) {
      return titles[key]
    }
  }
  return '首页'
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: var(--n8n-bg-secondary);
  border-right: 1px solid var(--n8n-border-color);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 16px;
  border-bottom: 1px solid var(--n8n-border-color);
  color: var(--n8n-accent-color);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
}

.sidebar-menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background-color: var(--n8n-bg-secondary);
  border-bottom: 1px solid var(--n8n-border-color);
  color: var(--n8n-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  color: var(--n8n-text-secondary);
  cursor: pointer;
}

.header-right .el-icon:hover {
  color: var(--n8n-text-primary);
}

.main-content {
  background-color: var(--n8n-bg-primary);
  overflow-y: auto;
}

.el-breadcrumb {
  font-size: 16px;
}
</style>
