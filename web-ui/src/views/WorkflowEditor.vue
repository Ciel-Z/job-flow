<template>
  <div class="workflow-editor">
    <div class="editor-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <el-input 
        v-model="workflowName" 
        placeholder="工作流名称" 
        style="width: 300px; margin: 0 20px;"
      />
      <div class="header-actions">
        <el-button @click="saveWorkflow" type="primary">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
        <el-button @click="executeWorkflow" type="success">
          <el-icon><VideoPlay /></el-icon>
          执行
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <!-- Node Palette -->
      <div class="node-palette">
        <div class="palette-header">
          <el-icon><Grid /></el-icon>
          <span>节点面板</span>
        </div>
        <div class="palette-search">
          <el-input 
            v-model="nodeSearch" 
            placeholder="搜索节点..." 
            size="small"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="palette-nodes">
          <div 
            v-for="node in filteredNodes" 
            :key="node.type"
            class="palette-node"
            draggable="true"
            @dragstart="onNodeDragStart($event, node)"
          >
            <el-icon :size="20">
              <component :is="node.icon" />
            </el-icon>
            <span>{{ node.label }}</span>
          </div>
        </div>
      </div>

      <!-- Workflow Canvas -->
      <div class="workflow-canvas">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :default-viewport="{ zoom: 1 }"
          :min-zoom="0.2"
          :max-zoom="4"
          @drop="onDrop"
          @dragover="onDragOver"
          @node-click="onNodeClick"
          @edge-click="onEdgeClick"
          @connect="onConnect"
          :connect-on-click="true"
          class="vue-flow-container"
        >
          <Background pattern-color="#404040" :gap="16" />
          <Controls />
          <MiniMap />
          
          <template #node-custom="{ data }">
            <div class="custom-node" :class="{ 'node-selected': data.selected }">
              <div class="node-header" :style="{ backgroundColor: data.color || '#667eea' }">
                <el-icon :size="16"><component :is="data.icon || 'Operation'" /></el-icon>
                <span class="node-title">{{ data.label }}</span>
              </div>
              <div class="node-body">
                <div class="node-info">
                  <span class="info-label">任务ID:</span>
                  <span>{{ data.jobId || '未设置' }}</span>
                </div>
              </div>
            </div>
          </template>
        </VueFlow>
      </div>

      <!-- Node Properties Panel -->
      <div class="properties-panel" v-if="selectedNode">
        <div class="panel-header">
          <el-icon><Setting /></el-icon>
          <span>节点属性</span>
          <el-button size="small" text @click="selectedNode = null">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-content">
          <el-form label-width="80px" size="small">
            <el-form-item label="节点名称">
              <el-input v-model="selectedNode.data.label" />
            </el-form-item>
            <el-form-item label="任务ID">
              <el-input-number v-model="selectedNode.data.jobId" :min="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="节点颜色">
              <el-color-picker v-model="selectedNode.data.color" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="selectedNode.data.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" size="small" @click="deleteNode">删除节点</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, Check, VideoPlay, Grid, Search, Setting, Close,
  Operation, DataAnalysis, Files, Document, Clock
} from '@element-plus/icons-vue'
import { workflowApi } from '@/api'

const router = useRouter()
const route = useRoute()
const { addNodes, addEdges, removeNodes, removeEdges, toObject } = useVueFlow()

const workflowId = ref(route.params.id)
const workflowName = ref('新建工作流')
const nodeSearch = ref('')
const selectedNode = ref(null)
const selectedEdge = ref(null)

const nodes = ref([])
const edges = ref([])

let nodeIdCounter = 0

const availableNodes = ref([
  { type: 'custom', label: '数据处理', icon: 'DataAnalysis', color: '#667eea' },
  { type: 'custom', label: '文件操作', icon: 'Files', color: '#764ba2' },
  { type: 'custom', label: '定时任务', icon: 'Clock', color: '#ff6d5a' },
  { type: 'custom', label: '脚本执行', icon: 'Document', color: '#4caf50' },
  { type: 'custom', label: '通用任务', icon: 'Operation', color: '#2196f3' }
])

const filteredNodes = computed(() => {
  if (!nodeSearch.value) return availableNodes.value
  return availableNodes.value.filter(node => 
    node.label.toLowerCase().includes(nodeSearch.value.toLowerCase())
  )
})

const onNodeDragStart = (event, node) => {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('application/vueflow', JSON.stringify(node))
}

const onDragOver = (event) => {
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
}

