# 项目计划：Rust-based Connected Car Data Platform

## 目标
本项目旨在构建一个端到端的互联汽车数据平台，涵盖数据采集、存储、处理、分析和可视化。

## 当前完成状态

### 阶段 0: 环境与基础设施搭建 (已完成)
*   项目结构初始化：
    *   在 `backend/` 中初始化 Cargo workspace，并创建 `app` crate。
    *   在 `frontend/` 中初始化 Vue 3 + TypeScript + Vite 项目。
*   Docker Compose 环境配置：
    *   在 `docker/` 目录中创建 `docker-compose.yml` 文件，并定义了 PostgreSQL 15、InfluxDB 2.x、EMQX 5、Zookeeper、Kafka、Flink JobManager 和 Flink TaskManager 服务。
*   验证基础设施：
    *   由于用户环境限制，跳过了本地 Docker 服务的运行和验证。

### 阶段 1: 数据接收与原始存储 (打通端到端) (进行中)
*   后端 Crate 结构：
    *   在 `backend/` workspace 中创建 `domain`, `persistence`, `data_pipeline` library crates。
*   领域模型 (`domain` crate)：
    *   在 `domain/src/models.rs` 中定义 `RawDataPoint` struct。
    *   在 `domain/src/lib.rs` 中公开 `models` 模块。
    *   在 `domain/Cargo.toml` 中添加 `serde` 和 `chrono` 依赖。
*   持久化层 (`persistence` crate)：
    *   在 `persistence` 中添加 `influxdb`、`tokio`、`domain`、`async-trait` 和 `chrono` 作为依赖。
    *   创建 `InfluxDBRepository` struct。
    *   定义 `InfluxDBRepositoryTrait` 并实现 `save_raw_data` 方法。
*   数据管道 (`data_pipeline` crate)：
    *   在 `data_pipeline` 中添加 `rumqttc`, `rdkafka-rust`, `serde_json` 作为依赖。
    *   创建 `MqttConsumer` 模块并实现连接和订阅功能。
    *   实现 MQTT 消息解析为 `RawDataPoint`。
    *   创建 `KafkaProducer` 模块。
    *   实现 MQTT 消费者转发数据到 Kafka。
    *   创建 `KafkaConsumer` 模块，消费 Kafka 并写入 InfluxDB。
*   主应用 (`app` crate)：
    *   在 `app/src/main.rs` 中编写 async main 函数。
    *   初始化 `InfluxDBRepository` 和 `KafkaConsumer`。
    *   Spawn `KafkaConsumer` 的主循环。
    *   Spawn 模拟 MQTT 客户端发送样本数据的任务。

## 开发上下文

在尝试运行 `cargo run -p app` 验证后端时，遇到了以下问题：

1.  **CMake 未找到错误**：
    *   尽管用户已确认安装 CMake，但 Rust 的构建系统仍然无法找到 `cmake` 命令。这可能是因为 CMake 没有被正确添加到系统的 PATH 中，或者 Rust 的构建环境没有正确刷新。
2.  **Rust 编译错误**：
    *   **`domain::RawDataPoint` 无法解析**：已通过在 `domain/src/lib.rs` 中公开 `models` 模块，并在 `domain/Cargo.toml` 中添加 `serde` 和 `chrono` 依赖解决。
    *   **`async_trait` 警告**：已通过在 `persistence/Cargo.toml` 中添加 `async-trait` 依赖，并在 `impl InfluxDBRepository` 块上添加 `#[async_trait]` 宏解决。
    *   **`influxdb` 库使用问题**：
        *   `Client::new` 参数错误：已修正 `InfluxDBRepository::new` 的签名和 `Client::new` 的调用，使其接受 `url` 和 `token`。
        *   `WriteQuery::new` 和 `Query::write_query` 使用错误：已修正 `save_raw_data` 方法中 `Query` 对象的构建方式，并确保 `influxdb::Query` 被正确导入。
        *   `Timestamp: From<i64>` 错误：已将 `RawDataPoint` 中的 `timestamp` 类型从 `i64` 改为 `chrono::DateTime<Utc>`，并在 `persistence/Cargo.toml` 中添加 `chrono` 依赖。
    *   **当前阻塞点**：
        *   CMake 未找到的问题仍然存在，导致 `rdkafka-sys` 无法编译。
        *   `persistence` crate 仍然存在编译错误，主要是 `Timestamp` 类型转换和 `Query` trait 的使用问题。尽管已进行多次修正和 `cargo clean`/`cargo update`，但错误依然存在，这可能表明对 `influxdb` 库的理解或使用方式仍有偏差。

## TODO List

