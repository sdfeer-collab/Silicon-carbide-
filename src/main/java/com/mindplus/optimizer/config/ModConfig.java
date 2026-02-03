package com.mindplus.optimizer.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "mindplus-optimizer")
public class ModConfig implements ConfigData {
    
    @ConfigEntry.Gui.Excluded
    public static ModConfig INSTANCE;
    
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public General general = new General();
    
    @ConfigEntry.Category("generation")
    @ConfigEntry.Gui.TransitiveObject
    public Generation generation = new Generation();
    
    @ConfigEntry.Category("runtime")
    @ConfigEntry.Gui.TransitiveObject
    public Runtime runtime = new Runtime();
    
    @ConfigEntry.Category("network")
    @ConfigEntry.Gui.TransitiveObject
    public Network network = new Network();
    
    public static class General {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean debugLogging = false;
        
        @ConfigEntry.Gui.Tooltip
        public int maxProcesses = 8;
    }
    
    public static class Generation {
        @ConfigEntry.Gui.Tooltip
        public boolean enableStructureGenerator = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableTerrainGenerator = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableBiomeGenerator = true;
        
        @ConfigEntry.Gui.Tooltip
        public boolean enableEntitySpawner = true;
        
        @ConfigEntry.Gui.Tooltip
        public int structureGeneratorPort = 5555;
        
        @ConfigEntry.Gui.Tooltip
        public int terrainGeneratorPort = 5556;
        
        @ConfigEntry.Gui.Tooltip
        public int biomeGeneratorPort = 5557;
        
        @ConfigEntry.Gui.Tooltip
        public int entitySpawnerPort = 5558;
    }
    
    public static class Runtime {
        @ConfigEntry.Gui.Tooltip
        public boolean enableAIProcessor = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableChunkPreloader = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableAudioProcessor = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableRenderProcess = true;

        @ConfigEntry.Gui.Tooltip
        public String rendererType = "Vulkan";

        @ConfigEntry.Gui.Tooltip
        public int renderProcessPort = 5580;

        @ConfigEntry.Gui.Tooltip
        public int aiProcessorPort = 5559;

        @ConfigEntry.Gui.Tooltip
        public int chunkPreloaderPort = 5560;

        @ConfigEntry.Gui.Tooltip
        public int audioProcessorPort = 5561;

        @ConfigEntry.Gui.Tooltip
        public int preloadRadius = 3;

        @ConfigEntry.Gui.Tooltip
        public int preloadThreads = 2;

        @ConfigEntry.Gui.Tooltip
        public int renderWidth = 1920;

        @ConfigEntry.Gui.Tooltip
        public int renderHeight = 1080;
    }
    
    public static class Network {
        @ConfigEntry.Gui.Tooltip
        public String host = "localhost";
        
        @ConfigEntry.Gui.Tooltip
        public int connectionTimeout = 5000;
        
        @ConfigEntry.Gui.Tooltip
        public int retryAttempts = 3;
        
        @ConfigEntry.Gui.Tooltip
        public int retryDelay = 1000;
    }
}