const onDrop = (event) => {
  const { left, top } = event.target.getBoundingClientRect()
  const nodeData = JSON.parse(event.dataTransfer.getData('application/vueflow'))
  
  const newNode = {
    id: `node_${++nodeIdCounter}`,
    type: 'custom',
    position: {
      x: event.clientX - left,
      y: event.clientY - top
    },
    data: {
      label: nodeData.label,
      icon: nodeData.icon,
      color: nodeData.color,
      jobId: null,
      description: ''
    }
  }
  
  addNodes([newNode])
}

const onNodeClick = (event) => {
  selectedNode.value = event.node
  selectedEdge.value = null
}

const onEdgeClick = (event) => {
  selectedEdge.value = event.edge
  selectedNode.value = null
}

const onConnect = (params) => {
  // Add edge when nodes are connected
  addEdges([{
    id: `edge_${Date.now()}`,
    source: params.source,
    target: params.target,
    animated: true
  }])
}

const deleteNode = () => {
  if (selectedNode.value) {
    removeNodes([selectedNode.value.id])
    selectedNode.value = null
  }
}

const goBack = () => {
  router.push('/workflows')
}

const saveWorkflow = async () => {
  try {
    const flowObject = toObject()
    
    const dag = {
      nodes: flowObject.nodes.map((node, index) => ({
        id: index + 1,
        jobId: node.data.jobId,
        name: node.data.label,
        position: node.position
      })),
      edges: flowObject.edges.map(edge => ({
        from: parseInt(edge.source.split('_')[1]),
        to: parseInt(edge.target.split('_')[1])
      }))
    }
    
    const data = {
      name: workflowName.value,
      dag: JSON.stringify(dag),
      params: '{}'
    }
    
    if (workflowId.value) {
      data.flowId = workflowId.value
    }
    
    await workflowApi.save(data)
    ElMessage.success('保存成功')
    router.push('/workflows')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const executeWorkflow = async () => {
  if (!workflowId.value) {
    ElMessage.warning('请先保存工作流')
    return
  }
  
  try {
    const result = await workflowApi.start(workflowId.value)
    ElMessage.success(`工作流已启动，实例ID: ${result.data}`)
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const loadWorkflow = async () => {
  if (!workflowId.value) return
  
  try {
    const result = await workflowApi.detail(workflowId.value)
    if (result.data) {
      workflowName.value = result.data.name
      
      if (result.data.dag) {
        const dag = JSON.parse(result.data.dag)
        
        nodes.value = dag.nodes.map(node => ({
          id: `node_${node.id}`,
          type: 'custom',
          position: node.position || { x: 100, y: 100 },
          data: {
            label: node.name,
            jobId: node.jobId,
            icon: 'Operation',
            color: '#667eea'
          }
        }))
        
        edges.value = dag.edges.map((edge, index) => ({
          id: `edge_${index}`,
          source: `node_${edge.from}`,
          target: `node_${edge.to}`,
          animated: true
        }))
        
        nodeIdCounter = Math.max(...dag.nodes.map(n => n.id))
      }
    }
  } catch (error) {
    ElMessage.error('加载工作流失败')
  }
}

onMounted(() => {
  if (workflowId.value) {
    loadWorkflow()
  }
})
</script>

<style scoped>
.workflow-editor {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background-color: var(--n8n-bg-primary);
}

.editor-header {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  background-color: var(--n8n-bg-secondary);
  border-bottom: 1px solid var(--n8n-border-color);
}

.header-actions {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

.editor-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.node-palette {
  width: 260px;
  background-color: var(--n8n-bg-secondary);
  border-right: 1px solid var(--n8n-border-color);
  display: flex;
  flex-direction: column;
}

.palette-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--n8n-border-color);
  color: var(--n8n-text-primary);
  font-weight: 600;
}

.palette-search {
  padding: 12px;
}

.palette-nodes {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.palette-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background-color: var(--n8n-bg-tertiary);
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  color: var(--n8n-text-primary);
}

.palette-node:hover {
  background-color: #404040;
  transform: translateX(4px);
}

.palette-node:active {
  cursor: grabbing;
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

.custom-node:hover,
.custom-node.node-selected {
  border-color: var(--n8n-accent-color);
  box-shadow: 0 4px 12px rgba(255, 109, 90, 0.3);
}

.node-header {
  padding: 10px 12px;
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.node-title {
  font-size: 13px;
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
}

.info-label {
  font-weight: 600;
  margin-right: 8px;
}

.properties-panel {
  width: 300px;
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
  padding: 16px;
}
</style>
