<template>
  <div class="topology-editor">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
          <h2>拓扑编辑 - {{ projectName }}</h2>
        </div>
        <div class="header-right">
          <el-button type="success" @click="saveTopology" :loading="saving">
            保存拓扑
          </el-button>
          <el-button type="primary" @click="startSimulation" :loading="simulating">
            启动仿真
          </el-button>
          <el-button type="danger" @click="stopSimulation" v-if="isSimulating">
            停止仿真
          </el-button>
        </div>
      </el-header>

      <el-container>
        <el-aside width="200px" class="component-panel">
          <h3>组件库</h3>
          <div class="component-list">
            <div
              v-for="comp in components"
              :key="comp.type"
              class="component-item"
              draggable="true"
              @dragstart="handleDragStart($event, comp)"
            >
              <el-icon><Operation /></el-icon>
              {{ comp.name }}
            </div>
          </div>
        </el-aside>

        <el-main class="canvas-container">
          <div
            id="g6-canvas"
            ref="canvasRef"
            @drop="handleDrop"
            @dragover.prevent
          ></div>
        </el-main>

        <el-aside width="350px" class="property-panel" v-if="selectedNode">
          <h3>节点配置</h3>
          <el-form :model="nodeConfig" label-width="100px">
            <el-form-item label="节点名称">
              <el-input v-model="nodeConfig.nodeName" />
            </el-form-item>
            <el-form-item label="节点类型">
              <el-input v-model="nodeConfig.nodeType" disabled />
            </el-form-item>
            <el-form-item label="HIL模式">
              <el-switch v-model="nodeConfig.params.isHil" />
            </el-form-item>
            
            <el-divider>应用配置</el-divider>
            <div v-for="(app, index) in nodeConfig.params.apps" :key="index" class="app-config">
              <h4>应用 {{ index + 1 }}</h4>
              <el-form-item label="类型">
                <el-select v-model="app.typename">
                  <el-option label="ModbusMasterApp" value="ModbusMasterApp" />
                  <el-option label="ModbusSlaveApp" value="ModbusSlaveApp" />
                  <el-option label="ModbusSlaveHILApp" value="ModbusSlaveHILApp" />
                  <el-option label="ModbusTcpServerApp" value="ModbusTcpServerApp" />
                  <el-option label="TransitApp" value="TransitApp" />
                  <el-option label="OperatorStationApp" value="OperatorStationApp" />
                </el-select>
              </el-form-item>
              <el-form-item label="本地地址">
                <el-input v-model="app.localAddress" placeholder="留空自动" />
              </el-form-item>
              <el-form-item label="本地端口">
                <el-input-number v-model="app.localPort" :min="1" :max="65535" />
              </el-form-item>
              <el-button type="danger" size="small" @click="removeApp(index)">
                删除应用
              </el-button>
            </div>
            <el-button type="primary" @click="addApp" size="small">
              添加应用
            </el-button>

            <el-divider>IP配置</el-divider>
            <div v-for="(ip, index) in nodeConfig.params.ipConfig" :key="index" class="ip-config">
              <el-form-item :label="'接口 ' + index">
                <el-input v-model="ip.ipAddress" placeholder="192.168.1.1" />
              </el-form-item>
              <el-form-item label="子网掩码">
                <el-input v-model="ip.netmask" placeholder="255.255.255.0" />
              </el-form-item>
            </div>

            <el-button type="primary" @click="applyNodeConfig">应用配置</el-button>
          </el-form>
        </el-aside>
      </el-container>
    </el-container>

    <!-- Simulation Log Dialog -->
    <el-dialog v-model="showLogDialog" title="仿真日志" width="70%" top="5vh">
      <div class="log-container">
        <el-scrollbar height="500px">
          <div v-for="(log, index) in simulationLogs" :key="index" class="log-line">
            {{ log }}
          </div>
        </el-scrollbar>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Operation } from '@element-plus/icons-vue'
import * as G6 from '@antv/g6'
import { projectApi, topologyApi, simulationApi } from '../api'
import { SimulationWebSocket } from '../utils/websocket'

const route = useRoute()
const router = useRouter()

const projectId = ref(route.params.projectId)
const projectName = ref('')
const saving = ref(false)
const simulating = ref(false)
const isSimulating = ref(false)
const canvasRef = ref(null)
const selectedNode = ref(null)
const showLogDialog = ref(false)
const simulationLogs = ref([])

let graph = null
let wsClient = null
let nodeIdCounter = 1

const components = [
  { type: 'TsnDevice', name: 'TSN设备' },
  { type: 'TsnSwitch', name: 'TSN交换机' }
]

const nodeConfig = reactive({
  nodeId: '',
  nodeName: '',
  nodeType: '',
  params: {
    isHil: false,
    apps: [],
    ipConfig: [],
    status: 'configured'
  }
})

