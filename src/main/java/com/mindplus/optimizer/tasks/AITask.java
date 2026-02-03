package com.mindplus.optimizer.tasks;

public class AITask {
    private final int entityId;
    private final String entityType;
    private final double posX;
    private final double posY;
    private final double posZ;
    
    public AITask(int entityId, String entityType, double posX, double posY, double posZ) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }
    
    public int getEntityId() { return entityId; }
    public String getEntityType() { return entityType; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPosZ() { return posZ; }
    
    public String serialize() {
        return entityId + "," + entityType + "," + posX + "," + posY + "," + posZ;
    }
    
    public static AITask deserialize(String data) {
        String[] parts = data.split(",");
        return new AITask(
            Integer.parseInt(parts[0]),
            parts[1],
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3]),
            Double.parseDouble(parts[4])
        );
    }
}