package com.mindplus.optimizer.renderer;

/**
 * 渲染结果
 * 包含区块渲染的数据
 */
public class RenderResult {
    public final int chunkX;
    public final int chunkZ;
    public final byte[] vertexData;  // 顶点数据
    public final byte[] lightData;   // 光照数据
    public final long renderTimeNs;  // 渲染耗时
    public final int triangleCount;  // 三角形数量

    public RenderResult(int chunkX, int chunkZ, byte[] vertexData, byte[] lightData, long renderTimeNs, int triangleCount) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.vertexData = vertexData;
        this.lightData = lightData;
        this.renderTimeNs = renderTimeNs;
        this.triangleCount = triangleCount;
    }

    /**
     * 序列化为字节数组（用于进程间传输）
     */
    public byte[] serialize() {
        // 简单序列化：chunkX(4) + chunkZ(4) + vertexLen(4) + vertexData + lightLen(4) + lightData + renderTimeNs(8) + triangleCount(4)
        int vertexLen = vertexData != null ? vertexData.length : 0;
        int lightLen = lightData != null ? lightData.length : 0;
        int totalLen = 4 + 4 + 4 + vertexLen + 4 + lightLen + 8 + 4;

        byte[] data = new byte[totalLen];
        int offset = 0;

        // chunkX
        data[offset++] = (byte) (chunkX >> 24);
        data[offset++] = (byte) (chunkX >> 16);
        data[offset++] = (byte) (chunkX >> 8);
        data[offset++] = (byte) chunkX;

        // chunkZ
        data[offset++] = (byte) (chunkZ >> 24);
        data[offset++] = (byte) (chunkZ >> 16);
        data[offset++] = (byte) (chunkZ >> 8);
        data[offset++] = (byte) chunkZ;

        // vertexLen
        data[offset++] = (byte) (vertexLen >> 24);
        data[offset++] = (byte) (vertexLen >> 16);
        data[offset++] = (byte) (vertexLen >> 8);
        data[offset++] = (byte) vertexLen;

        // vertexData
        if (vertexLen > 0) {
            System.arraycopy(vertexData, 0, data, offset, vertexLen);
            offset += vertexLen;
        }

        // lightLen
        data[offset++] = (byte) (lightLen >> 24);
        data[offset++] = (byte) (lightLen >> 16);
        data[offset++] = (byte) (lightLen >> 8);
        data[offset++] = (byte) lightLen;

        // lightData
        if (lightLen > 0) {
            System.arraycopy(lightData, 0, data, offset, lightLen);
            offset += lightLen;
        }

        // renderTimeNs
        long rt = renderTimeNs;
        data[offset++] = (byte) (rt >> 56);
        data[offset++] = (byte) (rt >> 48);
        data[offset++] = (byte) (rt >> 40);
        data[offset++] = (byte) (rt >> 32);
        data[offset++] = (byte) (rt >> 24);
        data[offset++] = (byte) (rt >> 16);
        data[offset++] = (byte) (rt >> 8);
        data[offset++] = (byte) rt;

        // triangleCount
        int tc = triangleCount;
        data[offset++] = (byte) (tc >> 24);
        data[offset++] = (byte) (tc >> 16);
        data[offset++] = (byte) (tc >> 8);
        data[offset++] = (byte) tc;

        return data;
    }

    /**
     * 从字节数组反序列化
     */
    public static RenderResult deserialize(byte[] data) {
        int offset = 0;

        // chunkX
        int chunkX = ((data[offset] & 0xFF) << 24) |
                     ((data[offset + 1] & 0xFF) << 16) |
                     ((data[offset + 2] & 0xFF) << 8) |
                     (data[offset + 3] & 0xFF);
        offset += 4;

        // chunkZ
        int chunkZ = ((data[offset] & 0xFF) << 24) |
                     ((data[offset + 1] & 0xFF) << 16) |
                     ((data[offset + 2] & 0xFF) << 8) |
                     (data[offset + 3] & 0xFF);
        offset += 4;

        // vertexLen
        int vertexLen = ((data[offset] & 0xFF) << 24) |
                        ((data[offset + 1] & 0xFF) << 16) |
                        ((data[offset + 2] & 0xFF) << 8) |
                        (data[offset + 3] & 0xFF);
        offset += 4;

        // vertexData
        byte[] vertexData = null;
        if (vertexLen > 0) {
            vertexData = new byte[vertexLen];
            System.arraycopy(data, offset, vertexData, 0, vertexLen);
            offset += vertexLen;
        }

        // lightLen
        int lightLen = ((data[offset] & 0xFF) << 24) |
                       ((data[offset + 1] & 0xFF) << 16) |
                       ((data[offset + 2] & 0xFF) << 8) |
                       (data[offset + 3] & 0xFF);
        offset += 4;

        // lightData
        byte[] lightData = null;
        if (lightLen > 0) {
            lightData = new byte[lightLen];
            System.arraycopy(data, offset, lightData, 0, lightLen);
            offset += lightLen;
        }

        // renderTimeNs
        long renderTimeNs = ((long) (data[offset] & 0xFF) << 56) |
                            ((long) (data[offset + 1] & 0xFF) << 48) |
                            ((long) (data[offset + 2] & 0xFF) << 40) |
                            ((long) (data[offset + 3] & 0xFF) << 32) |
                            ((long) (data[offset + 4] & 0xFF) << 24) |
                            ((long) (data[offset + 5] & 0xFF) << 16) |
                            ((long) (data[offset + 6] & 0xFF) << 8) |
                            ((long) (data[offset + 7] & 0xFF));
        offset += 8;

        // triangleCount
        int triangleCount = ((data[offset] & 0xFF) << 24) |
                            ((data[offset + 1] & 0xFF) << 16) |
                            ((data[offset + 2] & 0xFF) << 8) |
                            (data[offset + 3] & 0xFF);

        return new RenderResult(chunkX, chunkZ, vertexData, lightData, renderTimeNs, triangleCount);
    }
}