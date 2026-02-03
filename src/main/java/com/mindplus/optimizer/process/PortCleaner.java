package com.mindplus.optimizer.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger("PortCleaner");

    /**
     * 检查指定端口是否被占用，如果是则结束占用进程
     * @param port 端口号
     * @return true 表示端口已清理（或未被占用），false 表示清理失败
     */
    public static boolean cleanupPort(int port) {
        if (!isPortInUse(port)) {
            LOGGER.debug("Port {} is not in use", port);
            return true;
        }

        LOGGER.warn("Port {} is in use, attempting to kill the process", port);
        return killProcessUsingPort(port);
    }

    /**
     * 检查端口是否被占用
     */
    private static boolean isPortInUse(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                // macOS/Linux
                pb.command("lsof", "-i", ":" + port);
            } else if (os.contains("win")) {
                // Windows
                pb.command("netstat", "-ano", "|", "findstr", ":" + port);
            } else {
                LOGGER.warn("Unsupported OS: {}", os);
                return false;
            }

            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            LOGGER.error("Error checking port {}", port, e);
            return false;
        }
    }

    /**
     * 结束占用指定端口的进程
     */
    private static boolean killProcessUsingPort(int port) {
        try {
            List<Integer> pids = findProcessesUsingPort(port);

            if (pids.isEmpty()) {
                LOGGER.info("No processes found using port {}", port);
                return true;
            }

            for (int pid : pids) {
                if (killProcess(pid)) {
                    LOGGER.info("Successfully killed process {} using port {}", pid, port);
                } else {
                    LOGGER.error("Failed to kill process {} using port {}", pid, port);
                    return false;
                }
            }

            // 等待端口释放
            Thread.sleep(500);
            return !isPortInUse(port);
        } catch (Exception e) {
            LOGGER.error("Error killing process using port {}", port, e);
            return false;
        }
    }

    /**
     * 查找占用指定端口的进程 PID
     */
    private static List<Integer> findProcessesUsingPort(int port) {
        List<Integer> pids = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                // macOS/Linux: lsof -i :5555 -t
                pb.command("lsof", "-t", "-i", ":" + port);
            } else if (os.contains("win")) {
                // Windows: netstat -ano | findstr :5555
                pb.command("cmd", "/c", "netstat -ano | findstr :" + port);
            } else {
                LOGGER.warn("Unsupported OS: {}", os);
                return pids;
            }

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            if (os.contains("win")) {
                // Windows 需要解析 netstat 输出
                Pattern pattern = Pattern.compile(".*?:(\\d+).*");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            int pid = Integer.parseInt(matcher.group(1));
                            pids.add(pid);
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                }
            } else {
                // macOS/Linux 直接输出 PID
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            int pid = Integer.parseInt(line);
                            pids.add(pid);
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            LOGGER.error("Error finding processes using port {}", port, e);
        }

        return pids;
    }

    /**
     * 结束指定 PID 的进程
     */
    private static boolean killProcess(int pid) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                pb.command("kill", "-9", String.valueOf(pid));
            } else if (os.contains("win")) {
                pb.command("taskkill", "/F", "/PID", String.valueOf(pid));
            } else {
                LOGGER.warn("Unsupported OS: {}", os);
                return false;
            }

            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            LOGGER.error("Error killing process {}", pid, e);
            return false;
        }
    }

    /**
     * 清理多个端口
     */
    public static boolean cleanupPorts(int... ports) {
        boolean allSuccess = true;
        for (int port : ports) {
            if (!cleanupPort(port)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }
}
