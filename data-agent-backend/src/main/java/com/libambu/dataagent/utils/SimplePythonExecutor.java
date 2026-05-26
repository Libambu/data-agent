package com.libambu.dataagent.utils;

import com.libambu.dataagent.entity.dto.PythonExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Python 代码沙箱执行器。
 * <p>
 * 使用 Docker 容器隔离执行 Python 脚本，保证安全性与依赖一致性。
 */
@Slf4j
public final class SimplePythonExecutor {

    private static final String DEFAULT_DOCKER_IMAGE = "continuumio/anaconda3:latest";
    private static final String DEFAULT_MEMORY_LIMIT = "512m";

    private SimplePythonExecutor() {
    }

    /**
     * 执行 Python 代码。
     *
     * @param pythonCode 完整的 Python 代码字符串
     * @param inputJson  需要通过 stdin 传入的 JSON 数据字符串
     * @return 执行结果
     */
    public static PythonExecutionResult execute(String pythonCode, String inputJson) {
        return execute(pythonCode, inputJson, 60);
    }

    /**
     * 执行 Python 代码。
     *
     * @param pythonCode 完整的 Python 代码字符串
     * @param inputJson  需要通过 stdin 传入的 JSON 数据字符串
     * @param timeoutSec 超时时间（秒）
     * @return 执行结果
     */
    public static PythonExecutionResult execute(String pythonCode, String inputJson, long timeoutSec) {
        File workDir = null;
        try {
            workDir = Files.createTempDirectory("ai_python_exec_").toFile();
            File scriptFile = new File(workDir, "script.py");
            File dataFile = new File(workDir, "input.json");

            // 1. 写入脚本和输入数据
            Files.writeString(scriptFile.toPath(), pythonCode);
            Files.writeString(dataFile.toPath(), inputJson);

            // 2. 强制使用 Docker 沙箱执行
            if (!isDockerAvailable()) {
                return new PythonExecutionResult(false, "",
                        "Docker is required for Python sandbox execution but was not found. Please install/start Docker.");
            }

            String envImage = System.getenv("DATA_AGENT_PYTHON_DOCKER_IMAGE");
            String image = (envImage != null && !envImage.isBlank()) ? envImage.trim() : DEFAULT_DOCKER_IMAGE;
            String envMemory = System.getenv("DATA_AGENT_PYTHON_MEMORY_LIMIT");
            String memoryLimit = (envMemory != null && !envMemory.isBlank()) ? envMemory.trim() : DEFAULT_MEMORY_LIMIT;

            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("run");
            command.add("--rm");
            command.add("-i");
            command.add("--network");
            command.add("none");
            command.add("--cpus");
            command.add("1");
            command.add("--memory");
            command.add(memoryLimit);
            command.add("--pids-limit");
            command.add("128");
            command.add("--security-opt");
            command.add("no-new-privileges");
            command.add("-v");
            command.add(workDir.getAbsolutePath() + ":/work:ro");
            command.add("-w");
            command.add("/work");
            command.add(image);
            command.add("python");
            command.add("/work/script.py");

            PythonExecutionResult dockerResult = runCommand(command, dataFile, timeoutSec);
            if (dockerResult.isSuccess()) {
                return dockerResult;
            } else {
                return new PythonExecutionResult(false, dockerResult.getOutput(),
                        dockerResult.getError() + "\n\nSandbox image: " + image);
            }

        } catch (Exception e) {
            log.error("[SimplePythonExecutor] 执行异常", e);
            return new PythonExecutionResult(false, "", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
        } finally {
            // 3. 清理临时目录
            if (workDir != null) {
                deleteRecursively(workDir);
            }
        }
    }

    private static PythonExecutionResult runCommand(List<String> command, File inputFile, long timeoutSec) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(inputFile);
        Process process = pb.start();

        boolean completed = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            return new PythonExecutionResult(false, "", "Execution timed out after " + timeoutSec + " seconds.");
        }

        int exitCode = process.exitValue();
        String stdout = new String(process.getInputStream().readAllBytes()).trim();
        String stderr = new String(process.getErrorStream().readAllBytes()).trim();

        if (exitCode == 0) {
            return new PythonExecutionResult(true, stdout, stderr);
        } else {
            return new PythonExecutionResult(false, stdout, stderr);
        }
    }

    private static boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
