// API 基础路径
const API_BASE = '';
let websocket = null;

// 页面导航
document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const page = btn.dataset.page;
        
        // 更新导航按钮状态
        document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        
        // 显示对应页面
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.getElementById(`${page}-page`).classList.add('active');
        
        // 加载页面数据
        switch(page) {
            case 'jobs':
                loadJobs();
                break;
            case 'workflows':
                loadWorkflows();
                break;
            case 'instances':
                loadInstances();
                break;
            case 'monitor':
                break;
        }
    });
});

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    loadJobs();
});

// ===== 任务管理 =====
async function loadJobs() {
    const searchValue = document.getElementById('job-search').value;
    const tbody = document.getElementById('jobs-tbody');
    tbody.innerHTML = '<tr><td colspan="8" class="loading">加载中...</td></tr>';
    
    try {
        const response = await fetch('/job/list', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                jobName: searchValue
            })
        });
        
        const result = await response.json();
        
        if (result && result.list) {
            renderJobsTable(result.list);
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="loading">暂无数据</td></tr>';
        }
    } catch (error) {
        console.error('加载任务失败:', error);
        tbody.innerHTML = '<tr><td colspan="8" class="loading">加载失败，请检查后端服务是否运行</td></tr>';
    }
}

function renderJobsTable(jobs) {
    const tbody = document.getElementById('jobs-tbody');
    
    if (!jobs || jobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = jobs.map(job => `
        <tr>
            <td>${job.jobId}</td>
            <td>${job.jobName || ''}</td>
            <td>${job.cron || '-'}</td>
            <td>${job.processorInfo || ''}</td>
            <td>${getDispatchStrategyName(job.dispatchStrategy)}</td>
            <td>
                <span class="status-badge ${job.status === 1 ? 'status-running' : 'status-stopped'}">
                    ${job.status === 1 ? '运行中' : '已停止'}
                </span>
            </td>
            <td>${formatTime(job.nextTriggerTime)}</td>
            <td>
                <button class="btn" onclick="viewJobDetail(${job.jobId})">详情</button>
                <button class="btn ${job.status === 1 ? 'btn-danger' : 'btn-success'}" onclick="toggleJob(${job.jobId})">
                    ${job.status === 1 ? '停止' : '启动'}
                </button>
                <button class="btn btn-success" onclick="startJob(${job.jobId})">执行</button>
                <button class="btn btn-danger" onclick="deleteJob(${job.jobId})">删除</button>
            </td>
        </tr>
    `).join('');
}

function getDispatchStrategyName(strategy) {
    const strategies = {
        1: '轮询',
        2: '随机',
        3: '哈希',
        4: '指定Worker'
    };
    return strategies[strategy] || '-';
}

function formatTime(timestamp) {
    if (!timestamp || timestamp === 0) return '-';
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN');
}

function showJobModal(jobId = null) {
    const modal = document.getElementById('job-modal');
    const title = document.getElementById('job-modal-title');
    const form = document.getElementById('job-form');
    
    if (jobId) {
        title.textContent = '编辑任务';
        // TODO: 加载任务详情并填充表单
    } else {
        title.textContent = '新建任务';
        form.reset();
    }
    
    modal.classList.add('show');
}

function closeJobModal() {
    document.getElementById('job-modal').classList.remove('show');
}

async function saveJob(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    
    const jobData = {
        jobName: formData.get('jobName'),
        cron: formData.get('cron'),
        processorInfo: formData.get('processorInfo'),
        dispatchStrategy: parseInt(formData.get('dispatchStrategy')),
        tag: formData.get('tag'),
        params: formData.get('params'),
        maxRetryTimes: parseInt(formData.get('maxRetryTimes')),
        retryInterval: parseInt(formData.get('retryInterval'))
    };
    
    try {
        const response = await fetch('/job/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(jobData)
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('保存成功');
            closeJobModal();
            loadJobs();
        } else {
            alert('保存失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('保存任务失败:', error);
        alert('保存失败，请检查网络连接');
    }
}

async function toggleJob(jobId) {
    try {
        const response = await fetch(`/job/toggle/${jobId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200) {
            loadJobs();
        } else {
            alert('操作失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('切换任务状态失败:', error);
        alert('操作失败，请检查网络连接');
    }
}

async function startJob(jobId) {
    try {
        const response = await fetch(`/job/start/${jobId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('任务已启动');
        } else {
            alert('启动失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('启动任务失败:', error);
        alert('启动失败，请检查网络连接');
    }
}

async function deleteJob(jobId) {
    if (!confirm('确定要删除这个任务吗？')) {
        return;
    }
    
    try {
        const response = await fetch(`/job/delete?/${jobId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('删除成功');
            loadJobs();
        } else {
            alert('删除失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('删除任务失败:', error);
        alert('删除失败，请检查网络连接');
    }
}

async function viewJobDetail(jobId) {
    try {
        const response = await fetch(`/job/detail/${jobId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200 && result.data) {
            const job = result.data;
            alert(`任务详情:\n${JSON.stringify(job, null, 2)}`);
        }
    } catch (error) {
        console.error('获取任务详情失败:', error);
    }
}

// ===== 工作流管理 =====
async function loadWorkflows() {
    const searchValue = document.getElementById('workflow-search').value;
    const tbody = document.getElementById('workflows-tbody');
    tbody.innerHTML = '<tr><td colspan="5" class="loading">加载中...</td></tr>';
    
    try {
        const response = await fetch('/job/flow/list', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: searchValue
            })
        });
        
        const result = await response.json();
        
        if (result && result.list) {
            renderWorkflowsTable(result.list);
        } else {
            tbody.innerHTML = '<tr><td colspan="5" class="loading">暂无数据</td></tr>';
        }
    } catch (error) {
        console.error('加载工作流失败:', error);
        tbody.innerHTML = '<tr><td colspan="5" class="loading">加载失败，请检查后端服务是否运行</td></tr>';
    }
}

function renderWorkflowsTable(workflows) {
    const tbody = document.getElementById('workflows-tbody');
    
    if (!workflows || workflows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="loading">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = workflows.map(flow => `
        <tr>
            <td>${flow.flowId}</td>
            <td>${flow.name || ''}</td>
            <td>${flow.createdDate || '-'}</td>
            <td>${flow.updatedDate || '-'}</td>
            <td>
                <button class="btn" onclick="viewWorkflowDetail(${flow.flowId})">详情</button>
                <button class="btn btn-success" onclick="startWorkflow(${flow.flowId})">启动</button>
                <button class="btn btn-danger" onclick="deleteWorkflow(${flow.flowId})">删除</button>
            </td>
        </tr>
    `).join('');
}

function showWorkflowModal() {
    const modal = document.getElementById('workflow-modal');
    document.getElementById('workflow-form').reset();
    modal.classList.add('show');
}

function closeWorkflowModal() {
    document.getElementById('workflow-modal').classList.remove('show');
}

async function saveWorkflow(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    
    const workflowData = {
        name: formData.get('name'),
        dag: formData.get('dag'),
        params: formData.get('params')
    };
    
    try {
        const response = await fetch('/job/flow/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(workflowData)
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('保存成功');
            closeWorkflowModal();
            loadWorkflows();
        } else {
            alert('保存失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('保存工作流失败:', error);
        alert('保存失败，请检查网络连接');
    }
}

async function startWorkflow(flowId) {
    try {
        const response = await fetch(`/job/flow/start/${flowId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('工作流已启动，实例ID: ' + result.data);
        } else {
            alert('启动失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('启动工作流失败:', error);
        alert('启动失败，请检查网络连接');
    }
}

async function deleteWorkflow(flowId) {
    if (!confirm('确定要删除这个工作流吗？')) {
        return;
    }
    
    try {
        const response = await fetch(`/job/flow/delete/${flowId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('删除成功');
            loadWorkflows();
        } else {
            alert('删除失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('删除工作流失败:', error);
        alert('删除失败，请检查网络连接');
    }
}

async function viewWorkflowDetail(flowId) {
    try {
        const response = await fetch(`/job/flow/detail/${flowId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        if (result.code === 200 && result.data) {
            const flow = result.data;
            alert(`工作流详情:\n${JSON.stringify(flow, null, 2)}`);
        }
    } catch (error) {
        console.error('获取工作流详情失败:', error);
    }
}

// ===== 任务实例 =====
async function loadInstances() {
    const jobId = document.getElementById('instance-job-id').value;
    const status = document.getElementById('instance-status').value;
    const tbody = document.getElementById('instances-tbody');
    tbody.innerHTML = '<tr><td colspan="7" class="loading">加载中...</td></tr>';
    
    try {
        const requestBody = {};
        if (jobId) requestBody.jobId = parseInt(jobId);
        if (status) requestBody.status = parseInt(status);
        
        const response = await fetch('/job/instance/list', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        
        const result = await response.json();
        
        if (result && result.list) {
            renderInstancesTable(result.list);
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="loading">暂无数据</td></tr>';
        }
    } catch (error) {
        console.error('加载实例失败:', error);
        tbody.innerHTML = '<tr><td colspan="7" class="loading">加载失败，请检查后端服务是否运行</td></tr>';
    }
}

function renderInstancesTable(instances) {
    const tbody = document.getElementById('instances-tbody');
    
    if (!instances || instances.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="loading">暂无数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = instances.map(instance => `
        <tr>
            <td>${instance.instanceId}</td>
            <td>${instance.jobId}</td>
            <td>${instance.jobName || ''}</td>
            <td>${instance.workerAddress || '-'}</td>
            <td>${instance.triggerTime || '-'}</td>
            <td>
                <span class="status-badge ${getInstanceStatusClass(instance.status)}">
                    ${getInstanceStatusName(instance.status)}
                </span>
            </td>
            <td>
                <button class="btn" onclick="viewInstanceLog(${instance.instanceId})">日志</button>
                ${instance.status === 1 ? `<button class="btn btn-danger" onclick="stopInstance(${instance.jobId}, ${instance.instanceId})">停止</button>` : ''}
            </td>
        </tr>
    `).join('');
}

function getInstanceStatusName(status) {
    const statuses = {
        0: '等待接收',
        1: '运行中',
        2: '失败',
        3: '成功',
        4: '暂停'
    };
    return statuses[status] || '未知';
}

function getInstanceStatusClass(status) {
    const classes = {
        0: 'status-waiting',
        1: 'status-running',
        2: 'status-failed',
        3: 'status-success',
        4: 'status-stopped'
    };
    return classes[status] || '';
}

async function stopInstance(jobId, instanceId) {
    try {
        const response = await fetch('/job/stop', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                jobId: jobId,
                instanceId: instanceId
            })
        });
        
        const result = await response.json();
        if (result.code === 200) {
            alert('停止成功');
            loadInstances();
        } else {
            alert('停止失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        console.error('停止实例失败:', error);
        alert('停止失败，请检查网络连接');
    }
}

async function viewInstanceLog(instanceId) {
    const modal = document.getElementById('log-modal');
    const content = document.getElementById('log-content');
    modal.classList.add('show');
    content.innerHTML = '<p class="loading">加载中...</p>';
    
    try {
        const response = await fetch(`/job/log/${instanceId}`, {
            method: 'GET'
        });
        
        const result = await response.json();
        
        if (result && result.list) {
            renderLogs(result.list);
        } else {
            content.innerHTML = '<p class="loading">暂无日志</p>';
        }
    } catch (error) {
        console.error('加载日志失败:', error);
        content.innerHTML = '<p class="loading">加载失败</p>';
    }
}

function renderLogs(logs) {
    const content = document.getElementById('log-content');
    
    if (!logs || logs.length === 0) {
        content.innerHTML = '<p class="loading">暂无日志</p>';
        return;
    }
    
    content.innerHTML = logs.map(log => {
        const levelClass = log.level === 2 ? 'log-level-error' : 
                          log.level === 1 ? 'log-level-warn' : 'log-level-info';
        return `<div class="log-item ${levelClass}">[${log.timestamp}] ${log.content}</div>`;
    }).join('');
}

function closeLogModal() {
    document.getElementById('log-modal').classList.remove('show');
}

// ===== WebSocket 监控 =====
function toggleWebSocket() {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        websocket.close();
    } else {
        connectWebSocket();
    }
}

function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/job`;
    
    websocket = new WebSocket(wsUrl);
    
    websocket.onopen = () => {
        updateWebSocketStatus(true);
        addEventLog('WebSocket 连接成功');
    };
    
    websocket.onmessage = (event) => {
        addEventLog('收到消息: ' + event.data);
    };
    
    websocket.onerror = (error) => {
        console.error('WebSocket 错误:', error);
        addEventLog('WebSocket 错误');
    };
    
    websocket.onclose = () => {
        updateWebSocketStatus(false);
        addEventLog('WebSocket 连接已关闭');
    };
}

function updateWebSocketStatus(connected) {
    const status = document.getElementById('ws-status');
    const button = document.getElementById('ws-toggle');
    
    if (connected) {
        status.textContent = '已连接';
        status.className = 'status-connected';
        button.textContent = '断开连接';
    } else {
        status.textContent = '未连接';
        status.className = 'status-disconnected';
        button.textContent = '连接 WebSocket';
    }
}

function addEventLog(message) {
    const log = document.getElementById('event-log');
    const now = new Date().toLocaleTimeString('zh-CN');
    const eventItem = document.createElement('div');
    eventItem.className = 'event-item';
    eventItem.innerHTML = `
        <div class="event-time">${now}</div>
        <div>${message}</div>
    `;
    log.insertBefore(eventItem, log.firstChild);
    
    // 最多保留 50 条日志
    while (log.children.length > 50) {
        log.removeChild(log.lastChild);
    }
}
