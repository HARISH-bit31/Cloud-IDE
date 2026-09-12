import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import {
  Project,
  ProjectFile,
  SupportedLanguage,
  LayoutOrientation,
  OutputTabType,
  ProblemItem,
  ExecutionLog,
  ExecutionResult,
  IDESettings,
  ToastNotification,
} from '../types';
import { executionService } from '../services/executionService';
import { projectService } from '../services/projectService';

const DEFAULT_SETTINGS: IDESettings = {
  theme: 'stitch-dark',
  fontSize: 14,
  tabSize: 4,
  wordWrap: 'on',
  minimap: true,
  lineNumbers: 'on',
  defaultLanguage: 'java',
  defaultVersion: 'OpenJDK 21 LTS',
  defaultLayout: 'horizontal',
  sidebarWidth: 240,
  sidebarCollapsed: false,
  editorOutputRatio: 50,
  reducedMotion: false,
};

const SETTINGS_KEY = 'cloud_ide_settings_v1';

const INITIAL_PROBLEMS: ProblemItem[] = [];

interface IDEContextType {
  // Project & Files
  currentProject: Project | null;
  activeFileId: string;
  setActiveFileId: (id: string) => void;
  activeFile: ProjectFile | null;
  openFileIds: string[];
  openFiles: ProjectFile[];
  codeBuffer: string;
  saveStatus: 'saved' | 'saving' | 'unsaved';
  isLoadingProject: boolean;

  // Execution & Console
  activeExecutionId: string | null;
  activeOutputTab: OutputTabType;
  setActiveOutputTab: (tab: OutputTabType) => void;
  stdin: string;
  setStdin: (val: string) => void;
  executionStatus: 'idle' | 'running' | 'success' | 'error' | 'stopped';
  executionResult: ExecutionResult | null;
  logs: ExecutionLog[];
  problems: ProblemItem[];

  // Layout & UI
  layout: LayoutOrientation;
  setLayout: (l: LayoutOrientation) => void;
  sidebarWidth: number;
  setSidebarWidth: (w: number) => void;
  sidebarCollapsed: boolean;
  sidebarCollapsedState?: boolean;
  setSidebarCollapsed: (c: boolean | ((prev: boolean) => boolean)) => void;
  editorOutputRatio: number;
  setEditorOutputRatio: (r: number) => void;

  // Modals & Popups
  isCommandPaletteOpen: boolean;
  setIsCommandPaletteOpen: (open: boolean) => void;
  isSettingsModalOpen: boolean;
  setIsSettingsModalOpen: (open: boolean) => void;
  isCreateProjectModalOpen: boolean;
  setIsCreateProjectModalOpen: (open: boolean) => void;

  // Settings & Toasts
  settings: IDESettings;
  updateSettings: (newSettings: Partial<IDESettings>) => void;
  toasts: ToastNotification[];
  addToast: (title: string, type?: ToastNotification['type'], message?: string) => void;
  removeToast: (id: string) => void;

  // Actions
  loadProject: (projectId: string) => Promise<void>;
  openFile: (fileId: string) => void;
  closeTab: (fileId: string) => void;
  updateCode: (code: string) => void;
  registerEditorValueGetter: (getter: () => string) => void;
  saveCurrentFile: () => Promise<void>;
  runCode: () => void;
  sendInteractiveInput: (input: string) => Promise<boolean>;
  stopCode: () => void;
  clearOutput: () => void;
  createNewFile: (name: string, language?: SupportedLanguage | 'markdown' | 'text') => Promise<void>;
  deleteProjectFile: (fileId: string) => Promise<void>;
  renameProjectFile: (fileId: string, newName: string) => Promise<void>;
  duplicateProjectFile: (fileId: string) => Promise<void>;
  createAndNavigateProject: (name: string, language: SupportedLanguage, templateId?: string) => Promise<Project>;
}

const IDEContext = createContext<IDEContextType | undefined>(undefined);

