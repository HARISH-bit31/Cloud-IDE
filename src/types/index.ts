export type SupportedLanguage = 'java' | 'python' | 'c' | 'cpp';

export interface ProjectFile {
  id: string;
  name: string;
  language: SupportedLanguage | 'markdown' | 'text' | 'json';
  content: string;
  isModified?: boolean;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  language: SupportedLanguage;
  files: ProjectFile[];
  createdAt: string;
  updatedAt: string;
  lastRun?: string;
  status?: 'idle' | 'running' | 'success' | 'error';
  version?: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  role?: string;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
}

export type LayoutOrientation = 'horizontal' | 'vertical' | 'editor-only' | 'output-only';

export type OutputTabType = 'output' | 'terminal' | 'problems' | 'logs' | 'input';

export interface ProblemItem {
  id: string;
  file: string;
  line: number;
  column?: number;
  message: string;
  severity: 'error' | 'warning' | 'info';
  codeSnippet?: string;
}

export interface ExecutionLog {
  id: string;
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
}

export interface ExecutionResult {
  stdout: string;
  stderr: string;
  exitCode?: number | null;
  executionTime: number; // in seconds
  cpuUsage: string;
  memoryUsage: string;
  status: 'success' | 'error' | 'stopped' | 'running' | 'queued';
  compilationTime?: number;
  containerImage?: string;
}

export interface IDESettings {
  fontSize: number;
  tabSize: number;
  wordWrap: 'on' | 'off';
  minimap: boolean;
  lineNumbers: 'on' | 'off';
  defaultLanguage: SupportedLanguage;
  defaultVersion: string;
  defaultLayout: LayoutOrientation;
  sidebarWidth: number;
  sidebarCollapsed: boolean;
  editorOutputRatio: number; // e.g. 70 means 70% editor, 30% output
  reducedMotion: boolean;
  theme: string;
}

export interface ToastNotification {
  id: string;
  title: string;
  message?: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}
