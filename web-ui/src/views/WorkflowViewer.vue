<template>
  <div class="workflow-viewer">
    <div class="viewer-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="workflow-info">
        <h3>{{ workflowName }}</h3>
        <el-tag :type="getStatusType(instanceStatus)" size="large">
          {{ getStatusName(instanceStatus) }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button 
          v-if="instanceStatus === 4" 
          @click="retryWorkflow" 
          type="warning"
        >
          <el-icon><RefreshRight /></el-icon>
          重试
        </el-button>
        <el-button 
          v-if="instanceStatus === 1" 
          @click="stopWorkflow" 
          type="danger"
        >
          <el-icon><VideoPause /></el-icon>
          停止
        </el-button>
        <el-button 
          :type="wsConnected ? 'info' : 'success'" 
          @click="toggleWebSocket"
        >
          <el-icon><component :is="wsConnected ? 'Close' : 'Connection'" /></el-icon>
          {{ wsConnected ? '断开监控' : '连接监控' }}
        </el-button>
      </div>
    </div>

    <div class="viewer-content">
      <!-- Workflow Canvas (Read-only) -->
      <div class="workflow-canvas">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :default-viewport="{ zoom: 1 }"
          :min-zoom="0.2"
          :max-zoom="4"
          :nodes-draggable="false"
          :nodes-connectable="false"
          :elements-selectable="true"
          class="vue-flow-container"
        >
          <Background pattern-color="#404040" :gap="16" />
          <Controls />
          <MiniMap />
          
          <template #node-custom="{ data }">
            <div 
              class="custom-node" 
              :class="{ 
                'node-selected': data.selected,
                'node-running': data.status === 1,
                'node-success': data.status === 3,
                'node-failed': data.status === 2,
                'node-waiting': data.status === 0
              }"
            >
              <div class="node-header" :style="{ backgroundColor: getNodeColor(data) }">
                <el-icon :size="16"><component :is="data.icon || 'Operation'" /></el-icon>
                <span class="node-title">{{ data.label }}</span>
                <el-icon v-if="data.status === 1" class="status-icon spinning">
                  <Loading />
                </el-icon>
                <el-icon v-else-if="data.status === 3" class="status-icon">
                  <Select />
                </el-icon>
                <el-icon v-else-if="data.status === 2" class="status-icon">
                  <Close />
                </el-icon>
              </div>
              <div class="node-body">
                <div class="node-info">
                  <span class="info-label">任务ID:</span>
                  <span>{{ data.jobId || '未设置' }}</span>
                </div>
                <div class="node-info" v-if="data.status !== undefined">
                  <span class="info-label">状态:</span>
                  <span>{{ getNodeStatusName(data.status) }}</span>
                </div>
              </div>
            </div>
          </template>
        </VueFlow>
      </div>

      <!-- Event Log Panel -->
      <div class="event-panel">
        <div class="panel-header">
          <el-icon><Notification /></el-icon>
          <span>执行日志</span>
          <el-button size="small" @click="clearEvents">清空</el-button>
        </div>
        <div class="panel-content">
          <div v-if="events.length === 0" class="empty-events">
            <el-icon :size="48"><Box /></el-icon>
            <p>暂无执行日志</p>
          </div>
          <div v-else class="events-list">
            <div 
              v-for="(event, index) in events" 
              :key="index"
              class="event-item"
              :class="`event-${event.level}`"
            >
              <div class="event-time">{{ event.time }}</div>
              <div class="event-content">
                <span class="event-node" v-if="event.nodeId">[节点{{ event.nodeId }}]</span>
                {{ event.message }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, RefreshRight, VideoPause, Connection, Close,
  Operation, Notification, Box, Loading, Select
} from '@element-plus/icons-vue'
import { workflowApi } from '@/api'

const router = useRouter()
const route = useRoute()

const instanceId = ref(route.params.instanceId)
const workflowName = ref('工作流执行')
const instanceStatus = ref(0)
const wsConnected = ref(false)
const ws = ref(null)
const events = ref([])

const nodes = ref([])
const edges = ref([])

const getStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'danger',
    3: 'success',
    4: 'info'
  }
  return map[status] || ''
}

const getStatusName = (status) => {
  const map = {
    0: '等待执行',
    1: '执行中',
    2: '失败',
    3: '成功',
    4: '已暂停'
  }
  return map[status] || '未知'
}

const getNodeStatusName = (status) => {
  const map = {
    0: '等待',
    1: '运行中',
    2: '失败',
    3: '成功'
  }
  return map[status] || '-'
}

const getNodeColor = (data) => {
  if (data.status === 1) return '#2196f3' // Running - blue
  if (data.status === 3) return '#4caf50' // Success - green
  if (data.status === 2) return '#f44336' // Failed - red
  return data.color || '#667eea' // Default
}

const goBack = () => {
  router.push('/workflows')
}

const retryWorkflow = async () => {
  // TODO: Implement retry from specific node
  ElMessage.info('请在节点上右键选择重试起点')
}