export const IDEProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Settings loaded from local storage
  const [settings, setSettings] = useState<IDESettings>(() => {
    try {
      const stored = localStorage.getItem(SETTINGS_KEY);
      if (stored) return { ...DEFAULT_SETTINGS, ...JSON.parse(stored) };
    } catch {}
    return DEFAULT_SETTINGS;
  });

  // Project state
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [activeFileId, setActiveFileId] = useState<string>('');
  const [openFileIds, setOpenFileIds] = useState<string[]>([]);
  const [codeBuffer, setCodeBuffer] = useState<string>('');
  const [saveStatus, setSaveStatus] = useState<'saved' | 'saving' | 'unsaved'>('saved');
  const [isLoadingProject, setIsLoadingProject] = useState<boolean>(false);

  // References to guarantee zero stale closures during execution
  const codeBufferRef = useRef<string>('');
  const stdinRef = useRef<string>('');
  const getLiveEditorValueRef = useRef<(() => string) | null>(null);

  // Execution & Output state
  const [activeOutputTab, setActiveOutputTab] = useState<OutputTabType>('output');
  const [activeExecutionId, setActiveExecutionId] = useState<string | null>(null);
  const activeExecutionIdRef = useRef<string | null>(null);
  const [stdin, setStdinState] = useState<string>('');
  const [executionStatus, setExecutionStatus] = useState<'idle' | 'running' | 'success' | 'error' | 'stopped'>('idle');
  const [executionResult, setExecutionResult] = useState<ExecutionResult | null>(null);
  const [logs, setLogs] = useState<ExecutionLog[]>([]);
  const [problems, setProblems] = useState<ProblemItem[]>(INITIAL_PROBLEMS);

  // Layout state
  const [layout, setLayoutState] = useState<LayoutOrientation>(settings.defaultLayout);
  const [sidebarWidth, setSidebarWidthState] = useState<number>(settings.sidebarWidth);
  const [sidebarCollapsed, setSidebarCollapsedState] = useState<boolean>(settings.sidebarCollapsed);
  const [editorOutputRatio, setEditorOutputRatioState] = useState<number>(settings.editorOutputRatio);

  // Modals & Popups
  const [isCommandPaletteOpen, setIsCommandPaletteOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [isCreateProjectModalOpen, setIsCreateProjectModalOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastNotification[]>([]);

  const cancelExecutionRef = useRef<(() => void) | null>(null);

  const registerEditorValueGetter = useCallback((getter: () => string) => {
    getLiveEditorValueRef.current = getter;
  }, []);

  const setStdin = useCallback((val: string) => {
    stdinRef.current = val;
    setStdinState(val);
  }, []);

  // Save settings whenever they change
  const updateSettings = (newSettings: Partial<IDESettings>) => {
    setSettings((prev) => {
      const updated = { ...prev, ...newSettings };
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const setLayout = (newLayout: LayoutOrientation) => {
    setLayoutState(newLayout);
    updateSettings({ defaultLayout: newLayout });
  };

  const setSidebarWidth = (width: number) => {
    setSidebarWidthState(width);
    updateSettings({ sidebarWidth: width });
  };

  const setSidebarCollapsed = (collapsed: boolean | ((prev: boolean) => boolean)) => {
    setSidebarCollapsedState((prev) => {
      const val = typeof collapsed === 'function' ? collapsed(prev) : collapsed;
      updateSettings({ sidebarCollapsed: val });
      return val;
    });
  };

  const setEditorOutputRatio = (ratio: number) => {
    setEditorOutputRatioState(ratio);
    updateSettings({ editorOutputRatio: ratio });
  };

  // Toast management
  const addToast = useCallback((title: string, type: ToastNotification['type'] = 'info', message?: string) => {
    const id = 'toast-' + Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, title, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // Load project by ID from backend REST API
  const loadProject = useCallback(
    async (projectId: string) => {
      setIsLoadingProject(true);
      try {
        const project = await projectService.getProjectById(projectId);
        setCurrentProject(project);
        setExecutionResult(null);
        setLogs([]);

        if (project.files && project.files.length > 0) {
          const firstFile = project.files[0];
          setActiveFileId(firstFile.id);
          setOpenFileIds(project.files.slice(0, 3).map((f) => f.id));
          setCodeBuffer(firstFile.content);
          codeBufferRef.current = firstFile.content;

          const inputFile = project.files.find((f) => f.name.toLowerCase().includes('input'));
          if (inputFile) {
            setStdin(inputFile.content);
          } else {
            setStdin('');
          }
        } else {
          setActiveFileId('');
          setOpenFileIds([]);
          setCodeBuffer('');
          codeBufferRef.current = '';
          setStdin('');
        }
        setSaveStatus('saved');
      } catch (err: any) {
        addToast('Error loading project', 'error', err.message || 'Could not load project from database');
      } finally {
        setIsLoadingProject(false);
      }
    },
    [addToast, setStdin]
  );

  // Open a file tab
  const openFile = useCallback(
    (fileId: string) => {
      if (!currentProject) return;
      const file = currentProject.files.find((f) => f.id === fileId);
      if (!file) return;

      if (!openFileIds.includes(fileId)) {
        setOpenFileIds((prev) => [...prev, fileId]);
      }
      setActiveFileId(fileId);
      setCodeBuffer(file.content);
      codeBufferRef.current = file.content;
      setExecutionResult(null);
    },
    [currentProject, openFileIds]
  );

  // Close a file tab
  const closeTab = useCallback(
    (fileId: string) => {
      setOpenFileIds((prev) => {
        const filtered = prev.filter((id) => id !== fileId);
        if (activeFileId === fileId) {
          if (filtered.length > 0) {
            const nextActive = filtered[filtered.length - 1];
            setActiveFileId(nextActive);
            const nextFile = currentProject?.files.find((f) => f.id === nextActive);
            if (nextFile) {
              setCodeBuffer(nextFile.content);
              codeBufferRef.current = nextFile.content;
            }
          } else {
            setActiveFileId('');
            setCodeBuffer('');
            codeBufferRef.current = '';
          }
        }
        return filtered;
      });
    },
    [activeFileId, currentProject]
  );

  // Update live code buffer
  const updateCode = useCallback(
    (newCode: string) => {
      codeBufferRef.current = newCode;
      setCodeBuffer(newCode);
      setSaveStatus('unsaved');
      if (currentProject && activeFileId) {
        const file = currentProject.files.find((f) => f.id === activeFileId);
        if (file && file.content !== newCode) {
          file.isModified = true;
          file.content = newCode;
        }
      }
    },
    [currentProject, activeFileId]
  );

  // Save current file to MySQL via backend PUT API
  const saveCurrentFile = useCallback(async () => {
    if (!currentProject || !activeFileId) return;
    const currentFile = currentProject.files.find((f) => f.id === activeFileId);
    if (!currentFile) return;

    const liveCode = getLiveEditorValueRef.current ? getLiveEditorValueRef.current() : (codeBufferRef.current || codeBuffer);
    setSaveStatus('saving');
    try {
      const updatedFile = await projectService.saveFileContent(
        currentProject.id,
        activeFileId,
        currentFile.name,
        liveCode,
        currentProject.language
      );

      setCurrentProject((prev) => {
        if (!prev) return null;
        const updatedFiles = prev.files.map((f) => (f.id === activeFileId ? updatedFile : f));
        return { ...prev, files: updatedFiles };
      });
      setSaveStatus('saved');
      addToast('File saved', 'success', `Synced ${currentFile.name} to MySQL database.`);
    } catch (err: any) {
      setSaveStatus('unsaved');
      addToast('Failed to save file', 'error', err.message || 'Error updating file in backend');
    }
  }, [currentProject, activeFileId, codeBuffer, addToast]);

  // Run execution
  const runCode = useCallback(() => {
    if (!currentProject) return;
    const currentFile = currentProject.files.find((f) => f.id === activeFileId) || currentProject.files[0];
    if (!currentFile) return;

    // Get the exact, authoritative, live content from Monaco or ref/state
    const liveCode = getLiveEditorValueRef.current ? getLiveEditorValueRef.current() : (codeBufferRef.current || codeBuffer);
    const liveStdin = stdinRef.current !== undefined ? stdinRef.current : stdin;

    // 1. Immediately clear previous execution output & show running state
    setExecutionResult(null);
    setLogs([]);
    setActiveOutputTab('output');
    setExecutionStatus('running');

    // 2. Fire background async save (does NOT block or delay execution)
    projectService.saveFileContent(
      currentProject.id,
      currentFile.id,
      currentFile.name,
      liveCode,
      currentProject.language
    ).then((updatedFile) => {
      setCurrentProject((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          files: prev.files.map((f) => (f.id === currentFile.id ? updatedFile : f)),
        };
      });
      setSaveStatus('saved');
    }).catch((err) => {
      console.warn('Background save note:', err.message);
    });

    addToast('Compilation started', 'info', `Running ${currentFile.name} with ${currentProject.version || currentProject.language}`);

    const onLogCallback = (log: ExecutionLog) => {
      setLogs((prev) => [log, ...prev]);
    };

    const onProgressCallback = (partial: ExecutionResult) => {
      setExecutionResult(partial);
    };

    const onSessionStartedCallback = (execId: string) => {
      setActiveExecutionId(execId);
      activeExecutionIdRef.current = execId;
    };

    const { promise, cancel } = executionService.executeCode(
      currentProject.language,
      currentFile.name,
      liveCode,
      liveStdin,
      onLogCallback,
      onProgressCallback,
      onSessionStartedCallback,
      currentProject.id
    );

    cancelExecutionRef.current = cancel;

    promise.then((result) => {
      setActiveExecutionId(null);
      activeExecutionIdRef.current = null;
      setExecutionResult(result);
      if (result.status === 'success') {
        setExecutionStatus('success');
        addToast('Process finished', 'success', `Execution completed in ${result.executionTime}s with exit code 0`);
      } else if (result.status === 'stopped') {
        setExecutionStatus('stopped');
        addToast('Execution stopped', 'warning', 'Process terminated by user');
      } else {
        setExecutionStatus('error');
        addToast('Execution error', 'error', 'Check output console and problems tab');
      }
    });
  }, [currentProject, activeFileId, codeBuffer, stdin, addToast]);

  // Send interactive stdin input to running container
  const sendInteractiveInput = useCallback(async (input: string): Promise<boolean> => {
    const targetId = activeExecutionIdRef.current || activeExecutionId;
    if (!targetId || executionStatus !== 'running') {
      return false;
    }
    const success = await executionService.sendInteractiveStdin(input, targetId);
    if (success) {
      setLogs((prev) => [
        {
          id: 'stdin-' + Math.random().toString(36).substring(2, 9),
          timestamp: new Date().toLocaleTimeString(),
          message: `> Stdin input sent: "${input.trim()}"`,
          type: 'info',
        },
        ...prev,
      ]);
    }
    return success;
  }, [activeExecutionId, executionStatus]);

  // Stop execution
  const stopCode = useCallback(() => {
    if (cancelExecutionRef.current) {
      cancelExecutionRef.current();
      cancelExecutionRef.current = null;
    }
    setActiveExecutionId(null);
    activeExecutionIdRef.current = null;
    setExecutionStatus('stopped');
    addToast('Process stopped', 'warning', 'Container execution aborted');
  }, [addToast]);

  // Clear output
  const clearOutput = useCallback(() => {
    setExecutionResult(null);
    setLogs([]);
    addToast('Console cleared', 'info');
  }, [addToast]);

  // Create new file via backend POST API
  const createNewFile = useCallback(
    async (name: string, language?: SupportedLanguage | 'markdown' | 'text') => {
      if (!currentProject) return;
      try {
        const lang = language === 'markdown' || language === 'text' || !language ? undefined : language;
        const newFile = await projectService.addFile(currentProject.id, name, `// ${name}\n`, lang);

        setCurrentProject((prev) => {
          if (!prev) return null;
          return { ...prev, files: [...prev.files, newFile] };
        });

        openFile(newFile.id);
        addToast('File created', 'success', `Added ${name} to database`);
      } catch (err: any) {
        addToast('Failed to create file', 'error', err.message || 'File creation failed');
      }
    },
    [currentProject, openFile, addToast]
  );

  // Delete file via backend DELETE API
  const deleteProjectFile = useCallback(
    async (fileId: string) => {
      if (!currentProject) return;
      try {
        await projectService.deleteFile(currentProject.id, fileId);
        closeTab(fileId);
        setCurrentProject((prev) => {
          if (!prev) return null;
          return { ...prev, files: prev.files.filter((f) => f.id !== fileId) };
        });
        addToast('File deleted', 'info', 'File removed from database');
      } catch (err: any) {
        addToast('Failed to delete file', 'error', err.message || 'File deletion failed');
      }
    },
    [currentProject, closeTab, addToast]
  );

  // Rename file via backend PUT API
  const renameProjectFile = useCallback(
    async (fileId: string, newName: string) => {
      if (!currentProject) return;
      const target = currentProject.files.find((f) => f.id === fileId);
      if (!target) return;

      try {
        const updated = await projectService.updateFile(currentProject.id, fileId, {
          filename: newName,
          content: target.content,
        });

        setCurrentProject((prev) => {
          if (!prev) return null;
          return {
            ...prev,
            files: prev.files.map((f) => (f.id === fileId ? updated : f)),
          };
        });
        addToast('File renamed', 'success', `Renamed to ${newName}`);
      } catch (err: any) {
        addToast('Failed to rename file', 'error', err.message || 'File rename failed');
      }
    },
    [currentProject, addToast]
  );

  // Duplicate file via backend POST API
  const duplicateProjectFile = useCallback(
    async (fileId: string) => {
      if (!currentProject) return;
      const file = currentProject.files.find((f) => f.id === fileId);
      if (!file) return;

      const ext = file.name.includes('.') ? '.' + file.name.split('.').pop() : '';
      const base = file.name.includes('.') ? file.name.substring(0, file.name.lastIndexOf('.')) : file.name;
      const copyName = `${base}_copy${ext}`;

      try {
        const newFile = await projectService.addFile(currentProject.id, copyName, file.content, file.language as SupportedLanguage);
        setCurrentProject((prev) => {
          if (!prev) return null;
          return { ...prev, files: [...prev.files, newFile] };
        });
        openFile(newFile.id);
        addToast('File duplicated', 'success', `Created ${copyName}`);
      } catch (err: any) {
        addToast('Failed to duplicate file', 'error', err.message || 'File duplicate failed');
      }
    },
    [currentProject, openFile, addToast]
  );

  // Create project & navigate
  const createAndNavigateProject = useCallback(
    async (name: string, language: SupportedLanguage, templateId?: string): Promise<Project> => {
      const newProj = await projectService.createProject(name, language, templateId);
      setCurrentProject(newProj);
      if (newProj.files && newProj.files.length > 0) {
        setActiveFileId(newProj.files[0].id);
        setOpenFileIds([newProj.files[0].id]);
        setCodeBuffer(newProj.files[0].content);
        codeBufferRef.current = newProj.files[0].content;
      }
      setExecutionResult(null);
      setLogs([]);
      setStdin('');
      addToast('Project created', 'success', `Initialized ${name}`);
      return newProj;
    },
    [addToast, setStdin]
  );

  // Derive open files objects
  const openFiles = (currentProject?.files || []).filter((f) => openFileIds.includes(f.id));
  const activeFile = (currentProject?.files || []).find((f) => f.id === activeFileId) || null;

  return (
    <IDEContext.Provider
      value={{
        currentProject,
        activeFileId,
        setActiveFileId,
        activeFile,
        openFileIds,
        openFiles,
        codeBuffer,
        saveStatus,
        isLoadingProject,
        activeExecutionId,
        activeOutputTab,
        setActiveOutputTab,
        stdin,
        setStdin,
        executionStatus,
        executionResult,
        logs,
        problems,
        layout,
        setLayout,
        sidebarWidth,
        setSidebarWidth,
        sidebarCollapsed,
        setSidebarCollapsed,
        editorOutputRatio,
        setEditorOutputRatio,
        isCommandPaletteOpen,
        setIsCommandPaletteOpen,
        isSettingsModalOpen,
        setIsSettingsModalOpen,
        isCreateProjectModalOpen,
        setIsCreateProjectModalOpen,
        settings,
        updateSettings,
        toasts,
        addToast,
        removeToast,
        loadProject,
        openFile,
        closeTab,
        updateCode,
        registerEditorValueGetter,
        saveCurrentFile,
        runCode,
        sendInteractiveInput,
        stopCode,
        clearOutput,
        createNewFile,
        deleteProjectFile,
        renameProjectFile,
        duplicateProjectFile,
        createAndNavigateProject,
      }}
    >
      {children}
    </IDEContext.Provider>
  );
};

export const useIDE = () => {
  const context = useContext(IDEContext);
  if (!context) {
    throw new Error('useIDE must be used within an IDEProvider');
  }
  return context;
};
