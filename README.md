# Silicon carbide- Minecraft 多进程优化模组

## 项目概述

这是一个 Minecraft Java 版优化模组，通过多进程架构分离计算密集型任务，提升游戏性能。

## 架构设计

### 世界生成阶段（多进程分离）
- **结构生成进程** (5555端口) - 村庄、遗迹、地牢等复杂结构
- **地形生成进程** (5556端口) - 噪声生成、高度图计算
- **群系生成进程** (5557端口) - 生物群系分布、温度/湿度计算
- **动植物生成进程** (5558端口) - 实体生成、植被分布

### 游玩阶段（多进程分离）
- **AI 演算进程** (5559端口) - 实体 AI、寻路、行为树计算
- **地形预加载进程** (5560端口) - 异步加载周边区块、几何体构建
- **声音播放进程** (5561端口) - 音频解码、3D 音效处理、混音

## 技术栈

- **模组框架**: Fabric 1.20.4
- **进程间通信**: ZeroMQ (jeromq 0.5.4)
- **序列化**: Protocol Buffers 4.26.1
- **配置管理**: Cloth Config 11.1.136
- **构建工具**: Gradle 8.14.4+ (Loom 1.8.13)
- **Java 版本**: 17+
- **Fabric Loader**: 0.15.11
- **Fabric API**: 0.97.3+1.20.4

## 项目结构

```
mindplus-optimizer/
├── src/main/java/com/mindplus/optimizer/
│   ├── MindPlusOptimizer.java              # 主模组类
│   ├── MindPlusOptimizerClient.java        # 客户端入口
│   ├── client/                             # 客户端相关代码
│   │   ├── MindPlusOptimizerClient.java
│   │   └── ModMenuIntegration.java         # Mod Menu 集成
│   ├── process/                            # 进程管理
│   │   └── ProcessManager.java             # 进程管理器
│   ├── communication/                      # 进程间通信
│   │   └── IPCChannel.java                 # ZeroMQ 通信通道
│   ├── config/                             # 配置管理
│   ├── coordinator/                        # 协调器
│   │   ├── GenerationCoordinator.java      # 生成协调器
│   │   └── RuntimeCoordinator.java         # 运行时协调器
│   ├── generator/                          # 生成器
│   ├── mixin/                              # Mixin 钩子
│   ├── preloader/                          # 预加载器
│   ├── renderer/                           # 渲染相关
│   ├── tasks/                              # 任务管理
│   └── workers/                            # 工作进程
│       ├── StructureGenerator.java         # 结构生成器
│       ├── TerrainGenerator.java           # 地形生成器
│       ├── BiomeGenerator.java             # 群系生成器
│       ├── EntitySpawner.java              # 实体生成器
│       ├── AIProcessor.java                # AI 处理器
│       ├── ChunkPreloader.java             # 区块预加载器
│       └── AudioProcessor.java             # 音频处理器
├── src/main/resources/
│   ├── fabric.mod.json                     # 模组配置
│   └── mindplus-optimizer.mixins.json      # Mixin 配置
├── src/main/proto/                         # Protocol Buffers 定义
├── build.gradle.kts                        # 构建配置
├── gradle.properties                       # Gradle 属性
└── settings.gradle.kts                     # Gradle 设置
```

## 构建和运行

### 构建项目

```bash
./gradlew build
```

## 核心功能
### 生成协调器 (GenerationCoordinator)
- 协调世界生成阶段的多个进程
- 并行化生成任务
- 状态同步和错误处理

### 运行时协调器 (RuntimeCoordinator)
- 协调游戏运行时的进程
- 异步处理 AI 计算
- 预加载周边区块
- 独立音频处理
## 性能优化

### 世界生成优化
- 并行化生成任务，充分利用多核 CPU
- 独立进程避免主线程阻塞
- 智能任务调度和负载均衡

### 游玩时优化
- AI 计算独立进程，不影响主线程
- 异步区块预加载，减少加载延迟
- 独立音频进程，降低音频处理开销

## 开发计划
1.实现超级渲染
2.放大
## 许可证

MIT License