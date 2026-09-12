import { apiClient } from './client';
import {
  BackendFileResponse,
  FileCreateRequest,
  FileUpdateRequest,
} from '../types/api';

export const fileApi = {
  getFiles(projectId: string | number): Promise<BackendFileResponse[]> {
    return apiClient<BackendFileResponse[]>(`/projects/${projectId}/files`, {
      method: 'GET',
    });
  },

  getFile(projectId: string | number, fileId: string | number): Promise<BackendFileResponse> {
    return apiClient<BackendFileResponse>(`/projects/${projectId}/files/${fileId}`, {
      method: 'GET',
    });
  },

  createFile(projectId: string | number, data: FileCreateRequest): Promise<BackendFileResponse> {
    return apiClient<BackendFileResponse>(`/projects/${projectId}/files`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateFile(
    projectId: string | number,
    fileId: string | number,
    data: FileUpdateRequest
  ): Promise<BackendFileResponse> {
    return apiClient<BackendFileResponse>(`/projects/${projectId}/files/${fileId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteFile(projectId: string | number, fileId: string | number): Promise<void> {
    return apiClient<void>(`/projects/${projectId}/files/${fileId}`, {
      method: 'DELETE',
    });
  },
};
