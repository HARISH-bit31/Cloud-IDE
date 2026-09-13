package com.cloudide.execution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "execution")
public class ExecutionWorkerProperties {

    private int maxConcurrent = 4;
    private int timeoutSeconds = 10;
    private String memoryLimit = "256m";
    private String cpuLimit = "1.0";
    private int pidsLimit = 64;
    private int maxOutputSize = 65536; // 64 KB
    private boolean networkDisabled = true;

    private String javaImage = "cloud-ide-java:latest";
    private String pythonImage = "cloud-ide-python:latest";
    private String cImage = "cloud-ide-c:latest";
    private String cppImage = "cloud-ide-cpp:latest";

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public String getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(String cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public int getPidsLimit() {
        return pidsLimit;
    }

    public void setPidsLimit(int pidsLimit) {
        this.pidsLimit = pidsLimit;
    }

    public int getMaxOutputSize() {
        return maxOutputSize;
    }

    public void setMaxOutputSize(int maxOutputSize) {
        this.maxOutputSize = maxOutputSize;
    }

    public boolean isNetworkDisabled() {
        return networkDisabled;
    }

    public void setNetworkDisabled(boolean networkDisabled) {
        this.networkDisabled = networkDisabled;
    }

    public String getJavaImage() {
        return javaImage;
    }

    public void setJavaImage(String javaImage) {
        this.javaImage = javaImage;
    }

    public String getPythonImage() {
        return pythonImage;
    }

    public void setPythonImage(String pythonImage) {
        this.pythonImage = pythonImage;
    }

    public String getCImage() {
        return cImage;
    }

    public void setCImage(String cImage) {
        this.cImage = cImage;
    }

    public String getCppImage() {
        return cppImage;
    }

    public void setCppImage(String cppImage) {
        this.cppImage = cppImage;
    }
}
