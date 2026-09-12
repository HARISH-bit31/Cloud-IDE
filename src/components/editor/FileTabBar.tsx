import React from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  Coffee,
  FileCode2,
  BookOpen,
  FileText,
  X,
  Columns2,
  Rows2,
  MoreHorizontal,
} from 'lucide-react';
import { ProjectFile } from '../../types';

export const FileTabBar: React.FC = () => {
  const {
    openFiles,
    activeFile,
    openFile,
    closeTab,
    layout,
    setLayout,
    addToast,
  } = useIDE();

  const getFileIcon = (file: ProjectFile) => {
    if (file.name.endsWith('.java') || file.language === 'java') {
      return <Coffee className="w-3.5 h-3.5 text-[#ffb95f]" />;
    }
    if (file.name.endsWith('.py') || file.language === 'python') {
      return <FileCode2 className="w-3.5 h-3.5 text-[#7bd0ff]" />;
    }
    if (file.name.endsWith('.c') || file.name.endsWith('.cpp') || file.language === 'c' || file.language === 'cpp') {
      return <FileCode2 className="w-3.5 h-3.5 text-[#ffafd2]" />;
    }
    if (file.name.endsWith('.md') || file.language === 'markdown') {
      return <BookOpen className="w-3.5 h-3.5 text-[#c4e7ff]" />;
    }
    return <FileText className="w-3.5 h-3.5 text-[#dcbfc9]" />;
  };

  return (
    <div className="flex items-center justify-between h-9 px-1 bg-[#0c0e16] border-b border-[#262a3b] overflow-x-auto select-none">
      {/* Tabs list */}
      <div className="flex items-center h-full gap-0.5 min-w-0">
        {openFiles.map((file) => {
          const isActive = activeFile?.id === file.id;

          return (
            <div
              key={file.id}
              onClick={() => openFile(file.id)}
              className={`relative flex items-center gap-2 h-full px-3 text-xs font-mono rounded-t cursor-pointer transition-colors ${
                isActive
                  ? 'bg-[#11131c] text-[#e1e1ef] font-medium shadow-sm'
                  : 'bg-[#191b24] text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28]'
              }`}
            >
              {getFileIcon(file)}
              <span>{file.name}</span>

              {/* Modified indicator or close button */}
              {file.isModified && (
                <span className="w-2 h-2 rounded-full bg-[#00a6e0]" title="Unsaved edits" />
              )}

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  closeTab(file.id);
                }}
                className="p-0.5 rounded hover:bg-[#282933] text-[#dcbfc9]/60 hover:text-[#e1e1ef] transition-colors"
                title="Close Tab"
                type="button"
              >
                <X className="w-3 h-3" />
              </button>

              {/* Active Spectrum bottom border stripe */}
              {isActive && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-[#c13584] via-[#ffafd2] to-[#ffb95f]" />
              )}
            </div>
          );
        })}
      </div>

      {/* Right Tab utilities */}
      <div className="flex items-center gap-1 px-2 text-[#dcbfc9]/70">
        <button
          onClick={() => {
            setLayout('vertical');
            addToast('Editor Split Right', 'info');
          }}
          className="p-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] transition-colors"
          title="Split Right"
          type="button"
        >
          <Columns2 className="w-3.5 h-3.5" />
        </button>
        <button
          onClick={() => {
            setLayout('horizontal');
            addToast('Editor Split Down', 'info');
          }}
          className="p-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] transition-colors"
          title="Split Down"
          type="button"
        >
          <Rows2 className="w-3.5 h-3.5" />
        </button>
        <button
          onClick={() => addToast('Tabs Options', 'info', 'Close all / close other tabs options')}
          className="p-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] transition-colors"
          title="More Tab Actions"
          type="button"
        >
          <MoreHorizontal className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
