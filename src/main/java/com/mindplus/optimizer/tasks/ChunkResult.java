package com.mindplus.optimizer.tasks;

public class ChunkResult {
    private final boolean success;
    private final String message;
    
    public ChunkResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    
    public String serialize() {
        return success + ":" + message;
    }
    
    public static ChunkResult deserialize(String data) {
        int colonIndex = data.indexOf(':');
        boolean success = Boolean.parseBoolean(data.substring(0, colonIndex));
        String message = data.substring(colonIndex + 1);
        return new ChunkResult(success, message);
    }
}