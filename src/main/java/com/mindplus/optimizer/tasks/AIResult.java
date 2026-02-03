package com.mindplus.optimizer.tasks;

public class AIResult {
    private final boolean shouldMove;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    
    public AIResult(boolean shouldMove, double targetX, double targetY, double targetZ) {
        this.shouldMove = shouldMove;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }
    
    public boolean shouldMove() { return shouldMove; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }
    
    public String serialize() {
        return shouldMove + "," + targetX + "," + targetY + "," + targetZ;
    }
    
    public static AIResult deserialize(String data) {
        String[] parts = data.split(",");
        return new AIResult(
            Boolean.parseBoolean(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]),
            Double.parseDouble(parts[3])
        );
    }
}