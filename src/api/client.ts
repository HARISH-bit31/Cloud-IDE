import { ApiErrorResponse } from '../types/api';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088/api').replace(/\/$/, '');

export const TOKEN_STORAGE_KEY = 'cloud_ide_jwt_token';

export class ApiClientError extends Error {
  status: number;
  errors?: string[];
  data?: any;

  constructor(message: string, status: number, errors?: string[], data?: any) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.errors = errors;
    this.data = data;
  }
}

export const getToken = (): string | null => {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
};

export const setToken = (token: string | null): void => {
  try {
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch (e) {
    console.error('Failed to access localStorage for JWT token', e);
  }
};

export const clearToken = (): void => {
  setToken(null);
};

interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
}

export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { requiresAuth = true, headers: customHeaders, ...restOptions } = options;

  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  const url = `${API_BASE_URL}${cleanEndpoint}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(customHeaders as Record<string, string>),
  };

  const token = getToken();
  if (requiresAuth && token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...restOptions,
      headers,
    });
  } catch (err: any) {
    throw new ApiClientError(
      'Unable to reach backend server. Please check your network or server connection.',
      0
    );
  }

  // 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  let responseData: any = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      responseData = await response.json();
    } catch {
      responseData = null;
    }
  } else {
    try {
      responseData = await response.text();
    } catch {
      responseData = null;
    }
  }

  if (!response.ok) {
    let errorMessage = 'An unexpected error occurred';
    let validationErrors: string[] | undefined;

    if (responseData && typeof responseData === 'object') {
      const errObj = responseData as ApiErrorResponse;
      if (errObj.message) errorMessage = errObj.message;
      if (errObj.errors && Array.isArray(errObj.errors)) {
        validationErrors = errObj.errors;
        if (!errObj.message) {
          errorMessage = validationErrors.join(', ');
        }
      }
    } else if (typeof responseData === 'string' && responseData.trim()) {
      errorMessage = responseData;
    }

    if (response.status === 401) {
      // Broadcast 401 event for auth context to react
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }

    throw new ApiClientError(errorMessage, response.status, validationErrors, responseData);
  }

  return responseData as T;
}
