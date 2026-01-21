<template>
  <div class="jobs-container">
    <div class="page-header">
      <div>
        <h2>任务管理</h2>
        <p class="page-desc">创建和管理调度任务</p>
      </div>
      <el-button type="primary" @click="showJobDialog = true">
        <el-icon><Plus /></el-icon>
        新建任务
      </el-button>
    </div>

    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="任务名称">
          <el-input v-model="searchForm.jobName" placeholder="请输入任务名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadJobs">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="jobId" label="任务ID" width="80" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" />
        <el-table-column prop="cron" label="Cron表达式" width="150" />
        <el-table-column prop="processorInfo" label="处理器" min-width="200" />
        <el-table-column prop="dispatchStrategy" label="调度策略" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getStrategyName(row.dispatchStrategy) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '运行中' : '已停止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row.jobId)">详情</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleJob(row.jobId)">
              {{ row.status === 1 ? '停止' : '启动' }}
            </el-button>
            <el-button size="small" type="primary" @click="startJob(row.jobId)">执行</el-button>
            <el-button size="small" type="danger" @click="deleteJob(row.jobId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Job Dialog -->
    <el-dialog v-model="showJobDialog" title="新建任务" width="600px">
      <el-form :model="jobForm" label-width="120px">
        <el-form-item label="任务名称" required>
          <el-input v-model="jobForm.jobName" />
        </el-form-item>
        <el-form-item label="Cron表达式">
          <el-input v-model="jobForm.cron" placeholder="例如: 0 0/5 * * * ?" />
        </el-form-item>
        <el-form-item label="处理器" required>
          <el-input v-model="jobForm.processorInfo" placeholder="com.worker.handle.DemoJobHandle" />
        </el-form-item>
        <el-form-item label="调度策略">
          <el-select v-model="jobForm.dispatchStrategy">
            <el-option label="轮询" :value="1" />
            <el-option label="随机" :value="2" />
            <el-option label="哈希" :value="3" />
            <el-option label="指定Worker" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Worker Tag">
          <el-input v-model="jobForm.tag" />
        </el-form-item>
        <el-form-item label="参数">
          <el-input v-model="jobForm.params" type="textarea" :rows="4" placeholder='{"key": "value"}' />
        </el-form-item>
        <el-form-item label="最大重试次数">
          <el-input-number v-model="jobForm.maxRetryTimes" :min="0" />
        </el-form-item>
        <el-form-item label="重试间隔(ms)">
          <el-input-number v-model="jobForm.retryInterval" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJobDialog = false">取消</el-button>
        <el-button type="primary" @click="saveJob">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { jobApi } from '@/api'

const loading = ref(false)
const showJobDialog = ref(false)
const tableData = ref([])
const searchForm = ref({
  jobName: ''
})

const jobForm = ref({
  jobName: '',
  cron: '',
  processorInfo: '',
  dispatchStrategy: 1,
  tag: '',
  params: '',
  maxRetryTimes: 0,
  retryInterval: 1000
})

const getStrategyName = (strategy) => {
  const map = { 1: '轮询', 2: '随机', 3: '哈希', 4: '指定' }
  return map[strategy] || '-'
}

const loadJobs = async () => {
  loading.value = true
  try {
    const result = await jobApi.list(searchForm.value)
    if (result.list) {
      tableData.value = result.list
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const saveJob = async () => {
  try {
    await jobApi.save(jobForm.value)
    ElMessage.success('保存成功')
    showJobDialog.value = false
    loadJobs()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const toggleJob = async (jobId) => {
  try {
    await jobApi.toggle(jobId)
    ElMessage.success('操作成功')
    loadJobs()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const startJob = async (jobId) => {
  try {
    await jobApi.start(jobId)
    ElMessage.success('任务已启动')
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const deleteJob = async (jobId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await jobApi.delete(jobId)
    ElMessage.success('删除成功')
    loadJobs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewDetail = async (jobId) => {
  try {
    const result = await jobApi.detail(jobId)
    if (result.data) {
      ElMessageBox.alert(JSON.stringify(result.data, null, 2), '任务详情')
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

onMounted(() => {
  loadJobs()
})
</script>

<style scoped>
.jobs-container {
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
</style>
