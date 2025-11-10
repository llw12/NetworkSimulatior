<template>
  <div class="results-container">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
          <h2>仿真结果 - {{ projectName }}</h2>
        </div>
        <div class="header-right">
          <el-button type="success" @click="exportResults('csv')">
            导出CSV
          </el-button>
          <el-button type="primary" @click="exportResults('json')">
            导出JSON
          </el-button>
        </div>
      </el-header>

      <el-main>
        <!-- Filters -->
        <el-card class="filter-card">
          <el-form :inline="true" :model="filters">
            <el-form-item label="指标类型">
              <el-select v-model="filters.metricType" clearable placeholder="全部">
                <el-option label="标量" value="scalar" />
                <el-option label="向量" value="vector" />
              </el-select>
            </el-form-item>
            <el-form-item label="指标名称">
              <el-input
                v-model="filters.metricName"
                placeholder="输入指标名称"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadResults">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- Aggregation Statistics -->
        <el-card class="stats-card" v-if="aggregationData">
          <h3>聚合统计</h3>
          <el-row :gutter="20">
            <el-col :span="6" v-for="(stats, metricName) in aggregationData" :key="metricName">
              <el-statistic :title="metricName">
                <template #formatter>
                  <div class="stats-detail">
                    <div>最小值: {{ stats.min?.toFixed(4) }}</div>
                    <div>最大值: {{ stats.max?.toFixed(4) }}</div>
                    <div>平均值: {{ stats.avg?.toFixed(4) }}</div>
                    <div v-if="stats.p95">P95: {{ stats.p95?.toFixed(4) }}</div>
                    <div v-if="stats.p99">P99: {{ stats.p99?.toFixed(4) }}</div>
                  </div>
                </template>
              </el-statistic>
            </el-col>
          </el-row>
        </el-card>

        <!-- Results Table -->
        <el-card class="table-card">
          <el-table
            :data="results"
            style="width: 100%"
            v-loading="loading"
          >
            <el-table-column prop="metricType" label="类型" width="100" />
            <el-table-column prop="metricName" label="指标名称" width="200" />
            <el-table-column prop="sourceModule" label="源模块" />
            <el-table-column prop="value" label="值" width="150">
              <template #default="{ row }">
                {{ formatValue(row.value) }}
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next, sizes"
            @current-change="loadResults"
            @size-change="loadResults"
            style="margin-top: 20px; justify-content: center"
          />
        </el-card>

        <!-- Vector Data Chart -->
        <el-card class="chart-card" v-if="showChart">
          <h3>向量数据可视化</h3>
          <div ref="chartRef" style="width: 100%; height: 400px;"></div>
          <div class="chart-controls">
            <el-input
              v-model="vectorMetricName"
              placeholder="输入向量指标名称"
              style="width: 200px;"
            />
            <el-button type="primary" @click="loadVectorData">加载数据</el-button>
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { projectApi, resultApi } from '../api'

const route = useRoute()
const router = useRouter()

const projectId = ref(route.params.projectId)
const projectName = ref('')
const loading = ref(false)
const results = ref([])
const currentPage = ref(1)
const pageSize = ref(50)
const total = ref(0)
const aggregationData = ref(null)
const showChart = ref(false)
const chartRef = ref(null)
const vectorMetricName = ref('')

let chartInstance = null

const filters = reactive({
  metricType: '',
  metricName: ''
})

const loadProject = async () => {
  try {
    const response = await projectApi.get(projectId.value)
    projectName.value = response.data.projectName
  } catch (error) {
    ElMessage.error('加载项目失败: ' + error.message)
  }
}

const loadResults = async () => {
  loading.value = true
  try {
    const response = await resultApi.query({
      projectId: projectId.value,
      metricType: filters.metricType || undefined,
      metricName: filters.metricName || undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    results.value = response.data.list
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('加载结果失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const loadAggregation = async () => {
  try {
    const response = await resultApi.aggregate({
      projectId: projectId.value,
      metrics: 'endToEndDelay,throughput',
      percentiles: '95,99'
    })
    aggregationData.value = response.data
  } catch (error) {
    console.error('加载聚合统计失败:', error)
  }
}

const loadVectorData = async () => {
  if (!vectorMetricName.value) {
    ElMessage.warning('请输入向量指标名称')
    return
  }

  try {
    const response = await resultApi.getVector({
      projectId: projectId.value,
      metricName: vectorMetricName.value,
      offset: 0,
      limit: 500
    })

    showChart.value = true
    await nextTick()

    if (!chartInstance && chartRef.value) {
      chartInstance = echarts.init(chartRef.value)
    }

    const option = {
      title: {
        text: vectorMetricName.value
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: response.data.slice.map((_, index) => index + response.data.offset)
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: response.data.slice,
          type: 'line',
          smooth: true
        }
      ]
    }

    chartInstance.setOption(option)
  } catch (error) {
    ElMessage.error('加载向量数据失败: ' + error.message)
  }
}

const exportResults = async (format) => {
  try {
    const response = await resultApi.export({
      projectId: projectId.value,
      format: format
    })
    
    // Create blob and download
    const blob = new Blob([response.data], { 
      type: format === 'csv' ? 'text/csv' : 'application/json' 
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `results_${projectId.value}.${format}`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败: ' + error.message)
  }
}

const resetFilters = () => {
  filters.metricType = ''
  filters.metricName = ''
  loadResults()
}

const formatValue = (value) => {
  if (typeof value === 'number') {
    return value.toFixed(6)
  }
  return value
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  loadProject()
  loadResults()
  loadAggregation()
})
</script>

<style scoped>
.results-container {
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

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header h2 {
  margin: 0;
  font-size: 18px;
}

.filter-card,
.stats-card,
.table-card,
.chart-card {
  margin-bottom: 20px;
}

.stats-card h3,
.chart-card h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
}

.stats-detail {
  font-size: 12px;
  color: #666;
}

.stats-detail > div {
  margin-bottom: 4px;
}

.chart-controls {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