```
[x] 阶段 0: 环境与基础设施搭建
[x] 项目结构初始化
[x] 在 `backend/` 中初始化 Cargo workspace，并创建 `app` crate。
[x] 在 `frontend/` 中初始化 Vue 3 + TypeScript + Vite 项目。
[x] Docker Compose 环境配置
[x] 在 `docker/` 目录中创建 `docker-compose.yml` 文件。
[x] 在 `docker-compose.yml` 中定义 PostgreSQL 15 服务。
[x] 在 `docker-compose.yml` 中定义 InfluxDB 2.x 服务。
[x] 在 `docker-compose.yml` 中定义 EMQX 5 服务。
[x] 在 `docker-compose.yml` 中定义 Zookeeper 服务。
[x] 在 `docker-compose.yml` 中定义 Kafka 服务。
[x] 在 `docker-compose.yml` 中定义 Flink 服务。
[x] 验证基础设施
[x] 运行 `docker-compose -f docker/docker-compose.yml up -d`。 (已跳过，因为用户环境限制)
[x] 验证所有服务是否正在运行。 (已跳过，因为用户环境限制)
[x] 访问 Flink Dashboard。 (已跳过，因为用户环境限制)
[x] 访问 EMQX Dashboard。 (已跳过，因为用户环境限制)
[x] 阶段 1: 数据接收与原始存储 (打通端到端)
[x] 后端 Crate 结构
[x] 在 `backend/` workspace 中创建 `domain`, `persistence`, `data_pipeline` library crates。
[x] 领域模型 (`domain` crate)
[x] 在 `domain/src/models.rs` 中定义 `RawDataPoint` struct。
[x] 持久化层 (`persistence` crate)
[x] 在 `persistence` 中添加 `influxdb` 和 `tokio` 作为依赖。
[x] 创建 `InfluxDBRepository` struct。
[x] 实现 `save_raw_data` 方法。
[x] 数据管道 (`data_pipeline` crate)
[x] 在 `data_pipeline` 中添加 `rumqttc`, `rdkafka-rust`, `serde_json` 作为依赖。
[x] 创建 `MqttConsumer` 模块并实现连接和订阅功能。
[x] 实现 MQTT 消息解析为 `RawDataPoint`。
[x] 创建 `KafkaProducer` 模块。
[x] 实现 MQTT 消费者转发数据到 Kafka。
[x] 创建 `KafkaConsumer` 模块，消费 Kafka 并写入 InfluxDB。
[x] 主应用 (`app` crate)
[x] 在 `app/src/main.rs` 中编写 async main 函数。
[x] 初始化 `InfluxDBRepository` 和 `KafkaConsumer`。
[x] Spawn `KafkaConsumer` 的主循环。
[x] Spawn 模拟 MQTT 客户端发送样本数据的任务。
[-] 验证
[ ] 运行后端 `cargo run -p app`。
[ ] 使用 MQTTX 或 `mosquitto_pub` 发送 JSON 数据。
[ ] 在 InfluxDB UI 中确认数据。
[ ] 阶段 2: 行程与充电事件的识别
[ ] 领域模型 (`domain` crate)
[ ] 在 `domain/src/models.rs` 中定义 `Drive` 和 `Charge` structs。
[ ] 定义 `VehicleState` 枚举。
[ ] 在 `domain/src/services/state_machine.rs` 中创建 `StateMachine` struct。
[ ] 实现 `process_data_point` 方法。
[ ] 持久化层 (`persistence` crate)
[ ] 在 `persistence` 中添加 `sqlx` 依赖。
[ ] 创建 `DriveRepository` 和 `ChargeRepository` traits。
[ ] 在 `persistence` 中实现这些 traits。
[ ] 实现 `save_drive`, `update_drive` 等方法。
[ ] 使用 `sqlx::migrate!` 管理数据库 schema 迁移。
[ ] Flink 作业 (状态化处理)
[ ] 创建 Flink SQL 脚本。
[ ] 使用 `MATCH_RECOGNIZE` 检测模式。
[ ] 定义 sink table 写入 Kafka topic。
[ ] 提交 Flink job。
[ ] 数据管道 (`data_pipeline` crate)
[ ] 修改 `KafkaConsumer` 订阅 `drive_events` topic。
[ ] 解析事件并使用 `DriveRepository` 保存到 PostgreSQL。
[ ] 验证
[ ] 发送模拟驾驶的 MQTT 消息。
[ ] 在 Flink UI 中观察作业处理。
[ ] 在 PostgreSQL 的 `drives` 表中检查记录。
[ ] 阶段 3: API 服务与前端可视化
[ ] 后端 Crate 结构
[ ] 在 `backend/` workspace 中创建 `api_server` binary crate。
[ ] API 服务 (`api_server` crate)
[ ] 在 `api_server` 中添加 `axum`, `tokio`, `serde` 依赖。
[ ] 创建 `handlers` 模块并实现处理函数。
[ ] 处理函数使用 `DriveRepository` 从 PostgreSQL 获取数据。
[ ] 创建 `routes` 模块并定义路由。
[ ] 在 `main.rs` 中设置 Axum router，共享 DB 连接池，启动服务器。
[ ] 主应用 (`app` crate) 更新
[ ] 修改 `app/src/main.rs` 启动 `api_server`。
[ ] 前端 (`frontend` project)
[ ] 在 `frontend/src/api/` 中创建客户端调用后端 endpoint。
[ ] 创建 Vue 页面 `DrivesLog.vue` 显示行程列表。
[ ] 安装 charting library。
[ ] 创建 `DriveDetails.vue` 页面。
[ ] 后端 details endpoint 查询 PostgreSQL 和 InfluxDB。
[ ] 使用 charting library 可视化数据。
[ ] 验证
[ ] 启动整个应用。
[ ] 在浏览器中打开前端应用，确认行程列表和详情页图表。