const initGraph = () => {
  const container = document.getElementById('g6-canvas')
  const width = container.offsetWidth
  const height = container.offsetHeight

  graph = new G6.Graph({
    container: 'g6-canvas',
    width,
    height,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node']
    },
    defaultNode: {
      size: 60,
      style: {
        fill: '#5B8FF9',
        stroke: '#5B8FF9',
        lineWidth: 2
      },
      labelCfg: {
        style: {
          fill: '#000',
          fontSize: 12
        },
        position: 'bottom'
      }
    },
    defaultEdge: {
      style: {
        stroke: '#e2e2e2',
        lineWidth: 2
      }
    },
    layout: {
      type: 'force',
      preventOverlap: true,
      linkDistance: 150
    }
  })

  // Node click event
  graph.on('node:click', (evt) => {
    const node = evt.item
    const model = node.getModel()
    selectedNode.value = node
    Object.assign(nodeConfig, model)
  })

  // Node double click to add edge
  graph.on('node:dblclick', (evt) => {
    // This would open edge creation mode
  })
}

const handleDragStart = (event, component) => {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('componentType', component.type)
}

const handleDrop = (event) => {
  event.preventDefault()
  const componentType = event.dataTransfer.getData('componentType')
  
  const point = graph.getPointByClient(event.clientX, event.clientY)
  
  const nodeId = `node_${nodeIdCounter++}`
  const nodeName = componentType === 'TsnDevice' ? `device${nodeIdCounter}` : `switch${nodeIdCounter}`
  
  const newNode = {
    id: nodeId,
    nodeId: nodeId,
    nodeName: nodeName,
    nodeType: componentType,
    label: nodeName,
    x: point.x,
    y: point.y,
    position: {
      x: point.x,
      y: point.y
    },
    params: {
      isHil: false,
      apps: [],
      ipConfig: [
        { ipAddress: '', netmask: '255.255.255.0', interfaceName: 'eth[0]' }
      ],
      status: 'configured'
    }
  }
  
  graph.addItem('node', newNode)
}

const addApp = () => {
  nodeConfig.params.apps.push({
    typename: 'ModbusSlaveApp',
    localAddress: '',
    localPort: 502
  })
}

const removeApp = (index) => {
  nodeConfig.params.apps.splice(index, 1)
}

const applyNodeConfig = () => {
  if (selectedNode.value) {
    graph.updateItem(selectedNode.value, nodeConfig)
    ElMessage.success('配置已应用')
  }
}

const saveTopology = async () => {
  saving.value = true
  try {
    const nodes = []
    const edges = []
    
    graph.getNodes().forEach(node => {
      const model = node.getModel()
      nodes.push({
        nodeId: model.nodeId,
        nodeType: model.nodeType,
        nodeName: model.nodeName,
        position: model.position || { x: model.x, y: model.y },
        params: model.params
      })
    })
    
    graph.getEdges().forEach(edge => {
      const model = edge.getModel()
      edges.push({
        linkId: model.id,
        sourceNodeId: model.source,
        targetNodeId: model.target,
        dataRate: '100Mbps',
        ber: 0,
        per: 0,
        length: '10m',
        status: 'configured'
      })
    })
    
    await topologyApi.save(projectId.value, { nodes, links: edges })
    ElMessage.success('拓扑保存成功')
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const startSimulation = async () => {
  simulating.value = true
  simulationLogs.value = []
  showLogDialog.value = true
  
  try {
    // Connect WebSocket first
    wsClient = new SimulationWebSocket()
    wsClient.connect(
      projectId.value,
      (log) => {
        simulationLogs.value.push(log)
      },
      (error) => {
        console.error('WebSocket error:', error)
      }
    )
    
    // Start simulation
    await simulationApi.start(projectId.value)
    isSimulating.value = true
    ElMessage.success('仿真已启动')
  } catch (error) {
    ElMessage.error('启动失败: ' + error.message)
    if (wsClient) {
      wsClient.disconnect()
    }
  } finally {
    simulating.value = false
  }
}

const stopSimulation = async () => {
  try {
    await simulationApi.stop(projectId.value)
    isSimulating.value = false
    ElMessage.success('仿真已停止')
    if (wsClient) {
      wsClient.disconnect()
    }
  } catch (error) {
    ElMessage.error('停止失败: ' + error.message)
  }
}

const goBack = () => {
  router.push('/')
}

const loadProject = async () => {
  try {
    const response = await projectApi.get(projectId.value)
    projectName.value = response.data.projectName
  } catch (error) {
    ElMessage.error('加载项目失败: ' + error.message)
  }
}

onMounted(() => {
  loadProject()
  initGraph()
})

onUnmounted(() => {
  if (wsClient) {
    wsClient.disconnect()
  }
  if (graph) {
    graph.destroy()
  }
})
</script>

<style scoped>
.topology-editor {
  width: 100%;
  height: 100vh;
  overflow: hidden;
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

.component-panel {
  background: white;
  border-right: 1px solid #e0e0e0;
  padding: 15px;
  overflow-y: auto;
}

.component-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
}

.component-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.component-item {
  padding: 10px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: move;
  display: flex;
  align-items: center;
  gap: 8px;
}

.component-item:hover {
  background: #e8e8e8;
}

.canvas-container {
  padding: 0;
  background: #fafafa;
}

#g6-canvas {
  width: 100%;
  height: 100%;
}

.property-panel {
  background: white;
  border-left: 1px solid #e0e0e0;
  padding: 15px;
  overflow-y: auto;
}

.property-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
}

.app-config, .ip-config {
  background: #f9f9f9;
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 4px;
}

.app-config h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
}

.log-container {
  background: #000;
  color: #0f0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  padding: 10px;
}

.log-line {
  margin-bottom: 2px;
  line-height: 1.4;
}
</style>
