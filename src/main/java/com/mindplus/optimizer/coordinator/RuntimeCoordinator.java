package com.mindplus.optimizer.coordinator;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.generator.WorldGenerator;
import com.mindplus.optimizer.preloader.ChunkPreloader;
import com.mindplus.optimizer.renderer.RenderOptimizer;
import com.mindplus.optimizer.process.PortCleaner;
import com.mindplus.optimizer.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;

public class RuntimeCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("RuntimeCoordinator");
    
    private final ProcessManager processManager;
    private IPCChannel aiChannel;
    private ChunkPreloader chunkPreloader;
    private WorldGenerator worldGenerator;
    private RenderOptimizer renderOptimizer;
    
    public RuntimeCoordinator(ProcessManager processManager) {
        this.processManager = processManager;
    }
    
    public void initialize() {
        ModConfig config = ModConfig.INSTANCE;

        // 清理可能被占用的端口
        LOGGER.info("Checking for port conflicts...");
        cleanupPorts(config);

        // 初始化区块预加载器
        if (config.runtime.enableChunkPreloader) {
            chunkPreloader = new ChunkPreloader();
            // 注意：需要在服务器启动后才能初始化
            LOGGER.info("Chunk preloader created (will be initialized when server starts)");
        }

        // 初始化世界生成器
        worldGenerator = new WorldGenerator();
        LOGGER.info("World generator created (will be initialized when server starts)");

        // 渲染优化器只在客户端初始化时创建
        LOGGER.info("Render optimizer will be created on client initialization");

        // 音频处理器暂时不实现，因为涉及复杂的音频处理
        if (config.runtime.enableAudioProcessor) {
            LOGGER.info("Audio processor is not yet implemented");
        }

        // 先启动工作进程（它们会 bind 端口）
        startWorkerProcesses();

        // 等待工作进程启动并绑定端口
        try {
            Thread.sleep(1000); // 等待 1 秒让工作进程完全启动
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for workers to start", e);
        }

        // 延迟连接到工作进程，在第一次使用时才建立连接
        // 这样可以避免端口冲突问题
        LOGGER.info("Worker processes started, channels will be connected on first use");
    }

    /**
     * 延迟初始化通道连接
     */
    private void ensureChannelsConnected() {
        ModConfig config = ModConfig.INSTANCE;

        if (config.runtime.enableAIProcessor && aiChannel == null) {
            aiChannel = new IPCChannel(ZMQ.PUSH, "tcp://" + config.network.host + ":" + config.runtime.aiProcessorPort);
            aiChannel.connect();
            LOGGER.info("AI processor channel connected on port {}", config.runtime.aiProcessorPort);
        }
    }

    private void cleanupPorts(ModConfig config) {
        List<Integer> portsToCheck = new ArrayList<>();

        if (config.runtime.enableAIProcessor) {
            portsToCheck.add(config.runtime.aiProcessorPort);
        }
        if (config.runtime.enableChunkPreloader) {
            portsToCheck.add(config.runtime.chunkPreloaderPort);
        }
        if (config.runtime.enableAudioProcessor) {
            portsToCheck.add(config.runtime.audioProcessorPort);
        }
        if (config.runtime.enableRenderProcess) {
            portsToCheck.add(config.runtime.renderProcessPort);
        }

        if (!portsToCheck.isEmpty()) {
            int[] portsArray = portsToCheck.stream().mapToInt(i -> i).toArray();
            boolean success = PortCleaner.cleanupPorts(portsArray);
            if (success) {
                LOGGER.info("Port cleanup completed successfully");
            } else {
                LOGGER.warn("Some ports could not be cleaned, workers may fail to start");
            }
        }
    }
    
    public void setChunkPreloaderServer(Object server) {
        if (chunkPreloader != null && server != null) {
            chunkPreloader.initialize(server);
            LOGGER.info("Chunk preloader initialized with server");
        }

        if (worldGenerator != null && server != null) {
            worldGenerator.initialize(server);
            LOGGER.info("World generator initialized with server");
        }
    }

    public void setRenderOptimizerClient(Object client) {
        if (renderOptimizer == null) {
            renderOptimizer = new RenderOptimizer();
            LOGGER.info("Render optimizer created");
        }
        if (renderOptimizer != null && client != null) {
            renderOptimizer.initialize(client);
            LOGGER.info("Render optimizer initialized with client");
        }
    }
    
    private void startWorkerProcesses() {
        ModConfig config = ModConfig.INSTANCE;
        List<String> args = new ArrayList<>();

        LOGGER.info("Starting runtime worker processes...");

        if (config.runtime.enableAIProcessor) {
            processManager.startProcess("ai-processor",
                "com.mindplus.optimizer.workers.AIProcessor", args);
        }

        if (config.runtime.enableChunkPreloader) {
            processManager.startProcess("chunk-preloader",
                "com.mindplus.optimizer.workers.ChunkPreloader", args);
        }

        // 启动世界生成器工作进程
        processManager.startProcess("world-generator",
            "com.mindplus.optimizer.workers.WorldGeneratorWorker", args);

        // 启动多渲染器工作进程（支持 Vulkan、DirectX 12、OpenGL、软件渲染）
        if (config.runtime.enableRenderProcess) {
            List<String> renderArgs = new ArrayList<>();
            renderArgs.add(config.runtime.rendererType); // 渲染器类型
            renderArgs.add(String.valueOf(config.runtime.renderWidth)); // 渲染宽度
            renderArgs.add(String.valueOf(config.runtime.renderHeight)); // 渲染高度

            processManager.startProcess("multi-renderer",
                "com.mindplus.optimizer.workers.MultiRendererProcess", renderArgs);
        } else {
            // 降级使用旧版渲染工作进程
            processManager.startProcess("render-worker",
                "com.mindplus.optimizer.workers.RenderWorker", args);
        }

        if (config.runtime.enableAudioProcessor) {
            processManager.startProcess("audio-processor",
                "com.mindplus.optimizer.workers.AudioProcessor", args);
        }

        LOGGER.info("Runtime worker processes started");
    }
    
    public void sendAITaskAsync(byte[] taskData) {
        ensureChannelsConnected();
        if (aiChannel != null) {
            try {
                aiChannel.send(taskData, ZMQ.NOBLOCK);
            } catch (Exception e) {
                LOGGER.debug("Failed to send AI task", e);
            }
        }
    }
    
    public ChunkPreloader getChunkPreloader() {
        return chunkPreloader;
    }
    
    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
    
    public RenderOptimizer getRenderOptimizer() {
        return renderOptimizer;
    }
    
    public void shutdown() {
        processManager.stopAll();
        
        if (aiChannel != null) {
            aiChannel.close();
        }
        if (chunkPreloader != null) {
            chunkPreloader.shutdown();
        }
        if (worldGenerator != null) {
            worldGenerator.shutdown();
        }
        if (renderOptimizer != null) {
            renderOptimizer.shutdown();
        }
    }
}