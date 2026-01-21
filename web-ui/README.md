# Job Flow Web UI

基于 Vue 3 + Element Plus 的现代化任务调度管理界面，采用 n8n 风格设计。

## ✨ 特性

- 🎨 **现代化设计**：参考 n8n 的暗色主题设计，专业美观
- 🔗 **可视化工作流编辑**：拖拽式 DAG 编辑器，直观创建工作流
- ⚡ **实时响应**：WebSocket 实时监控任务状态
- 📱 **响应式布局**：支持桌面和移动端访问
- 🛠️ **现代技术栈**：Vue 3 + Vite + Element Plus + Vue Flow

## 🏗️ 技术栈

- **Vue 3** - 最新的 Vue 框架，使用 Composition API
- **Vite** - 快速的现代化构建工具
- **Element Plus** - 企业级 Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 状态管理
- **Vue Flow** - 强大的节点流程图库，用于工作流可视化
- **Axios** - HTTP 客户端

## 📦 安装依赖

```bash
npm install
```

## 🚀 开发

启动开发服务器（带热更新）：

```bash
npm run dev
```

访问 http://localhost:3000

开发服务器会自动代理 API 请求到 `http://localhost:8080`

## 🔨 构建

构建生产版本：

```bash
npm run build
```

构建产物会输出到 `../admin/src/main/resources/static` 目录，Spring Boot 会自动提供静态文件服务。

## 📁 项目结构

```
web-ui/
├── src/
│   ├── api/              # API 接口定义
│   ├── assets/           # 静态资源
│   ├── components/       # 可复用组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── styles/           # 全局样式
│   ├── views/            # 页面组件
│   │   ├── Layout.vue    # 主布局
│   │   ├── Jobs.vue      # 任务管理
│   │   ├── Workflows.vue # 工作流列表
│   │   ├── WorkflowEditor.vue  # 工作流编辑器（核心）
│   │   ├── Instances.vue # 实例管理
│   │   └── Monitor.vue   # 实时监控
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html            # HTML 模板
├── vite.config.js        # Vite 配置
└── package.json          # 项目配置
```

## 🎯 核心功能

### 1. 任务管理
- 创建、编辑、删除任务
- 配置 Cron 表达式
- 选择调度策略（轮询、随机、哈希、指定 Worker）
- 启动/停止任务
- 立即执行任务

### 2. 可视化工作流编辑器
- **拖拽添加节点**：从左侧面板拖拽节点到画布
- **连接节点**：点击节点手柄创建连线，形成 DAG
- **节点配置**：点击节点配置属性（任务 ID、名称、颜色等）
- **画布操作**：
  - 缩放（鼠标滚轮或控制栏）
  - 拖动画布
  - 小地图导航
  - 节点对齐辅助线
- **保存和执行**：保存工作流，支持立即执行

### 3. 实例监控
- 查看任务执行实例
- 按状态筛选（等待、运行中、成功、失败、暂停）
- 查看详细执行日志
- 停止运行中的任务

### 4. 实时监控
- WebSocket 连接管理
- 实时接收任务事件
- 系统统计展示

## 🎨 主题定制

项目使用 CSS 变量定义 n8n 风格主题，可以在 `src/styles/main.css` 中修改：

```css
:root {
  --n8n-bg-primary: #1a1a1a;      /* 主背景色 */
  --n8n-bg-secondary: #2a2a2a;    /* 次要背景色 */
  --n8n-bg-tertiary: #333333;     /* 第三级背景色 */
  --n8n-text-primary: #ffffff;    /* 主文字颜色 */
  --n8n-text-secondary: #a0a0a0;  /* 次要文字颜色 */
  --n8n-accent-color: #ff6d5a;    /* 强调色 */
  --n8n-success-color: #4caf50;   /* 成功色 */
  --n8n-error-color: #f44336;     /* 错误色 */
}
```

## 🔧 配置

### API 代理配置

开发环境的 API 代理在 `vite.config.js` 中配置：

```javascript
server: {
  port: 3000,
  proxy: {
    '/job': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 构建配置

构建输出目录配置：

```javascript
build: {
  outDir: '../admin/src/main/resources/static',
  emptyOutDir: true
}
```

## 📝 开发建议

1. **代码规范**：使用 ESLint 和 Prettier
2. **组件复用**：将通用逻辑抽取为可复用组件
3. **状态管理**：复杂状态使用 Pinia 管理
4. **类型安全**：可考虑迁移到 TypeScript
5. **性能优化**：使用路由懒加载，避免首屏加载过大

## 🐛 常见问题

### 1. 构建后 API 请求失败
确保后端服务运行在 8080 端口，或修改 `vite.config.js` 中的代理配置。

### 2. WebSocket 连接失败
检查防火墙设置，确保 WebSocket 端口未被阻止。

### 3. 样式不生效
清除浏览器缓存，或使用无痕模式测试。

## 📄 License

与主项目保持一致
