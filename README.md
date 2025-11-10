# 工业控制网络仿真平台 (Industrial Control Network Simulation Platform)

基于 OMNeT++ 的工业控制网络仿真平台，采用前后端分离架构。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **数据库**: H2 (可切换到 MySQL/PostgreSQL)
- **构建工具**: Maven 3.9+
- **WebSocket**: STOMP over SockJS

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI组件**: Element Plus
- **图形库**: @antv/g6 (拓扑编辑)
- **图表库**: ECharts (结果可视化)
- **路由**: Vue Router
- **HTTP客户端**: Axios

## 功能特性

### 1. 项目管理
- 创建、查询、更新、删除项目
- 项目列表分页和搜索
- 项目状态管理（未运行、运行中、已完成、失败）

### 2. 拓扑编辑
- 可视化拓扑编辑器（基于 G6）
- 拖拽添加网络节点（TsnDevice, TsnSwitch）
- 节点配置（应用、IP、HIL模式）
- 自动生成 OMNeT++ NED 和 INI 文件
- 自动生成 MasterConfig.json 和 SlaveConfig.json

### 3. 仿真控制
- 启动/停止仿真
- 实时日志流式传输（WebSocket）
- 仿真状态监控

### 4. 结果分析
- 仿真结果查询和筛选
- 标量和向量数据展示
- 聚合统计（min、max、avg、p95、p99）
- 向量数据可视化（曲线图）
- 结果导出（CSV、JSON）

## 快速开始

### 前置要求

1. Java 17+
2. Maven 3.9+
3. Node.js 20+
4. npm 10+
5. OMNeT++ 6.1+ (用于仿真)

### 后端启动

```bash
cd backend

# 编译
mvn clean package -DskipTests

# 运行
java -jar target/network-simulator-1.0.0.jar

# 或者使用 Maven
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

前端将在 `http://localhost:5173` 启动

## API 文档

详细 API 文档请参考：`工业控制网络仿真平台接口文档.md`

### 主要接口

#### 项目管理
- `GET /api/v1/projects` - 项目列表
- `POST /api/v1/projects` - 创建项目
- `GET /api/v1/projects/{projectId}` - 获取项目详情
- `PUT /api/v1/projects` - 更新项目
- `DELETE /api/v1/projects/{projectId}` - 删除项目

#### 拓扑管理
- `POST /api/v1/projects/{projectId}/topology` - 保存拓扑

#### 仿真控制
- `GET /api/v1/simulations/start/{projectId}` - 启动仿真
- `GET /api/v1/simulations/stop/{projectId}` - 停止仿真
- `WebSocket /ws/sim/log` - 实时日志

#### 结果查询
- `GET /api/v1/simulation-results` - 查询结果
- `GET /api/v1/simulation-results/export` - 导出结果
- `GET /api/v1/simulation-results/aggregate` - 聚合统计
- `GET /api/v1/simulation-results/vector` - 向量数据

## 目录结构

```
NetworkSimulatior/
├── backend/                          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/simulation/network/
│   │   │   │       ├── common/       # 公共类
│   │   │   │       ├── config/       # 配置类
│   │   │   │       ├── controller/   # 控制器
│   │   │   │       ├── dto/          # 数据传输对象
│   │   │   │       ├── entity/       # 实体类
│   │   │   │       ├── exception/    # 异常处理
│   │   │   │       ├── repository/   # 数据访问层
│   │   │   │       ├── service/      # 业务逻辑层
│   │   │   │       └── util/         # 工具类
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── api/                      # API 接口
│   │   ├── components/               # 公共组件
│   │   ├── router/                   # 路由配置
│   │   ├── utils/                    # 工具函数
│   │   ├── views/                    # 页面组件
│   │   │   ├── ProjectList.vue       # 项目列表
│   │   │   ├── TopologyEditor.vue    # 拓扑编辑器
│   │   │   └── SimulationResults.vue # 结果查看
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
│
├── run_sim.sh                        # 仿真启动脚本
└── 工业控制网络仿真平台接口文档.md
```

## 配置说明

### 后端配置 (application.properties)

```properties
# 服务器端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:h2:file:./data/simulator
spring.datasource.username=sa
spring.datasource.password=

# 仿真目录
simulation.base.dir=${user.home}/NetworkSimulation

# CORS 配置
cors.allowed.origins=http://localhost:3000,http://localhost:5173
```

### 前端配置 (vite.config.js)

```javascript
export default defineConfig({
  server: {
    port: 5173,
    host: true
  }
})
```

## 开发指南

### 添加新的节点类型

1. 在 TopologyEditor.vue 的 `components` 数组中添加新类型
2. 在 NedGenerator.java 中添加相应的 NED 生成逻辑
3. 在 IniGenerator.java 中添加相应的 INI 生成逻辑

### 添加新的应用类型

1. 在 TopologyEditor.vue 的应用类型下拉框中添加选项
2. 在 IniGenerator.java 中添加应用配置生成逻辑

### 自定义错误码

在 ErrorCode.java 中定义新的错误码：

```java
NEW_ERROR(40xxx, "错误描述")
```

## 注意事项

1. 确保 OMNeT++ 和 INET 已正确安装并配置环境变量
2. 仿真脚本 `run_sim.sh` 路径需要根据实际情况调整
3. 默认数据库使用 H2，生产环境建议切换到 MySQL 或 PostgreSQL
4. WebSocket 连接需要前后端同时运行
5. 仿真结果的 SQLite 文件需要 JDBC 驱动支持

## 故障排查

### 后端无法启动
- 检查 Java 版本是否为 17+
- 检查端口 8080 是否被占用
- 检查数据库连接配置

### 前端无法启动
- 清除 node_modules 并重新安装依赖
- 检查 Node.js 版本
- 检查端口 5173 是否被占用

### 仿真无法启动
- 检查 OMNeT++ 是否正确安装
- 检查 run_sim.sh 脚本权限
- 查看仿真日志获取详细错误信息

### WebSocket 连接失败
- 检查后端是否正常运行
- 检查 CORS 配置
- 检查浏览器控制台错误信息

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题，请提交 Issue。
