import { SupportedLanguage } from './index';

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthUserResponse {
  id: number | string;
  name: string;
  email: string;
  avatar?: string;
  role?: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUserResponse;
}

export interface ProjectCreateRequest {
  name: string;
  description?: string;
  language: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
}

export interface ProjectUpdateRequest {
  name?: string;
  description?: string;
  language?: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
}

export interface FileCreateRequest {
  filename: string;
  content: string;
  language?: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
}

export interface FileUpdateRequest {
  filename?: string;
  content?: string;
  language?: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
}

export interface BackendFileResponse {
  id: number;
  filename: string;
  content: string;
  language: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
  projectId: number;
  createdAt: string;
  updatedAt: string;
}

export interface BackendProjectResponse {
  id: number;
  name: string;
  description: string;
  language: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
  userId: number;
  userName: string;
  fileCount: number;
  files: BackendFileResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ApiErrorResponse {
  status: number;
  message: string;
  path?: string;
  timestamp?: string;
  errors?: string[];
}

export type BackendExecutionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'WAITING_FOR_INPUT'
  | 'SUCCESS'
  | 'COMPILATION_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIMEOUT'
  | 'OUTPUT_LIMIT_EXCEEDED'
  | 'SYSTEM_ERROR'
  | 'STOPPED';

export interface ExecutionRequest {
  language: 'JAVA' | 'PYTHON' | 'C' | 'CPP';
  code: string;
  stdin?: string;
  timeoutSeconds?: number;
  projectId?: number;
}

export interface ExecutionInputRequest {
  input: string;
}

export interface ExecutionResponse {
  executionId?: string;
  stdout: string;
  stderr: string;
  exitCode: number | null;
  status: BackendExecutionStatus;
  executionTimeMs: number;
  memoryUsedMb?: number;
  errorDetails?: string;
}

export interface ExecutionHealthResponse {
  dockerAvailable: boolean;
  dockerVersion: string;
  status: string;
  imagesStatus: Record<string, boolean>;
}

