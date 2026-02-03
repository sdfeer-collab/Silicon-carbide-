package com.mindplus.optimizer.tasks;

public class ChunkTask {
    private final int chunkX;
    private final int chunkZ;
    private final long worldSeed;
    private final String dimension;
    
    public ChunkTask(int chunkX, int chunkZ, long worldSeed, String dimension) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldSeed = worldSeed;
        this.dimension = dimension;
    }
    
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public long getWorldSeed() { return worldSeed; }
    public String getDimension() { return dimension; }
    
    public String serialize() {
        return chunkX + "," + chunkZ + "," + worldSeed + "," + dimension;
    }
    
    public static ChunkTask deserialize(String data) {
        String[] parts = data.split(",");
        return new ChunkTask(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Long.parseLong(parts[2]),
            parts[3]
        );
    }
}