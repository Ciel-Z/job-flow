<template>
  <div class="monitor-container">
    <div class="page-header">
      <div>
        <h2>实时监控</h2>
        <p class="page-desc">WebSocket实时事件监控</p>
      </div>
      <el-button :type="wsConnected ? 'danger' : 'success'" @click="toggleWebSocket">
        <el-icon><component :is="wsConnected ? 'Close' : 'Connection'" /></el-icon>
        {{ wsConnected ? '断开连接' : '连接WebSocket' }}
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <el-icon><Connection /></el-icon>
              <span>连接状态</span>
            </div>
          </template>
          <div class="status-content">
            <div class="status-indicator" :class="{ 'connected': wsConnected }">
              {{ wsConnected ? '已连接' : '未连接' }}
            </div>
            <div class="status-info">
              <p>WebSocket URL: {{ wsUrl }}</p>
              <p>消息数量: {{ eventCount }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="events-card">
          <template #header>
            <div class="card-header">
              <div>
                <el-icon><Notification /></el-icon>
                <span>实时事件</span>
              </div>
              <el-button size="small" @click="clearEvents">清空</el-button>
            </div>
          </template>
          <div class="events-content">
            <div v-if="events.length === 0" class="empty-events">
              <el-icon :size="48"><Box /></el-icon>
              <p>暂无事件</p>
            </div>
            <div v-else class="events-list">
              <div 
                v-for="(event, index) in events" 
                :key="index"
                class="event-item"
              >
                <div class="event-time">{{ event.time }}</div>
                <div class="event-content">{{ event.message }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 16px">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon><DataAnalysis /></el-icon>
              <span>系统统计</span>
            </div>
          </template>
          <div class="chart-content">
            <el-row :gutter="24">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ stats.totalJobs }}</div>
                  <div class="stat-label">总任务数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ stats.runningJobs }}</div>
                  <div class="stat-label">运行中</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ stats.successJobs }}</div>
                  <div class="stat-label">成功</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ stats.failedJobs }}</div>
                  <div class="stat-label">失败</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Close, Notification, Box, DataAnalysis } from '@element-plus/icons-vue'

const wsConnected = ref(false)
const ws = ref(null)
const events = ref([])
const eventCount = ref(0)
const wsUrl = ref('')

const stats = ref({
  totalJobs: 0,
  runningJobs: 0,
  successJobs: 0,
  failedJobs: 0
})

const toggleWebSocket = () => {
  if (wsConnected.value) {
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  wsUrl.value = `${protocol}//${window.location.host}/ws/job`
  
  try {
    ws.value = new WebSocket(wsUrl.value)
    
    ws.value.onopen = () => {
      wsConnected.value = true
      addEvent('WebSocket连接成功')
      ElMessage.success('WebSocket连接成功')
    }
    
    ws.value.onmessage = (event) => {
      addEvent(`收到消息: ${event.data}`)
      eventCount.value++
      
      // Try to parse as JSON for stats
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'stats') {
          stats.value = data.data
        }
      } catch (e) {
        // Not JSON, just display as text
      }
    }
    
    ws.value.onerror = (error) => {
      console.error('WebSocket error:', error)
      addEvent('WebSocket连接错误')
      ElMessage.error('WebSocket连接错误')
    }
    
    ws.value.onclose = () => {
      wsConnected.value = false
      addEvent('WebSocket连接已关闭')
    }
  } catch (error) {
    ElMessage.error('无法建立WebSocket连接')
  }
}

const disconnectWebSocket = () => {
  if (ws.value) {
    ws.value.close()
    ws.value = null
    wsConnected.value = false
  }
}

const addEvent = (message) => {
  const now = new Date()
  const timeStr = now.toLocaleTimeString('zh-CN')
  
  events.value.unshift({
    time: timeStr,
    message: message
  })
  
  // Keep only last 50 events
  if (events.value.length > 50) {
    events.value.pop()
  }
}

const clearEvents = () => {
  events.value = []
  eventCount.value = 0
}

onMounted(() => {
  // Auto-connect on mount (optional)
  // connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.monitor-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  color: var(--n8n-text-primary);
  margin-bottom: 4px;
}

.page-desc {
  color: var(--n8n-text-secondary);
  font-size: 14px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 600;
}

.status-content {
  text-align: center;
}

.status-indicator {
  display: inline-block;
  padding: 12px 32px;
  border-radius: 8px;
  background-color: var(--n8n-error-color);
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.status-indicator.connected {
  background-color: var(--n8n-success-color);
}

.status-info {
  text-align: left;
  color: var(--n8n-text-secondary);
  font-size: 14px;
}

.status-info p {
  margin: 8px 0;
}

.events-content {
  min-height: 300px;
  max-height: 400px;
  overflow-y: auto;
}

.empty-events {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--n8n-text-secondary);
}

.events-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.event-item {
  padding: 12px;
  background-color: var(--n8n-bg-tertiary);
  border-left: 3px solid var(--n8n-accent-color);
  border-radius: 4px;
}

.event-time {
  color: var(--n8n-text-secondary);
  font-size: 12px;
  margin-bottom: 4px;
}

.event-content {
  color: var(--n8n-text-primary);
  font-size: 14px;
}

.chart-content {
  padding: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background-color: var(--n8n-bg-tertiary);
  border-radius: 8px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--n8n-accent-color);
  margin-bottom: 8px;
}

.stat-label {
  color: var(--n8n-text-secondary);
  font-size: 14px;
}
</style>
