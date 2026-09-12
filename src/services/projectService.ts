import { Project, ProjectFile, SupportedLanguage } from '../types';
import { BackendProjectResponse, BackendFileResponse } from '../types/api';
import { projectApi } from '../api/projectApi';
import { fileApi } from '../api/fileApi';

export const mapBackendLanguage = (lang: string): SupportedLanguage => {
  const l = (lang || '').toLowerCase();
  if (l === 'python') return 'python';
  if (l === 'c') return 'c';
  if (l === 'cpp') return 'cpp';
  return 'java';
};

export const mapFrontendLanguageToBackend = (
  lang: SupportedLanguage | string
): 'JAVA' | 'PYTHON' | 'C' | 'CPP' => {
  const l = (lang || '').toLowerCase();
  switch (l) {
    case 'python':
      return 'PYTHON';
    case 'c':
      return 'C';
    case 'cpp':
      return 'CPP';
    case 'java':
    default:
      return 'JAVA';
  }
};

export const mapBackendFileToFile = (f: BackendFileResponse): ProjectFile => {
  const name = f.filename || '';
  let fileLang: ProjectFile['language'] = mapBackendLanguage(f.language);
  if (name.endsWith('.md')) fileLang = 'markdown';
  else if (name.endsWith('.txt')) fileLang = 'text';
  else if (name.endsWith('.json')) fileLang = 'json';

  return {
    id: String(f.id),
    name: f.filename,
    language: fileLang,
    content: f.content || '',
    isModified: false,
  };
};

export const mapBackendProjectToProject = (p: BackendProjectResponse): Project => {
  const lang = mapBackendLanguage(p.language);
  const files = (p.files || []).map(mapBackendFileToFile);
  return {
    id: String(p.id),
    name: p.name,
    description: p.description,
    language: lang,
    files,
    createdAt: p.createdAt,
    updatedAt: p.updatedAt,
    version:
      lang === 'java'
        ? 'JDK 21 LTS (Temurin)'
        : lang === 'python'
        ? 'Python 3.12 (CPython)'
        : lang === 'c'
        ? 'GCC 13.2 (C23)'
        : 'G++ 13.2 (C++20)',
    status: 'idle',
  };
};

export const projectService = {
  async getProjects(): Promise<Project[]> {
    const raw = await projectApi.getProjects();
    return raw.map(mapBackendProjectToProject);
  },

  async getProjectById(id: string | number): Promise<Project> {
    const raw = await projectApi.getProject(id);
    return mapBackendProjectToProject(raw);
  },

  async createProject(
    name: string,
    language: SupportedLanguage,
    description?: string
  ): Promise<Project> {
    const backendLang = mapFrontendLanguageToBackend(language);
    const raw = await projectApi.createProject({
      name: name.trim() || 'Untitled Project',
      description: description || `Cloud sandbox for ${language.toUpperCase()}`,
      language: backendLang,
    });
    return mapBackendProjectToProject(raw);
  },

  async updateProject(
    id: string | number,
    data: { name?: string; description?: string; language?: SupportedLanguage }
  ): Promise<Project> {
    const raw = await projectApi.updateProject(id, {
      name: data.name,
      description: data.description,
      language: data.language ? mapFrontendLanguageToBackend(data.language) : undefined,
    });
    return mapBackendProjectToProject(raw);
  },

  async deleteProject(id: string | number): Promise<void> {
    await projectApi.deleteProject(id);
  },

  async getFiles(projectId: string | number): Promise<ProjectFile[]> {
    const raw = await fileApi.getFiles(projectId);
    return raw.map(mapBackendFileToFile);
  },

  async saveFileContent(
    projectId: string | number,
    fileId: string | number,
    filename: string,
    content: string,
    language?: SupportedLanguage
  ): Promise<ProjectFile> {
    const raw = await fileApi.updateFile(projectId, fileId, {
      filename,
      content,
      language: language ? mapFrontendLanguageToBackend(language) : undefined,
    });
    return mapBackendFileToFile(raw);
  },

  async addFile(
    projectId: string | number,
    name: string,
    content: string = '',
    language?: SupportedLanguage
  ): Promise<ProjectFile> {
    const raw = await fileApi.createFile(projectId, {
      filename: name.trim(),
      content,
      language: language ? mapFrontendLanguageToBackend(language) : undefined,
    });
    return mapBackendFileToFile(raw);
  },

  async updateFile(
    projectId: string | number,
    fileId: string | number,
    data: { filename?: string; content?: string; language?: SupportedLanguage }
  ): Promise<ProjectFile> {
    const raw = await fileApi.updateFile(projectId, fileId, {
      filename: data.filename,
      content: data.content,
      language: data.language ? mapFrontendLanguageToBackend(data.language) : undefined,
    });
    return mapBackendFileToFile(raw);
  },

  async deleteFile(projectId: string | number, fileId: string | number): Promise<void> {
    await fileApi.deleteFile(projectId, fileId);
  },
};
