<template>
  <div class="project-list-container">
    <el-container>
      <el-header class="header">
        <h1>工业控制网络仿真平台</h1>
        <el-button type="primary" @click="showCreateDialog = true" :icon="Plus">
          新建项目
        </el-button>
      </el-header>
      
      <el-main>
        <div class="search-bar">
          <el-input
            v-model="searchKeywords"
            placeholder="搜索项目名称或描述"
            clearable
            @clear="loadProjects"
            @keyup.enter="loadProjects"
            style="width: 300px"
          >
            <template #append>
              <el-button :icon="Search" @click="loadProjects" />
            </template>
          </el-input>
        </div>

        <el-table
          :data="projects"
          style="width: 100%"
          v-loading="loading"
        >
          <el-table-column prop="projectId" label="ID" width="80" />
          <el-table-column prop="projectName" label="项目名称" width="200" />
          <el-table-column prop="simTimeLimit" label="仿真时长" width="120" />
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="info">未运行</el-tag>
              <el-tag v-else-if="row.status === 1" type="warning">运行中</el-tag>
              <el-tag v-else-if="row.status === 2" type="success">已完成</el-tag>
              <el-tag v-else type="danger">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="editTopology(row)">
                拓扑编辑
              </el-button>
              <el-button type="success" size="small" @click="viewResults(row)">
                查看结果
              </el-button>
              <el-button type="danger" size="small" @click="deleteProject(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="loadProjects"
          @size-change="loadProjects"
          style="margin-top: 20px; justify-content: center"
        />
      </el-main>
    </el-container>

    <!-- Create Project Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建项目" width="500px">
      <el-form :model="newProject" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="newProject.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="仿真时长">
          <el-input v-model="newProject.simTimeLimit" placeholder="例如: 10s" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newProject.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述"
          />
        </el-form-item>
        <el-form-item label="创建人">
          <el-input v-model="newProject.createUser" placeholder="请输入创建人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createProject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { projectApi } from '../api'

const router = useRouter()

const loading = ref(false)
const projects = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const searchKeywords = ref('')
const showCreateDialog = ref(false)
const newProject = ref({
  projectName: '',
  simTimeLimit: '10s',
  description: '',
  createUser: ''
})

const loadProjects = async () => {
  loading.value = true
  try {
    const response = await projectApi.list({
      page: currentPage.value,
      pageSize: pageSize.value,
      keywords: searchKeywords.value
    })
    projects.value = response.data.list
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('加载项目列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const createProject = async () => {
  if (!newProject.value.projectName) {
    ElMessage.warning('请输入项目名称')
    return
  }
  
  try {
    await projectApi.create(newProject.value)
    ElMessage.success('项目创建成功')
    showCreateDialog.value = false
    newProject.value = {
      projectName: '',
      simTimeLimit: '10s',
      description: '',
      createUser: ''
    }
    loadProjects()
  } catch (error) {
    ElMessage.error('创建项目失败: ' + error.message)
  }
}

const editTopology = (project) => {
  router.push(`/topology/${project.projectId}`)
}

const viewResults = (project) => {
  router.push(`/results/${project.projectId}`)
}

const deleteProject = async (project) => {
  try {
    await ElMessageBox.confirm('确定要删除该项目吗?', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await projectApi.delete(project.projectId)
    ElMessage.success('删除成功')
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped>
.project-list-container {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
}

.header h1 {
  font-size: 24px;
  color: #333;
}

.el-main {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
}
</style>
