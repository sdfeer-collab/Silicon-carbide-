package com.mindplus.optimizer.coordinator;

import com.mindplus.optimizer.communication.IPCChannel;
import com.mindplus.optimizer.config.ModConfig;
import com.mindplus.optimizer.process.PortCleaner;
import com.mindplus.optimizer.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenerationCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("GenerationCoordinator");
    
    private final ProcessManager processManager;
    private IPCChannel structureChannel;
    private IPCChannel terrainChannel;
    private IPCChannel biomeChannel;
    private IPCChannel entityChannel;
    
    public GenerationCoordinator(ProcessManager processManager) {
        this.processManager = processManager;
    }
    
    public void initialize() {
        ModConfig config = ModConfig.INSTANCE;

        // 清理可能被占用的端口
        LOGGER.info("Checking for port conflicts...");
        cleanupPorts(config);

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

        if (config.generation.enableStructureGenerator && structureChannel == null) {
            structureChannel = new IPCChannel(ZMQ.REQ,
                "tcp://" + config.network.host + ":" + config.generation.structureGeneratorPort);
            structureChannel.connect();
            LOGGER.info("Connected to structure generator on port {}", config.generation.structureGeneratorPort);
        }

        if (config.generation.enableTerrainGenerator && terrainChannel == null) {
            terrainChannel = new IPCChannel(ZMQ.REQ,
                "tcp://" + config.network.host + ":" + config.generation.terrainGeneratorPort);
            terrainChannel.connect();
            LOGGER.info("Connected to terrain generator on port {}", config.generation.terrainGeneratorPort);
        }

        if (config.generation.enableBiomeGenerator && biomeChannel == null) {
            biomeChannel = new IPCChannel(ZMQ.REQ,
                "tcp://" + config.network.host + ":" + config.generation.biomeGeneratorPort);
            biomeChannel.connect();
            LOGGER.info("Connected to biome generator on port {}", config.generation.biomeGeneratorPort);
        }

        if (config.generation.enableEntitySpawner && entityChannel == null) {
            entityChannel = new IPCChannel(ZMQ.REQ,
                "tcp://" + config.network.host + ":" + config.generation.entitySpawnerPort);
            entityChannel.connect();
            LOGGER.info("Connected to entity spawner on port {}", config.generation.entitySpawnerPort);
        }
    }

    private void cleanupPorts(ModConfig config) {
        List<Integer> portsToCheck = new ArrayList<>();

        if (config.generation.enableStructureGenerator) {
            portsToCheck.add(config.generation.structureGeneratorPort);
        }
        if (config.generation.enableTerrainGenerator) {
            portsToCheck.add(config.generation.terrainGeneratorPort);
        }
        if (config.generation.enableBiomeGenerator) {
            portsToCheck.add(config.generation.biomeGeneratorPort);
        }
        if (config.generation.enableEntitySpawner) {
            portsToCheck.add(config.generation.entitySpawnerPort);
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
    
    private void startWorkerProcesses() {
        ModConfig config = ModConfig.INSTANCE;
        List<String> args = new ArrayList<>();

        LOGGER.info("Starting generation worker processes...");

        if (config.generation.enableStructureGenerator) {
            processManager.startProcess("structure-generator",
                "com.mindplus.optimizer.workers.StructureGenerator", args);
        }

        if (config.generation.enableTerrainGenerator) {
            processManager.startProcess("terrain-generator",
                "com.mindplus.optimizer.workers.TerrainGenerator", args);
        }

        if (config.generation.enableBiomeGenerator) {
            processManager.startProcess("biome-generator",
                "com.mindplus.optimizer.workers.BiomeGenerator", args);
        }

        if (config.generation.enableEntitySpawner) {
            processManager.startProcess("entity-spawner",
                "com.mindplus.optimizer.workers.EntitySpawner", args);
        }

        LOGGER.info("Generation worker processes started");
    }
    
    public CompletableFuture<String> generateChunk(String chunkX, String chunkZ) {
        return CompletableFuture.supplyAsync(() -> {
            ensureChannelsConnected();
            ModConfig config = ModConfig.INSTANCE;
            String chunkData = chunkX + "," + chunkZ;

            if (config.generation.enableBiomeGenerator && biomeChannel != null) {
                biomeChannel.send(chunkData.getBytes());
                byte[] biomeResult = biomeChannel.receive();
            }

            if (config.generation.enableTerrainGenerator && terrainChannel != null) {
                terrainChannel.send(chunkData.getBytes());
                byte[] terrainResult = terrainChannel.receive();
            }

            if (config.generation.enableStructureGenerator && structureChannel != null) {
                structureChannel.send(chunkData.getBytes());
                byte[] structureResult = structureChannel.receive();
            }

            if (config.generation.enableEntitySpawner && entityChannel != null) {
                entityChannel.send(chunkData.getBytes());
                byte[] entityResult = entityChannel.receive();
            }

            LOGGER.info("Chunk generation completed: {}", chunkData);
            return "GENERATED";
        });
    }
    
    public byte[] sendTaskToWorker(String workerName, byte[] taskData) {
        ensureChannelsConnected();
        IPCChannel channel = null;

        switch (workerName) {
            case "structure-generator":
                channel = structureChannel;
                break;
            case "terrain-generator":
                channel = terrainChannel;
                break;
            case "biome-generator":
                channel = biomeChannel;
                break;
            case "entity-spawner":
                channel = entityChannel;
                break;
            default:
                LOGGER.warn("Unknown worker: {}", workerName);
                return null;
        }

        if (channel != null) {
            try {
                channel.send(taskData);
                byte[] response = channel.receive();
                return response;
            } catch (Exception e) {
                LOGGER.error("Failed to send task to {}", workerName, e);
                return null;
            }
        }

        return null;
    }
    
    public void shutdown() {
        processManager.stopAll();
        
        if (structureChannel != null) {
            structureChannel.close();
        }
        if (terrainChannel != null) {
            terrainChannel.close();
        }
        if (biomeChannel != null) {
            biomeChannel.close();
        }
        if (entityChannel != null) {
            entityChannel.close();
        }
    }
}