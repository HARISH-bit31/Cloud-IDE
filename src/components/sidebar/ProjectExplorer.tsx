import React, { useState } from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  FilePlus,
  FolderPlus,
  RotateCw,
  ChevronsDownUp,
  Search,
  ChevronDown,
  ChevronRight,
  FolderOpen,
  Folder,
  FileCode2,
  FileText,
  BookOpen,
  Coffee,
  MoreVertical,
  Trash2,
  Edit2,
  Copy,
  Download,
  GitBranch,
} from 'lucide-react';
import { ProjectFile } from '../../types';

export const ProjectExplorer: React.FC = () => {
  const {
    currentProject,
    activeFile,
    openFile,
    createNewFile,
    deleteProjectFile,
    renameProjectFile,
    duplicateProjectFile,
    sidebarWidth,
    sidebarCollapsed,
    addToast,
  } = useIDE();

  const [filterQuery, setFilterQuery] = useState('');
  const [isProjectsExpanded, setIsProjectsExpanded] = useState(true);
  const [isWorkspaceExpanded, setIsWorkspaceExpanded] = useState(true);
  const [isNewFileInputOpen, setIsNewFileInputOpen] = useState(false);
  const [newFileName, setNewFileName] = useState('');
  const [editingFileId, setEditingFileId] = useState<string | null>(null);
  const [editingFileName, setEditingFileName] = useState('');
  const [contextMenuFileId, setContextMenuFileId] = useState<string | null>(null);

  if (sidebarCollapsed) return null;

  const handleCreateFileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (newFileName.trim()) {
      createNewFile(newFileName.trim());
      setNewFileName('');
      setIsNewFileInputOpen(false);
    }
  };

  const handleRenameSubmit = (fileId: string, e: React.FormEvent) => {
    e.preventDefault();
    if (editingFileName.trim()) {
      renameProjectFile(fileId, editingFileName.trim());
      setEditingFileId(null);
    }
  };

  const downloadFile = (file: ProjectFile) => {
    const blob = new Blob([file.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    addToast('File downloaded', 'success', `Saved ${file.name} to local disk`);
  };

  const getFileIcon = (fileName: string, lang: string) => {
    if (fileName.endsWith('.java') || lang === 'java') {
      return <Coffee className="w-4 h-4 text-[#ffb95f] flex-shrink-0" />;
    }
    if (fileName.endsWith('.py') || lang === 'python') {
      return <FileCode2 className="w-4 h-4 text-[#7bd0ff] flex-shrink-0" />;
    }
    if (fileName.endsWith('.c') || fileName.endsWith('.cpp') || lang === 'c' || lang === 'cpp') {
      return <FileCode2 className="w-4 h-4 text-[#ffafd2] flex-shrink-0" />;
    }
    if (fileName.endsWith('.md') || lang === 'markdown') {
      return <BookOpen className="w-4 h-4 text-[#c4e7ff] flex-shrink-0" />;
    }
    if (fileName.endsWith('.txt') || lang === 'text') {
      return <FileText className="w-4 h-4 text-[#7bd0ff] flex-shrink-0" />;
    }
    return <FileCode2 className="w-4 h-4 text-[#dcbfc9] flex-shrink-0" />;
  };

  const files = currentProject?.files || [];
  const filteredFiles = files.filter((f) =>
    f.name.toLowerCase().includes(filterQuery.toLowerCase())
  );

  return (
    <section
      className="relative flex flex-col flex-shrink-0 bg-[#191b24] border-r border-[#262a3b] transition-all duration-200 select-none h-full"
      style={{ width: `${sidebarWidth}px` }}
      id="sidebar-explorer"
    >
      {/* Panel Header */}
      <div className="flex items-center justify-between h-9 px-3 bg-[#1d1f28] text-[#e1e1ef] border-b border-[#262a3b]">
        <div className="flex items-center gap-2 min-w-0">
          <span className="font-mono text-[11px] font-semibold tracking-wider uppercase text-[#dcbfc9]/80">
            Explorer
          </span>
          <span className="px-1.5 py-0.5 rounded bg-[#32343e] text-[#7bd0ff] font-mono text-[10px]">
            {currentProject?.language?.toUpperCase() || 'JAVA'}
          </span>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-1 text-[#dcbfc9]/70">
          <button
            onClick={() => setIsNewFileInputOpen(true)}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title="New File"
            type="button"
          >
            <FilePlus className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => addToast('New Folder', 'info', 'Folder support simulated in browser sandbox')}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title="New Folder"
            type="button"
          >
            <FolderPlus className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => addToast('Explorer Refreshed', 'info', 'File system index reloaded')}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title="Refresh Explorer"
            type="button"
          >
            <RotateCw className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => {
              setIsWorkspaceExpanded(!isWorkspaceExpanded);
            }}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title="Collapse / Expand All"
            type="button"
          >
            <ChevronsDownUp className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Quick Filter Search Bar */}
      <div className="p-2 bg-[#0c0e16] border-b border-[#262a3b]">
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-[#1d1f28] text-[#e1e1ef] border border-[#282933] focus-within:border-[#7bd0ff]">
          <Search className="w-3.5 h-3.5 text-[#a48a93]" />
          <input
            className="w-full bg-transparent font-mono text-xs text-[#e1e1ef] placeholder:text-[#a48a93] focus:outline-none"
            placeholder="Filter files (Ctrl+F)"
            type="text"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
          />
          <kbd className="px-1 py-0.5 rounded bg-[#32343e] text-[9px] font-mono text-[#a48a93]">
            ⌘F
          </kbd>
        </div>
      </div>

      {/* Tree Hierarchy Container */}
      <div className="flex-1 overflow-y-auto px-1.5 py-2 font-mono text-xs space-y-1">
        {/* Root Project Header */}
        <div>
          <button
            onClick={() => setIsProjectsExpanded(!isProjectsExpanded)}
            className="w-full flex items-center gap-1 px-1.5 py-1 text-left rounded hover:bg-[#1d1f28] text-[#e1e1ef] font-semibold transition-colors"
            type="button"
          >
            {isProjectsExpanded ? (
              <ChevronDown className="w-3.5 h-3.5 text-[#a48a93]" />
            ) : (
              <ChevronRight className="w-3.5 h-3.5 text-[#a48a93]" />
            )}
            <span className="text-[11px] uppercase tracking-wider text-[#a48a93] font-bold">
              My Projects
            </span>
          </button>

          {isProjectsExpanded && (
            <div className="ml-2 pl-2 space-y-0.5 border-l border-[#262a3b]">
              {/* Active Workspace Root */}
              <div
                onClick={() => setIsWorkspaceExpanded(!isWorkspaceExpanded)}
                className="flex items-center justify-between px-1.5 py-1 rounded bg-[#1d1f28]/70 text-[#7bd0ff] cursor-pointer hover:bg-[#1d1f28] transition-colors"
              >
                <div className="flex items-center gap-1.5 truncate">
                  {isWorkspaceExpanded ? (
                    <FolderOpen className="w-3.5 h-3.5 text-[#7bd0ff]" />
                  ) : (
                    <Folder className="w-3.5 h-3.5 text-[#7bd0ff]" />
                  )}
                  <span className="font-medium text-[#e1e1ef] truncate">
                    {currentProject?.name || 'Workspace'}
                  </span>
                </div>
                <span className="w-2 h-2 rounded-full bg-[#7bd0ff] shadow-[0_0_8px_rgba(123,208,255,0.8)]" />
              </div>

              {/* Nested Project Files */}
              {isWorkspaceExpanded && (
                <div className="ml-2 pl-2 space-y-0.5 border-l border-[#262a3b]">
                  {/* New File Inline Input */}
                  {isNewFileInputOpen && (
                    <form onSubmit={handleCreateFileSubmit} className="flex items-center gap-1 px-1 py-0.5">
                      <input
                        autoFocus
                        type="text"
                        placeholder="e.g. Solution.java"
                        value={newFileName}
                        onChange={(e) => setNewFileName(e.target.value)}
                        onBlur={() => setIsNewFileInputOpen(false)}
                        className="w-full bg-[#0c0e16] border border-[#ffafd2] px-2 py-0.5 text-xs text-[#e1e1ef] rounded focus:outline-none"
                      />
                    </form>
                  )}

                  {/* File List Items */}
                  {filteredFiles.map((file) => {
                    const isActive = activeFile?.id === file.id;
                    const isEditing = editingFileId === file.id;

                    return (
                      <div
                        key={file.id}
                        className={`group relative flex items-center justify-between px-2 py-1 rounded cursor-pointer transition-colors ${
                          isActive
                            ? 'bg-[#1d1f28] text-[#ffafd2] font-medium shadow-sm border border-[#564149]/50'
                            : 'hover:bg-[#1d1f28] text-[#dcbfc9]/80 hover:text-[#e1e1ef]'
                        }`}
                        onClick={() => openFile(file.id)}
                      >
                        {isEditing ? (
                          <form
                            onSubmit={(e) => handleRenameSubmit(file.id, e)}
                            onClick={(e) => e.stopPropagation()}
                            className="flex-1"
                          >
                            <input
                              autoFocus
                              type="text"
                              value={editingFileName}
                              onChange={(e) => setEditingFileName(e.target.value)}
                              onBlur={() => setEditingFileId(null)}
                              className="w-full bg-[#0c0e16] border border-[#ffafd2] px-1 py-0.5 text-xs text-[#e1e1ef] rounded focus:outline-none"
                            />
                          </form>
                        ) : (
                          <div className="flex items-center gap-2 truncate flex-1 min-w-0">
                            {getFileIcon(file.name, file.language)}
                            <span className="truncate">{file.name}</span>
                          </div>
                        )}

                        {/* Status indicators and action buttons */}
                        <div className="flex items-center gap-1">
                          {isActive && (
                            <span className="px-1 py-0.2 rounded bg-[#ffafd2]/20 text-[#ffafd2] text-[9px] font-mono">
                              active
                            </span>
                          )}
                          {file.isModified && !isActive && (
                            <span
                              className="w-2 h-2 rounded-full bg-[#00a6e0]"
                              title="Unsaved changes"
                            />
                          )}

                          {/* Hover action menu button */}
                          <div className="hidden group-hover:flex items-center gap-0.5">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setEditingFileId(file.id);
                                setEditingFileName(file.name);
                              }}
                              className="p-0.5 text-[#dcbfc9]/60 hover:text-[#e1e1ef] rounded"
                              title="Rename"
                            >
                              <Edit2 className="w-3 h-3" />
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                duplicateProjectFile(file.id);
                              }}
                              className="p-0.5 text-[#dcbfc9]/60 hover:text-[#e1e1ef] rounded"
                              title="Duplicate"
                            >
                              <Copy className="w-3 h-3" />
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                downloadFile(file);
                              }}
                              className="p-0.5 text-[#dcbfc9]/60 hover:text-[#e1e1ef] rounded"
                              title="Download"
                            >
                              <Download className="w-3 h-3" />
                            </button>
                            {files.length > 1 && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  deleteProjectFile(file.id);
                                }}
                                className="p-0.5 text-[#dcbfc9]/60 hover:text-[#ffb4ab] rounded"
                                title="Delete"
                              >
                                <Trash2 className="w-3 h-3" />
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Secondary Collapsed Folders for Visual Density */}
              <div className="space-y-0.5 pt-2">
                <button
                  onClick={() => addToast('Folder', 'info', 'C Programs sandbox folder')}
                  className="w-full flex items-center justify-between px-2 py-1 rounded hover:bg-[#1d1f28] text-[#dcbfc9]/60 hover:text-[#e1e1ef] transition-colors text-left"
                  type="button"
                >
                  <div className="flex items-center gap-1.5">
                    <ChevronRight className="w-3.5 h-3.5 text-[#a48a93]" />
                    <Folder className="w-3.5 h-3.5 text-[#a48a93]" />
                    <span>C Programs</span>
                  </div>
                  <span className="text-[10px] text-[#a48a93]">2 files</span>
                </button>

                <button
                  onClick={() => addToast('Folder', 'info', 'Python Scripts sandbox folder')}
                  className="w-full flex items-center justify-between px-2 py-1 rounded hover:bg-[#1d1f28] text-[#dcbfc9]/60 hover:text-[#e1e1ef] transition-colors text-left"
                  type="button"
                >
                  <div className="flex items-center gap-1.5">
                    <ChevronRight className="w-3.5 h-3.5 text-[#a48a93]" />
                    <Folder className="w-3.5 h-3.5 text-[#a48a93]" />
                    <span>Python Scripts</span>
                  </div>
                  <span className="text-[10px] text-[#a48a93]">ml_pipeline.py</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Quick Status Footer Card */}
      <div className="p-2 bg-[#0c0e16] border-t border-[#262a3b] text-[#dcbfc9]/70 space-y-1.5 font-mono text-[11px]">
        <div className="flex items-center justify-between px-2 py-1.5 rounded bg-[#1d1f28]">
          <div className="flex items-center gap-1.5">
            <GitBranch className="w-3.5 h-3.5 text-[#7bd0ff]" />
            <span className="text-[#e1e1ef]">1 uncommitted</span>
          </div>
          <span className="px-1.5 py-0.5 rounded bg-[#282933] text-[10px] text-[#7bd0ff]">
            +18 / -4
          </span>
        </div>

        <div className="flex items-center justify-between px-2 py-1.5 rounded bg-[#1d1f28]">
          <div className="flex items-center gap-1.5 truncate">
            <span className="w-2 h-2 rounded-full bg-[#10b981] animate-pulse"></span>
            <span className="text-[10px] text-[#e1e1ef] truncate">
              {currentProject?.language === 'java'
                ? 'openjdk:21-alpine'
                : currentProject?.language === 'python'
                ? 'python:3.12-alpine'
                : 'gcc:13.2-alpine'}
            </span>
          </div>
          <span className="text-[10px] text-[#10b981] font-semibold">Active</span>
        </div>
      </div>
    </section>
  );
};
