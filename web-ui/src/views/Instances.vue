<template>
  <div class="instances-container">
    <div class="page-header">
      <div>
        <h2>执行实例</h2>
        <p class="page-desc">查看任务执行状态和日志</p>
      </div>
    </div>

    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="任务ID">
          <el-input-number v-model="searchForm.jobId" :min="0" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部">
            <el-option label="等待接收" :value="0" />
            <el-option label="运行中" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="成功" :value="3" />
            <el-option label="暂停" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadInstances">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="instanceId" label="实例ID" width="100" />
        <el-table-column prop="jobId" label="任务ID" width="100" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" />
        <el-table-column prop="workerAddress" label="Worker地址" min-width="180" />
        <el-table-column prop="triggerTime" label="触发时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewLog(row.instanceId)">
              <el-icon><Document /></el-icon>
              日志
            </el-button>
            <el-button 
              v-if="row.status === 1" 
              size="small" 
              type="danger" 
              @click="stopInstance(row)"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Log Dialog -->
    <el-dialog v-model="showLogDialog" title="任务日志" width="800px" top="5vh">
      <div class="log-container" v-loading="logLoading">
        <div v-if="logs.length === 0" class="empty-log">暂无日志</div>
        <div v-else class="log-content">
          <div 
            v-for="(log, index) in logs" 
            :key="index"
            class="log-item"
            :class="`log-level-${log.level}`"
          >
            <span class="log-time">[{{ log.timestamp }}]</span>
            <span class="log-text">{{ log.content }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document, VideoPause } from '@element-plus/icons-vue'
import { jobApi } from '@/api'

const loading = ref(false)
const logLoading = ref(false)
const showLogDialog = ref(false)
const tableData = ref([])
const logs = ref([])
const searchForm = ref({
  jobId: null,
  status: null
})

const getStatusName = (status) => {
  const map = {
    0: '等待接收',
    1: '运行中',
    2: '失败',
    3: '成功',
    4: '暂停'
  }
  return map[status] || '未知'
}

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

const loadInstances = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.value.jobId) params.jobId = searchForm.value.jobId
    if (searchForm.value.status !== null) params.status = searchForm.value.status
    
    const result = await jobApi.instanceList(params)
    if (result.list) {
      tableData.value = result.list
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const viewLog = async (instanceId) => {
  showLogDialog.value = true
  logLoading.value = true
  logs.value = []
  
  try {
    const result = await jobApi.log(instanceId)
    if (result.list) {
      logs.value = result.list
    }
  } catch (error) {
    ElMessage.error('加载日志失败')
  } finally {
    logLoading.value = false
  }
}

const stopInstance = async (instance) => {
  try {
    await ElMessageBox.confirm('确定要停止这个任务实例吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await jobApi.stop({
      jobId: instance.jobId,
      instanceId: instance.instanceId
    })
    
    ElMessage.success('停止成功')
    loadInstances()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停止失败')
    }
  }
}

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
.instances-container {
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

.search-card,
.table-card {
  margin-bottom: 16px;
}

.log-container {
  max-height: 600px;
  overflow-y: auto;
  background: #1a1a1a;
  border-radius: 8px;
  padding: 16px;
  font-family: 'Courier New', monospace;
}

.empty-log {
  text-align: center;
  color: #a0a0a0;
  padding: 40px;
}

.log-content {
  color: #e0e0e0;
}

.log-item {
  margin-bottom: 8px;
  line-height: 1.6;
}

.log-time {
  color: #808080;
  margin-right: 12px;
}

.log-level-0 {
  color: #63b3ed;
}

.log-level-1 {
  color: #f6ad55;
}

.log-level-2 {
  color: #fc8181;
}
</style>