const stopWorkflow = async () => {
  try {
    await workflowApi.stop(instanceId.value)
    ElMessage.success('工作流已停止')
    loadWorkflowProgress()
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

const toggleWebSocket = () => {
  if (wsConnected.value) {
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/job`
  
  try {
    ws.value = new WebSocket(wsUrl)
    
    ws.value.onopen = () => {
      wsConnected.value = true
      addEvent('info', '实时监控已连接')
      // Subscribe to this workflow instance
      ws.value.send(JSON.stringify({
        type: 'subscribe',
        instanceId: instanceId.value
      }))
    }
    
    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleWebSocketMessage(data)
      } catch (e) {
        addEvent('info', event.data)
      }
    }
    
    ws.value.onerror = (error) => {
      console.error('WebSocket error:', error)
      addEvent('error', 'WebSocket连接错误')
    }
    
    ws.value.onclose = () => {
      wsConnected.value = false
      addEvent('info', '实时监控已断开')
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

const handleWebSocketMessage = (data) => {
  if (data.type === 'nodeStatus') {
    // Update node status
    const node = nodes.value.find(n => n.id === `node_${data.nodeId}`)
    if (node) {
      node.data.status = data.status
    }
    addEvent(data.status === 2 ? 'error' : 'info', data.message, data.nodeId)
  } else if (data.type === 'flowStatus') {
    instanceStatus.value = data.status
    addEvent('info', data.message)
  } else if (data.type === 'progress') {
    // Refresh progress
    loadWorkflowProgress()
  }
}

const addEvent = (level, message, nodeId = null) => {
  const now = new Date()
  const timeStr = now.toLocaleTimeString('zh-CN')
  
  events.value.unshift({
    time: timeStr,
    level: level,
    message: message,
    nodeId: nodeId
  })
  
  // Keep only last 100 events
  if (events.value.length > 100) {
    events.value.pop()
  }
}

const clearEvents = () => {
  events.value = []
}

const loadWorkflowProgress = async () => {
  try {
    const result = await workflowApi.progress(instanceId.value)
    if (result.data) {
      const flowData = result.data
      workflowName.value = flowData.name || '工作流执行'
      instanceStatus.value = flowData.status || 0
      
      // Parse DAG
      if (flowData.dag) {
        const dag = JSON.parse(flowData.dag)
        
        // Load nodes with status
        nodes.value = dag.nodes.map(node => ({
          id: `node_${node.id}`,
          type: 'custom',
          position: node.position || { x: 100 + node.id * 200, y: 100 },
          data: {
            label: node.name,
            jobId: node.jobId,
            icon: 'Operation',
            color: '#667eea',
            status: node.status // 0: waiting, 1: running, 2: failed, 3: success
          }
        }))
        
        // Load edges
        edges.value = dag.edges.map((edge, index) => ({
          id: `edge_${index}`,
          source: `node_${edge.from}`,
          target: `node_${edge.to}`,
          animated: true
        }))
      }
    }
  } catch (error) {
    console.error('加载工作流进度失败:', error)
    ElMessage.error('加载工作流进度失败')
  }
}

onMounted(() => {
  loadWorkflowProgress()
  // Auto-connect to WebSocket
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped>
.workflow-viewer {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background-color: var(--n8n-bg-primary);
}

.viewer-header {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  background-color: var(--n8n-bg-secondary);
  border-bottom: 1px solid var(--n8n-border-color);
  gap: 16px;
}

.workflow-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.workflow-info h3 {
  color: var(--n8n-text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.viewer-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.workflow-canvas {
  flex: 1;
  position: relative;
}

.vue-flow-container {
  background-color: var(--n8n-bg-primary);
}

.custom-node {
  min-width: 180px;
  background: var(--n8n-bg-secondary);
  border: 2px solid var(--n8n-border-color);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}

.custom-node.node-running {
  border-color: #2196f3;
  box-shadow: 0 0 20px rgba(33, 150, 243, 0.5);
}

.custom-node.node-success {
  border-color: #4caf50;
}

.custom-node.node-failed {
  border-color: #f44336;
  box-shadow: 0 0 20px rgba(244, 67, 54, 0.5);
}

.custom-node.node-waiting {
  opacity: 0.7;
}

.node-header {
  padding: 10px 12px;
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  position: relative;
}

.node-title {
  font-size: 13px;
  flex: 1;
}

.status-icon {
  margin-left: auto;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.node-body {
  padding: 12px;
  background: var(--n8n-bg-tertiary);
}

.node-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--n8n-text-secondary);
  margin-bottom: 4px;
}

.node-info:last-child {
  margin-bottom: 0;
}

.info-label {
  font-weight: 600;
  margin-right: 8px;
}

.event-panel {
  width: 350px;
  background-color: var(--n8n-bg-secondary);
  border-left: 1px solid var(--n8n-border-color);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--n8n-border-color);
  color: var(--n8n-text-primary);
  font-weight: 600;
}

.panel-header .el-button {
  margin-left: auto;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
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
  padding: 10px;
  background-color: var(--n8n-bg-tertiary);
  border-left: 3px solid var(--n8n-info-color);
  border-radius: 4px;
}

.event-item.event-error {
  border-left-color: var(--n8n-error-color);
}

.event-item.event-warning {
  border-left-color: var(--n8n-warning-color);
}

.event-item.event-success {
  border-left-color: var(--n8n-success-color);
}

.event-time {
  color: var(--n8n-text-secondary);
  font-size: 11px;
  margin-bottom: 4px;
}

.event-content {
  color: var(--n8n-text-primary);
  font-size: 13px;
  line-height: 1.4;
}

.event-node {
  color: var(--n8n-accent-color);
  font-weight: 600;
  margin-right: 4px;
}
</style>
