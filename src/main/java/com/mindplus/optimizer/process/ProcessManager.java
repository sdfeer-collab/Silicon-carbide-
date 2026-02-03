package com.mindplus.optimizer.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class ProcessManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProcessManager");

    private final Map<String, WorkerProcess> processes = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final String modJarPath;

    public ProcessManager() {
        this.modJarPath = getModJarPath();
        LOGGER.info("Mod JAR path: {}", modJarPath);
    }

    private String getModJarPath() {
        try {
            URL jarUrl = ProcessManager.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl != null) {
                Path path = Paths.get(jarUrl.toURI());
                if (path.toString().endsWith(".jar")) {
                    return path.toString();
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to get mod JAR path", e);
        }
        return "";
    }

    public void startProcess(String processId, String mainClass, List<String> args) {
        if (processes.containsKey(processId)) {
            LOGGER.warn("Process {} already exists", processId);
            return;
        }

        try {
            WorkerProcess process = new WorkerProcess(processId, mainClass, args, executor, modJarPath);
            processes.put(processId, process);
            process.start();
            LOGGER.info("Started process: {}", processId);
        } catch (Exception e) {
            LOGGER.error("Failed to start process {}", processId, e);
        }
    }

    public void stopProcess(String processId) {
        WorkerProcess process = processes.get(processId);
        if (process != null) {
            process.stop();
            processes.remove(processId);
            LOGGER.info("Stopped process: {}", processId);
        }
    }

    public void stopAll() {
        processes.keySet().forEach(this::stopProcess);
        executor.shutdown();
    }

    public WorkerProcess getProcess(String processId) {
        return processes.get(processId);
    }

    public static class WorkerProcess {
        private final String processId;
        private final String mainClass;
        private final List<String> args;
        private final ExecutorService executor;
        private final String modJarPath;
        private Process process;
        private boolean running = false;

        public WorkerProcess(String processId, String mainClass, List<String> args, ExecutorService executor, String modJarPath) {
            this.processId = processId;
            this.mainClass = mainClass;
            this.args = args;
            this.executor = executor;
            this.modJarPath = modJarPath;
        }

        public void start() throws IOException {
            List<String> command = new ArrayList<>();
            command.add(System.getProperty("java.home") + "/bin/java");
            command.add("-cp");
            
            // Build classpath: mod JAR + system classpath
            String classpath = System.getProperty("java.class.path");
            if (!modJarPath.isEmpty()) {
                classpath = modJarPath + File.pathSeparator + classpath;
            }
            command.add(classpath);
            
            command.add(mainClass);
            command.addAll(args);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            this.process = pb.start();
            this.running = true;

            executor.submit(this::monitorProcess);
        }

        public void stop() {
            if (process != null && process.isAlive()) {
                process.destroy();
                running = false;
            }
        }

        private void monitorProcess() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("[{}] {}", processId, line);
                }
            } catch (IOException e) {
                LOGGER.error("Error reading output from {}", processId, e);
            }

            int exitCode = process.exitValue();
            LOGGER.warn("Process {} exited with code {}", processId, exitCode);
            running = false;
        }

        public boolean isRunning() {
            return running && process != null && process.isAlive();
        }
    }
}