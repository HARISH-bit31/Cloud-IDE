import { apiClient } from './client';
import {
  RegisterRequest,
  LoginRequest,
  LoginResponse,
  AuthUserResponse,
} from '../types/api';

export const authApi = {
  register(data: RegisterRequest): Promise<AuthUserResponse> {
    return apiClient<AuthUserResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: false,
    });
  },

  login(data: LoginRequest): Promise<LoginResponse> {
    return apiClient<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: false,
    });
  },

  getCurrentUser(): Promise<AuthUserResponse> {
    return apiClient<AuthUserResponse>('/auth/me', {
      method: 'GET',
      requiresAuth: true,
    });
  },
};
