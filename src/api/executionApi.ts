import { apiClient } from './client';
import { ExecutionRequest, ExecutionResponse, ExecutionHealthResponse, ExecutionInputRequest } from '../types/api';

export const executionApi = {
  execute(data: ExecutionRequest): Promise<ExecutionResponse> {
    return apiClient<ExecutionResponse>('/execute', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    });
  },

  sendInput(executionId: string, input: string): Promise<{ success: boolean; message?: string }> {
    return apiClient<{ success: boolean; message?: string }>(`/execute/${executionId}/input`, {
      method: 'POST',
      body: JSON.stringify({ input } as ExecutionInputRequest),
      requiresAuth: true,
    });
  },

  getExecutionStatus(executionId: string): Promise<ExecutionResponse> {
    return apiClient<ExecutionResponse>(`/execute/${executionId}`, {
      method: 'GET',
      requiresAuth: true,
    });
  },

  stopExecution(executionId: string): Promise<ExecutionResponse> {
    return apiClient<ExecutionResponse>(`/execute/${executionId}/stop`, {
      method: 'POST',
      requiresAuth: true,
    });
  },

  getHealth(): Promise<ExecutionHealthResponse> {
    return apiClient<ExecutionHealthResponse>('/execution/health', {
      method: 'GET',
      requiresAuth: false,
    });
  },
};

