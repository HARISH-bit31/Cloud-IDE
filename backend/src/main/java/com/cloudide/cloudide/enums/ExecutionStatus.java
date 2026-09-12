package com.cloudide.cloudide.enums;

public enum ExecutionStatus {
    QUEUED,
    RUNNING,
    WAITING_FOR_INPUT,
    SUCCESS,
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TIMEOUT,
    OUTPUT_LIMIT_EXCEEDED,
    SYSTEM_ERROR,
    STOPPED
}
