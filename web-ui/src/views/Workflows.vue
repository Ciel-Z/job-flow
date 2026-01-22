<template>
  <div class="workflows-container">
    <div class="page-header">
      <div>
        <h2>工作流管理</h2>
        <p class="page-desc">创建和管理DAG工作流</p>
      </div>
      <el-button type="primary" @click="createWorkflow">
        <el-icon><Plus /></el-icon>
        新建工作流
      </el-button>
    </div>

    <el-card class="search-card">
      <el-form :inline="true">
        <el-form-item label="工作流名称">
          <el-input v-model="searchForm.name" placeholder="请输入工作流名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadWorkflows">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="flowId" label="工作流ID" width="100" />
        <el-table-column prop="name" label="工作流名称" min-width="200" />
        <el-table-column prop="createdDate" label="创建时间" width="180" />
        <el-table-column prop="updatedDate" label="更新时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editWorkflow(row.flowId)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="primary" @click="viewWorkflow(row.flowId)">
              <el-icon><View /></el-icon>
              查看执行
            </el-button>
            <el-button size="small" type="success" @click="startWorkflow(row.flowId)">
              <el-icon><VideoPlay /></el-icon>
              启动
            </el-button>
            <el-button size="small" type="danger" @click="deleteWorkflow(row.flowId)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, VideoPlay, Delete, View } from '@element-plus/icons-vue'
import { workflowApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const searchForm = ref({
  name: ''
})

const loadWorkflows = async () => {
  loading.value = true
  try {
    const result = await workflowApi.list(searchForm.value)
    if (result.list) {
      tableData.value = result.list
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const createWorkflow = () => {
  router.push('/workflow/edit')
}

const editWorkflow = (flowId) => {
  router.push(`/workflow/edit/${flowId}`)
}

const startWorkflow = async (flowId) => {
  try {
    const result = await workflowApi.start(flowId)
    const instanceId = result.data
    ElMessage.success(`工作流已启动，实例ID: ${instanceId}`)
    // Navigate to viewer to watch execution
    router.push(`/workflow/view/${instanceId}`)
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

const viewWorkflow = (flowId) => {
  // For viewing, we need to get the latest instance
  // For now, just navigate to a placeholder or show a dialog to select instance
  ElMessage.info('请先启动工作流以查看执行情况')
}

const deleteWorkflow = async (flowId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个工作流吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await workflowApi.delete(flowId)
    ElMessage.success('删除成功')
    loadWorkflows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.workflows-container {
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
