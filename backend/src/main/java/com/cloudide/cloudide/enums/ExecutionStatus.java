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
    QUEUE_FULL,
    SYSTEM_ERROR,
    STOPPED,
    FAILED
}
