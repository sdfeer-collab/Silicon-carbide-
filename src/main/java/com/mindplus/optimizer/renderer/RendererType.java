package com.mindplus.optimizer.renderer;

/**
 * 支持的渲染器类型
 */
public enum RendererType {
    OPENGL("OpenGL", "OpenGL 渲染器"),
    VULKAN("Vulkan", "Vulkan 高性能渲染器"),
    DIRECTX_12("DirectX12", "DirectX 12 渲染器"),
    SOFTWARE("Software", "软件渲染器（CPU）");

    private final String name;
    private final String description;

    RendererType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static RendererType fromString(String name) {
        for (RendererType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return OPENGL; // 默认返回 OpenGL
    }
}