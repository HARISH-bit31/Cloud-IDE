import { apiClient } from './client';
import {
  BackendProjectResponse,
  ProjectCreateRequest,
  ProjectUpdateRequest,
} from '../types/api';

export const projectApi = {
  getProjects(): Promise<BackendProjectResponse[]> {
    return apiClient<BackendProjectResponse[]>('/projects', {
      method: 'GET',
    });
  },

  getProject(id: string | number): Promise<BackendProjectResponse> {
    return apiClient<BackendProjectResponse>(`/projects/${id}`, {
      method: 'GET',
    });
  },

  createProject(data: ProjectCreateRequest): Promise<BackendProjectResponse> {
    return apiClient<BackendProjectResponse>('/projects', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateProject(id: string | number, data: ProjectUpdateRequest): Promise<BackendProjectResponse> {
    return apiClient<BackendProjectResponse>(`/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  deleteProject(id: string | number): Promise<void> {
    return apiClient<void>(`/projects/${id}`, {
      method: 'DELETE',
    });
  },
};
