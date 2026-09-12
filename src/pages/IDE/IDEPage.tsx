import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useIDE } from '../../context/IDEContext';
import { IDEHeader } from '../../components/layout/IDEHeader';
import { ActivityBar } from '../../components/layout/ActivityBar';
import { IDEFooter } from '../../components/layout/IDEFooter';
import { ProjectExplorer } from '../../components/sidebar/ProjectExplorer';
import { FileTabBar } from '../../components/editor/FileTabBar';
import { EditorToolbar } from '../../components/editor/EditorToolbar';
import { MonacoEditorPane } from '../../components/editor/MonacoEditorPane';
import { ConsoleSection } from '../../components/terminal/ConsoleSection';
import { CreateProjectModal } from '../../components/modals/CreateProjectModal';
import { SettingsModal } from '../../components/modals/SettingsModal';
import { CommandPalette } from '../../components/modals/CommandPalette';
import { ToastContainer } from '../../components/common/ToastContainer';
import { projectService } from '../../services/projectService';

export const IDEPage: React.FC = () => {
  const { projectId } = useParams<{ projectId?: string }>();
  const {
    currentProject,
    loadProject,
    layout,
    setLayout,
    sidebarWidth,
    setSidebarWidth,
    sidebarCollapsed,
    setSidebarCollapsed,
    editorOutputRatio,
    setEditorOutputRatio,
    saveCurrentFile,
    runCode,
    setIsCommandPaletteOpen,
    setActiveOutputTab,
  } = useIDE();

  // Load project on mount or when projectId parameter changes
  useEffect(() => {
    const initProject = async () => {
      if (projectId) {
        await loadProject(projectId);
      } else {
        try {
          const projects = await projectService.getProjects();
          if (projects.length > 0) {
            await loadProject(projects[0].id);
          }
        } catch {
          // Handled via toast notifications
        }
      }
    };
    initProject();
  }, [projectId, loadProject]);

  // Sidebar Drag Resizing
  const isDraggingSidebar = useRef(false);
  const handleSidebarMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isDraggingSidebar.current = true;
    document.addEventListener('mousemove', handleSidebarMouseMove);
    document.addEventListener('mouseup', handleSidebarMouseUp);
  };

  const handleSidebarMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!isDraggingSidebar.current) return;
      const activityBarWidth = 48;
      const newWidth = Math.min(Math.max(e.clientX - activityBarWidth, 180), 420);
      setSidebarWidth(newWidth);
    },
    [setSidebarWidth]
  );

  const handleSidebarMouseUp = useCallback(() => {
    isDraggingSidebar.current = false;
    document.removeEventListener('mousemove', handleSidebarMouseMove);
    document.removeEventListener('mouseup', handleSidebarMouseUp);
  }, [handleSidebarMouseMove]);

  // Editor vs Output Split Drag Resizing
  const isDraggingSplitter = useRef(false);
  const workspaceContainerRef = useRef<HTMLDivElement>(null);

  const handleSplitterMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isDraggingSplitter.current = true;
    document.addEventListener('mousemove', handleSplitterMouseMove);
    document.addEventListener('mouseup', handleSplitterMouseUp);
  };

  const handleSplitterMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!isDraggingSplitter.current || !workspaceContainerRef.current) return;
      const rect = workspaceContainerRef.current.getBoundingClientRect();

      if (layout === 'horizontal') {
        const relativeY = e.clientY - rect.top;
        const percentage = (relativeY / rect.height) * 100;
        const clamped = Math.min(Math.max(percentage, 20), 85);
        setEditorOutputRatio(Math.round(clamped));
      } else if (layout === 'vertical') {
        const relativeX = e.clientX - rect.left;
        const percentage = (relativeX / rect.width) * 100;
        const clamped = Math.min(Math.max(percentage, 20), 85);
        setEditorOutputRatio(Math.round(clamped));
      }
    },
    [layout, setEditorOutputRatio]
  );

  const handleSplitterMouseUp = useCallback(() => {
    isDraggingSplitter.current = false;
    document.removeEventListener('mousemove', handleSplitterMouseMove);
    document.removeEventListener('mouseup', handleSplitterMouseUp);
  }, [handleSplitterMouseMove]);

  // Global Keyboard Shortcuts
  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      // Ctrl+S / Cmd+S: Save
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        saveCurrentFile();
      }
      // Ctrl+Enter / Cmd+Enter: Run
      else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        runCode();
      }
      // Ctrl+B / Cmd+B: Toggle Sidebar
      else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
        e.preventDefault();
        setSidebarCollapsed((prev) => !prev);
      }
      // Ctrl+` / Cmd+`: Toggle Terminal
      else if ((e.ctrlKey || e.metaKey) && (e.key === '`' || e.key === '~')) {
        e.preventDefault();
        setActiveOutputTab('terminal');
      }
      // Ctrl+Shift+P / Cmd+Shift+P / Cmd+K: Command Palette
      else if (
        ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'p') ||
        ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k')
      ) {
        e.preventDefault();
        setIsCommandPaletteOpen(true);
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [saveCurrentFile, runCode, setSidebarCollapsed, setActiveOutputTab, setIsCommandPaletteOpen]);

  return (
    <div className="h-screen w-screen bg-[#11131c] text-[#e1e1ef] flex flex-col font-sans overflow-hidden select-none">
      {/* Top IDE Header (56px) */}
      <IDEHeader />

      {/* Main Workspace Rail (Between Header 56px and Footer 28px) */}
      <div className="flex flex-1 w-full pt-14 pb-7 overflow-hidden relative">
        {/* Leftmost Activity Bar (48px) */}
        <ActivityBar />

        {/* Workspace Body shifted right by 48px */}
        <div className="flex flex-1 pl-12 h-full min-w-0 bg-[#0c0e16]">
          {/* Project Explorer Sidebar (Resizable) */}
          <ProjectExplorer />

          {/* Sidebar Resizer Draggable Divider */}
          {!sidebarCollapsed && (
            <div
              onMouseDown={handleSidebarMouseDown}
              className="w-1 bg-[#262a3b] hover:bg-[#ffafd2] cursor-col-resize transition-colors z-20 flex-shrink-0"
              title="Drag to resize Project Explorer"
            />
          )}

          {/* Central Code Workspace Area */}
          <main
            ref={workspaceContainerRef}
            className={`flex-1 flex min-w-0 bg-[#11131c] overflow-hidden ${
              layout === 'vertical' ? 'flex-row' : 'flex-col'
            }`}
          >
            {/* Editor Portion */}
            {layout !== 'output-only' && (
              <div
                className="flex flex-col min-w-0 min-h-0 bg-[#11131c] overflow-hidden"
                style={{
                  flex: layout === 'editor-only' ? '1 1 100%' : `${editorOutputRatio} 1 0%`,
                }}
                id="editor-section"
              >
                <FileTabBar />
                <EditorToolbar />
                <MonacoEditorPane />
              </div>
            )}

            {/* Resizable Splitter between Editor and Output */}
            {layout !== 'editor-only' && layout !== 'output-only' && (
              <div
                onMouseDown={handleSplitterMouseDown}
                onDoubleClick={() => {
                  setEditorOutputRatio(editorOutputRatio > 80 ? 68 : 88);
                }}
                className={`flex items-center justify-between bg-[#1d1f28] hover:bg-[#ffafd2]/40 transition-colors select-none z-20 flex-shrink-0 ${
                  layout === 'vertical'
                    ? 'w-2 cursor-col-resize flex-col py-4'
                    : 'h-2 cursor-row-resize flex-row px-4'
                }`}
                id="split-divider"
                title="Drag to resize | Double click to toggle collapse"
              >
                <div className={`flex items-center gap-1 ${layout === 'vertical' ? 'flex-col' : 'flex-row'}`}>
                  <span className="w-1 h-1 rounded-full bg-[#a48a93]" />
                  <span className="w-1 h-1 rounded-full bg-[#a48a93]" />
                  <span className="w-1 h-1 rounded-full bg-[#a48a93]" />
                </div>

                {layout === 'horizontal' && (
                  <span className="font-mono text-[9px] text-[#a48a93] hover:text-[#e1e1ef]">
                    Editor {editorOutputRatio}% | Output {100 - editorOutputRatio}%
                  </span>
                )}

                <div className={`flex items-center gap-1 ${layout === 'vertical' ? 'flex-col' : 'flex-row'}`}>
                  <span className="w-1 h-1 rounded-full bg-[#a48a93]" />
                  <span className="w-1 h-1 rounded-full bg-[#a48a93]" />
                </div>
              </div>
            )}

            {/* Output / Console Portion */}
            {layout !== 'editor-only' && (
              <div
                className="flex flex-col min-w-0 min-h-0 bg-[#0c0e16] overflow-hidden"
                style={{
                  flex: layout === 'output-only' ? '1 1 100%' : `${100 - editorOutputRatio} 1 0%`,
                }}
                id="console-section"
              >
                <ConsoleSection />
              </div>
            )}
          </main>
        </div>
      </div>

      {/* Bottom Status Footer (28px) */}
      <IDEFooter />

      {/* Modals, Command Palette & Toast Notifications */}
      <CreateProjectModal />
      <SettingsModal />
      <CommandPalette />
      <ToastContainer />
    </div>
  );
};
