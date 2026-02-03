package com.mindplus.optimizer.workers;

import com.mindplus.optimizer.communication.IPCChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class BiomeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("BiomeGenerator");
    
    private final IPCChannel channel;
    private volatile boolean running = true;
    
    public BiomeGenerator() {
        this.channel = new IPCChannel(ZMQ.REP, "tcp://*:5557");
    }
    
    public void start() {
        channel.bind();
        LOGGER.info("Biome Generator started");
        
        while (running) {
            byte[] request = channel.receive();
            if (request != null) {
                processRequest(request);
            }
        }
        
        channel.close();
    }
    
    private void processRequest(byte[] request) {
        try {
            String chunkData = new String(request);
            LOGGER.info("Processing biome generation for chunk: {}", chunkData);
            
            byte[] response = generateBiomes(chunkData);
            channel.send(response);
        } catch (Exception e) {
            LOGGER.error("Error processing biome generation", e);
        }
    }
    
    private byte[] generateBiomes(String chunkData) {
        return "BIOMES_GENERATED".getBytes();
    }
    
    public void stop() {
        running = false;
    }
    
    public static void main(String[] args) {
        BiomeGenerator generator = new BiomeGenerator();
        generator.start();
    }
}