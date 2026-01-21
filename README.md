# job-flow

## 🚀 简介

轻量级分布式任务调度系统，基于 **Hazelcast** 构建提供动态扩展的调度能力。 支持 cron，api 方式进行触发。支持对工作流进行 DAG 级别的调度和局部重试，外部依赖仅需数据库。


![工作流调度](https://github.com/user-attachments/assets/25ec5ba2-c62c-403a-b350-5d99bf1fbf9a)


## ✨ 功能特性

### 🖥️ 集群管理
- 采用 **Hazelcast** 组成弱集群架构  
- 支持 **Kubernetes（K8s）** 自动发现，通过指定 **Namespace** 进行服务发现  

### 📌 任务管理
- 支持 **任务的 CRUD**（创建、读取、更新、删除）  
- 提供 **Cron 表达式 & API 触发** 调度任务  
- 多种调度策略：**轮询、哈希、随机、自定义**  
- 运行状态监控 & 详细日志记录  

### 🔗 工作流编排
- **DAG（有向无环图）** 任务编排，支持任务依赖顺序执行  
- 提供 **WebSocket 订阅**，实时监听工作流变更事件  
- **子版本机制**：支持从指定节点局部重试，提高调度粒度  

### ⚡ handle 注册
- **启动时自动注册** worker 将 handle 注册至 Hazelcast 分布式容器  
- 通过 **定期心跳检测 & 节点离线监控** 维护能力有效性  

### ⏳ 定时调度
- **任务通过哈希策略进行分配**  
- 基于 **Cron 表达式** 计算下次触发时间  
- **结合 Netty 时间轮**，精准触发即将执行的任务  

## 📦 依赖环境
- **Java 17+**  
- **Vert.x**  
- **Hazelcast**  
- **Kubernetes（可选）**

## 📜 使用方式

### 环境构建


通过 docker 安装 mysql

```bash
docker pull mysql:8.0
docker run -d -p 3306:3306 --name mysql-container -e MYSQL_ROOT_PASSWORD=your_pwd mysql:8.0
```

查看容器启动状态
```bash
docker ps
```


## 数据库库表构建

进入 mysql 容器
```bash
docker exec -it {your_mysql_container_id} mysql -p
```

创建数据库和用户
```mysql
SET GLOBAL time_zone = 'Asia/Shanghai';
create database job_flow;
create user 'develop'@'%' identified by 'develop';
grant all privileges on job_flow.* to 'develop'@'%';
```

表结构
```sql
drop table if exists t_job_info;
create table if not exists t_job_info
(
    job_id             bigint(20) unsigned primary key auto_increment comment 'ID',
    job_name           varchar(255) comment '任务名称',
    cron               varchar(255) comment 'cron表达式',
    processor_info     varchar(255) comment '处理器信息(节点实现类全路径)',
    dispatch_strategy  int(5) comment '调度策略 (1-轮询, 2-随机, 3-hash, 4-指定)',
    designated_workers varchar(255) comment '指定 worker (若存在多个用“,”分隔, 此时任意调度)',
    tag                varchar(255) comment 'worker tag',
    params             longtext comment '参数',
    status             int(5)   default 0 comment '状态（0-停止, 1-启动）',
    next_trigger_time  bigint   default 0 comment '下次触发时间',
    server_ip          varchar(255) comment '服务 IP',
    max_retry_times    int(5)   default 0 comment '最大重试次数',
    retry_interval     int(10)  default 1 comment '重试间隔(毫秒)',
    created_date       datetime default current_timestamp comment '创建时间',
    updated_date       datetime default current_timestamp on update current_timestamp comment '更新时间',
    index idx_next_trigger_time (next_trigger_time)
) comment '任务信息表';

drop table if exists t_job_instance;
create table if not exists t_job_instance
(
    id                bigint(20) unsigned primary key auto_increment comment '实例 ID',
    job_id            bigint comment '任务 ID',
    job_name          varchar(255) comment '任务名称',
    instance_id       bigint(20) unsigned comment '任务实例 ID',
    flow_instance_id  bigint(20) unsigned comment '工作流实例 ID',
    flow_node_id      bigint(20) unsigned comment '工作流节点 ID',
    dispatch_strategy int(5) comment '调度策略 (1-轮询, 2-随机, 3-hash, 4-指定)',
    tag               varchar(255) comment 'worker tag',
    processor_info    varchar(255) comment '处理器信息(节点实现类全路径)',
    params            longtext comment '参数',
    worker_address    varchar(255) comment 'Worker地址',
    trigger_time      datetime comment '触发时间',
    reply_time        datetime comment '最后上报时间',
    end_time          datetime comment '结束时间',
    result            longtext comment '结果',
    status            int(5)   default 0 comment '执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停）',
    created_date      datetime default current_timestamp comment '创建时间',
    updated_date      datetime default current_timestamp on update current_timestamp comment '更新时间',
    index idx_job_id (job_id, status),
    index idx_instance_id (instance_id, status),
    index idx_flow_instance_id (flow_instance_id, status)
) comment '任务实例表';

drop table if exists t_job_log;
create table if not exists t_job_log
(
    id             bigint(20) unsigned primary key auto_increment comment 'ID',
    job_id         bigint(20) unsigned comment '任务 ID',
    instance_id    bigint(20) unsigned comment '实例 ID',
    worker_address varchar(255) comment 'Worker地址',
    timestamp      datetime comment '日志时间',
    level          int(5) comment '日志级别',
    content        longtext comment '日志内容',
    created_date   datetime default current_timestamp comment '创建时间',
    updated_date   datetime default current_timestamp on update current_timestamp comment '更新时间',
    index idx_jod_instance (job_id, instance_id)
) comment '任务日志表';

drop table if exists t_job_flow;
create table if not exists t_job_flow
(
    flow_id      bigint(20) unsigned primary key auto_increment comment 'ID',
    name         varchar(255) comment '工作流名称',
    dag          longtext comment 'DAG',
    params       longtext comment '工作流级别参数',
    created_date datetime default current_timestamp comment '创建时间',
    updated_date datetime default current_timestamp on update current_timestamp comment '更新时间'
) comment '工作流表';

drop table if exists t_job_flow_instance;
create table if not exists t_job_flow_instance
(
    id           bigint(20) unsigned primary key auto_increment comment 'ID',
    flow_id      bigint(20) unsigned comment '工作流 ID',
    version      int(7) unsigned default 0 comment '版本号(暂停, 重试时自增)',
    name         varchar(255) comment '任务流名称',
    dag          longtext comment 'DAG',
    params       longtext comment '工作流级别参数',
    trigger_time datetime comment '触发时间',
    end_time     datetime comment '结束时间',
    status       int(5)          default 0 comment '执行状态（0-等待Worker接收 1-运行中 2-失败 3-成功 4-暂停）',
    result       varchar(255) comment '结果',
    created_date datetime        default current_timestamp comment '创建时间',
    updated_date datetime        default current_timestamp on update current_timestamp comment '更新时间',
    index idx_flow_id (flow_id)
) comment '工作流实例表';
```

## 🎨 Web 管理界面

访问 `http://localhost:8080` 即可使用 Web 管理控制台

### 功能特性
- **任务管理**：创建、编辑、启动、停止、删除任务
- **工作流管理**：创建、编辑、启动工作流，支持 DAG 可视化编排
- **任务实例监控**：查看任务执行状态、日志
- **实时监控**：通过 WebSocket 实时接收任务状态变更事件

### 使用说明
1. 启动 Admin 服务后，浏览器访问 `http://localhost:8080`
2. 在任务管理页面可以创建新任务，配置 Cron 表达式、处理器等
3. 在工作流管理页面可以创建 DAG 工作流
4. 在任务实例页面可以查看任务执行情况和日志
5. 在实时监控页面可以连接 WebSocket 监听任务事件

## 🔖 TODO
1. 支持秒级任务调度
2. api 及 Hazelcast 节点安全性
3. ~~新增 web 页面~~ ✅ 已完成
