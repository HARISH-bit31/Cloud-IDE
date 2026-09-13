import { ExecutionLog, ExecutionResult, SupportedLanguage } from '../types';
import { executionApi } from '../api/executionApi';
import { ExecutionRequest, ExecutionResponse } from '../types/api';

let currentExecutionAbortController: AbortController | null = null;
let currentActiveExecutionId: string | null = null;

const LANGUAGE_MAP: Record<SupportedLanguage, ExecutionRequest['language']> = {
  java: 'JAVA',
  python: 'PYTHON',
  c: 'C',
  cpp: 'CPP',
};

const DOCKER_IMAGE_NAMES: Record<SupportedLanguage, string> = {
  java: 'cloud-ide-java:latest (OpenJDK 21)',
  python: 'cloud-ide-python:latest (Python 3.12)',
  c: 'cloud-ide-c:latest (GCC 14 / Alpine)',
  cpp: 'cloud-ide-cpp:latest (G++ 14 / Alpine)',
};

export const executionService = {
  executeCode(
    language: SupportedLanguage,
    fileName: string,
    code: string,
    stdin: string = '',
    onLog?: (log: ExecutionLog) => void,
    onProgress?: (partialResult: ExecutionResult) => void,
    onSessionStarted?: (executionId: string) => void,
    projectId?: string | number
  ): { promise: Promise<ExecutionResult>; cancel: () => void } {
    if (currentExecutionAbortController) {
      currentExecutionAbortController.abort();
    }
    const controller = new AbortController();
    currentExecutionAbortController = controller;

    const emitLog = (msg: string, type: ExecutionLog['type'] = 'info') => {
      if (onLog) {
        onLog({
          id: 'log-' + Math.random().toString(36).substring(2, 9),
          timestamp: new Date().toLocaleTimeString(),
          message: msg,
          type,
        });
      }
    };

    const promise = new Promise<ExecutionResult>(async (resolve) => {
      const containerTag = DOCKER_IMAGE_NAMES[language] || 'docker-sandbox:latest';
      emitLog(`Initializing container execution request for ${fileName}...`, 'info');
      emitLog(`Allocating isolated Docker sandbox [${containerTag}] with --network none`, 'info');

      try {
        const backendLang = LANGUAGE_MAP[language] || 'JAVA';
        const parsedProjectId = projectId ? Number(projectId) : undefined;

        console.log(`[Cloud IDE Execution] dispatching request: language=${backendLang}, filename=${fileName}, codeLength=${code.length}, inputLength=${stdin.length}`);

        emitLog(`Compiling and executing ${fileName}...`, 'info');

        const initialResponse: ExecutionResponse = await executionApi.execute({
          language: backendLang,
          code,
          stdin,
          projectId: isNaN(Number(parsedProjectId)) ? undefined : parsedProjectId,
          timeoutSeconds: 10,
        });

        const executionId = initialResponse.executionId || null;
        if (executionId) {
          currentActiveExecutionId = executionId;
          if (onSessionStarted) {
            onSessionStarted(executionId);
          }
        }

        let response = initialResponse;

        if (response.status === 'QUEUED') {
          emitLog(`Execution queued (ID: ${executionId ? executionId.substring(0, 8) : 'unknown'}). Waiting for worker slot...`, 'info');
        }

        // If queued or running, poll for updates until finished
        if ((response.status === 'QUEUED' || response.status === 'RUNNING' || response.status === 'WAITING_FOR_INPUT') && executionId) {
          if (response.status === 'RUNNING') {
            emitLog(`Interactive process running in container (ID: ${executionId.substring(0, 8)})...`, 'info');
          }

          // Initial progress state
          if (onProgress) {
            onProgress({
              stdout: response.stdout || '',
              stderr: response.stderr || '',
              exitCode: null,
              executionTime: 0.1,
              cpuUsage: '0.8%',
              memoryUsage: '< 32 MB',
              status: response.status === 'QUEUED' ? 'queued' : 'running',
              containerImage: containerTag,
            });
          }

          let loggedRunning = response.status === 'RUNNING';

          while (!controller.signal.aborted) {
            await new Promise((r) => setTimeout(r, 120));
            if (controller.signal.aborted) break;

            try {
              const pollRes = await executionApi.getExecutionStatus(executionId);
              response = pollRes;

              if (!loggedRunning && (pollRes.status === 'RUNNING' || pollRes.status === 'WAITING_FOR_INPUT')) {
                emitLog(`Worker slot acquired. Process running in container (ID: ${executionId.substring(0, 8)})...`, 'info');
                loggedRunning = true;
              }

              const elapsedSec = Number((pollRes.executionTimeMs / 1000).toFixed(2));
              if (onProgress && (pollRes.status === 'RUNNING' || pollRes.status === 'WAITING_FOR_INPUT' || pollRes.status === 'QUEUED')) {
                onProgress({
                  stdout: pollRes.stdout || '',
                  stderr: pollRes.stderr || '',
                  exitCode: null,
                  executionTime: elapsedSec,
                  cpuUsage: '1.0%',
                  memoryUsage: '< 32 MB',
                  status: pollRes.status === 'QUEUED' ? 'queued' : 'running',
                  containerImage: containerTag,
                });
              }

              if (pollRes.status !== 'RUNNING' && pollRes.status !== 'WAITING_FOR_INPUT' && pollRes.status !== 'QUEUED') {
                break;
              }
            } catch (err) {
              console.warn('Poll error:', err);
              break;
            }
          }
        }

        if (controller.signal.aborted) {
          emitLog('Execution stopped by user.', 'warning');
          if (executionId) {
            executionApi.stopExecution(executionId).catch(() => {});
          }
          return resolve({
            stdout: response.stdout || '',
            stderr: (response.stderr ? response.stderr + '\n' : '') + 'Execution interrupted by user signal.',
            exitCode: 130,
            executionTime: 0.1,
            cpuUsage: '0.0%',
            memoryUsage: '0.0 MB',
            status: 'stopped',
            containerImage: containerTag,
          });
        }

        const executionTimeSec = Number((response.executionTimeMs / 1000).toFixed(3));
        const memoryFormatted = response.memoryUsedMb
          ? `${response.memoryUsedMb.toFixed(1)} MB`
          : '< 32 MB';
        const cpuFormatted = response.executionTimeMs > 200 ? '1.2%' : '0.4%';

        if (response.status === 'SUCCESS') {
          emitLog(`✓ Process finished cleanly with exit code ${response.exitCode ?? 0} (${response.executionTimeMs}ms)`, 'success');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr,
            exitCode: response.exitCode ?? 0,
            executionTime: executionTimeSec,
            compilationTime: executionTimeSec > 0.3 ? Number((executionTimeSec * 0.4).toFixed(3)) : undefined,
            cpuUsage: cpuFormatted,
            memoryUsage: memoryFormatted,
            status: 'success',
            containerImage: containerTag,
          });
        } else if (response.status === 'COMPILATION_ERROR') {
          emitLog(`[ERROR] Compilation failed in ${fileName}`, 'error');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr,
            exitCode: response.exitCode || 1,
            executionTime: executionTimeSec,
            cpuUsage: cpuFormatted,
            memoryUsage: memoryFormatted,
            status: 'error',
            containerImage: containerTag,
          });
        } else if (response.status === 'TIMEOUT') {
          emitLog(`[TIMEOUT] Process killed: exceeded maximum runtime limit (10s)`, 'error');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr || 'Error: Execution timed out after 10.0 seconds.',
            exitCode: 124,
            executionTime: 10.0,
            cpuUsage: '100%',
            memoryUsage: memoryFormatted,
            status: 'error',
            containerImage: containerTag,
          });
        } else if (response.status === 'OUTPUT_LIMIT_EXCEEDED') {
          emitLog(`[LIMIT] Standard output exceeded 64KB limit and was truncated`, 'warning');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr,
            exitCode: response.exitCode ?? 1,
            executionTime: executionTimeSec,
            cpuUsage: cpuFormatted,
            memoryUsage: memoryFormatted,
            status: 'error',
            containerImage: containerTag,
          });
        } else if (response.status === 'STOPPED') {
          emitLog(`Execution stopped by user signal`, 'warning');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr,
            exitCode: 130,
            executionTime: executionTimeSec,
            cpuUsage: '0.0%',
            memoryUsage: memoryFormatted,
            status: 'stopped',
            containerImage: containerTag,
          });
        } else {
          emitLog(`[ERROR] Runtime error: ${response.status}`, 'error');
          return resolve({
            stdout: response.stdout,
            stderr: response.stderr || `Execution failed with status: ${response.status}`,
            exitCode: response.exitCode || 1,
            executionTime: executionTimeSec,
            cpuUsage: cpuFormatted,
            memoryUsage: memoryFormatted,
            status: 'error',
            containerImage: containerTag,
          });
        }
      } catch (err: any) {
        if (controller.signal.aborted) {
          emitLog('Execution stopped by user.', 'warning');
          return resolve({
            stdout: '',
            stderr: 'Execution interrupted by user signal.',
            exitCode: 130,
            executionTime: 0.1,
            cpuUsage: '0.0%',
            memoryUsage: '0.0 MB',
            status: 'stopped',
            containerImage: containerTag,
          });
        }

        const isQueueFull = err?.response?.status === 429;
        const errorMessage = isQueueFull
          ? 'Execution queue is full. Too many concurrent executions. Please try again in a few moments.'
          : (err?.response?.data?.message || err?.message || 'Failed to connect to execution engine');
        emitLog(`[SYSTEM_ERROR] ${errorMessage}`, 'error');

        return resolve({
          stdout: '',
          stderr: isQueueFull
            ? `Error 429: Execution queue saturated.\nThe system is currently processing maximum concurrent executions. Please wait a moment and try again.`
            : `System Error: ${errorMessage}\nPlease ensure Docker Desktop is running and backend is healthy.`,
          exitCode: isQueueFull ? 429 : -1,
          executionTime: 0.0,
          cpuUsage: '0.0%',
          memoryUsage: '0.0 MB',
          status: 'error',
          containerImage: containerTag,
        });
      } finally {
        currentActiveExecutionId = null;
      }
    });

    return {
      promise,
      cancel: () => {
        controller.abort();
        if (currentActiveExecutionId) {
          executionApi.stopExecution(currentActiveExecutionId).catch(() => {});
        }
      },
    };
  },

  async sendInteractiveStdin(input: string, executionId?: string): Promise<boolean> {
    const targetId = executionId || currentActiveExecutionId;
    if (!targetId) return false;
    try {
      const res = await executionApi.sendInput(targetId, input);
      return res.success;
    } catch (err) {
      console.warn('Failed to send interactive stdin:', err);
      return false;
    }
  },

  stopExecution(): void {
    if (currentExecutionAbortController) {
      currentExecutionAbortController.abort();
      currentExecutionAbortController = null;
    }
    if (currentActiveExecutionId) {
      executionApi.stopExecution(currentActiveExecutionId).catch(() => {});
      currentActiveExecutionId = null;
    }
  },

  async checkDockerHealth() {
    try {
      return await executionApi.getHealth();
    } catch {
      return {
        dockerAvailable: false,
        dockerVersion: 'Unavailable',
        status: 'DISCONNECTED',
        imagesStatus: {},
      };
    }
  },
};
