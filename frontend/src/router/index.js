import { createRouter, createWebHistory } from 'vue-router'
import ProjectList from '../views/ProjectList.vue'
import TopologyEditor from '../views/TopologyEditor.vue'
import SimulationResults from '../views/SimulationResults.vue'

const routes = [
  {
    path: '/',
    name: 'ProjectList',
    component: ProjectList
  },
  {
    path: '/topology/:projectId',
    name: 'TopologyEditor',
    component: TopologyEditor
  },
  {
    path: '/results/:projectId',
    name: 'SimulationResults',
    component: SimulationResults
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
