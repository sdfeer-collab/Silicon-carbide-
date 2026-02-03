Silicon carbide - Minecraft multi-process optimization mod
Project Overview
This is an optimized mod for Minecraft Java Edition that improves game performance by separating compute-intensive tasks through a multi-process architecture.
Architecture design
World generation stage (multi-process separation)
		Structure Generation Process (Port 5555) - Complex structures such as villages, ruins, dungeons, etc
		Terrain generation process (port 5556) - Noise generation, altitude map calculation
		Swarm Generation Process (Port 5557) - Biome distribution, temperature/humidity calculations
		Flora and Fauna Generation Progress (port 5558) - Entity generation, vegetation distribution
Play Stages (Multi-Process Separation)
		AI Rendering Process (port 5559) - Entity AI, pathfinding, behavior tree calculations
		Terrain Preload Process (port 5560) - Asynchronously loads perimeter blocks, geometry builds
		Sound playback process (port 5561) - audio decoding, 3D sound processing, mixing
Technology stack
		Mod Framework: Fabric 1.20.4
		Inter-process communication: ZeroMQ (jeromq 0.5.4)
		序列化: Protocol Buffers 4.26.1
		Configuration Management: Cloth Config 11.1.136
		Build Tools: Gradle 8.14.4+ (Loom 1.8.13)
		Java version: 17+
		Fabric Loader: 0.15.11
		Fabric API: 0.97.3+1.20.4
Project structure
MindPlus-Optimizer/
├── src/main/java/com/mindplus/optimizer/
│ ├── MindPlusOptimizer.java
│ ├── MindPlusOptimizerClient.java 
│ ├── client/
│ │ ├── MindPlusOptimizerClient.java
│ │ └── ModMenuIntegration.java #Not enabled
│ ├── process/ 
│ │ └── ProcessManager.java
│ ├── communication/ 
│ │ └── IPCChannel.java # ZeroMQ 
│ ├── config/ 
│ ├── coordinator/ 
│ │ ├── GenerationCoordinator.java 
│ │ └── RuntimeCoordinator.java 
│ ├── generator/ 
│ ├── mixin/ # mixin 
│ ├── preloader/
│ ├── renderer/ 
│ ├── tasks/ 
│ └── workers/ 
│ ├── StructureGenerator.java 
│ ├── TerrainGenerator.java 
│ ├── BiomeGenerator.java
│ ├── EntitySpawner.java
│ ├── AIProcessor.java 
│ ├── ChunkPreloader.java 
│ └── AudioProcessor.java 
├── src/main/resources/
│ ├── fabric.mod.json
│ └── mindplus-optimizer.mixins.json # Mixin config
├── src/main/proto/ # Protocol Buffers 
├── build.gradle.kts 
├── gradle.properties # gradle 
└── settings.gradle.kts
Build and run
Build the project
./gradlew build
Core features:
生成协调器 (GenerationCoordinator)
		Coordinate multiple processes in the world generation phase
		Parallelize the generation of tasks
		State synchronization and error handling
RuntimeCoordinator
		Coordinating the process of the game runtime
		Asynchronous processing of AI calculations
		Preload perimeter blocks
		Independent audio processing
Performance optimization
World generation optimization
		Parallelize build tasks to take full advantage of multi-core CPUs
		Independent processes avoid blocking the main thread
		Intelligent task scheduling and load balancing
Optimized while playing
		AI computing processes independently without affecting the main thread
		Asynchronous block preloading reduces loading latency
		Independent audio process reduces audio processing overhead
Development plan
1. Achieve super rendering 2. Upscale
license
MIT License
