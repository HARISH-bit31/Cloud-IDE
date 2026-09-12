import React, { useState } from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  FolderClosed,
  Search,
  GitBranch,
  Play,
  Boxes,
  Terminal,
  Settings as SettingsIcon,
} from 'lucide-react';

export type ActivityTab = 'explorer' | 'search' | 'git' | 'debug' | 'extensions';

export const ActivityBar: React.FC = () => {
  const {
    sidebarCollapsed,
    setSidebarCollapsed,
    setActiveOutputTab,
    setIsSettingsModalOpen,
    setIsCommandPaletteOpen,
    runCode,
  } = useIDE();

  const [activeTab, setActiveTab] = useState<ActivityTab>('explorer');

  const handleTabClick = (tab: ActivityTab) => {
    if (activeTab === tab && !sidebarCollapsed) {
      setSidebarCollapsed(true);
    } else {
      setActiveTab(tab);
      setSidebarCollapsed(false);
    }
  };

  return (
    <aside className="fixed left-0 top-14 bottom-7 w-12 bg-[#0c0e16] border-r border-[#262a3b] z-40 flex flex-col items-center justify-between py-2 select-none">
      {/* Top Nav Group */}
      <nav className="flex flex-col items-center gap-1.5 w-full">
        {/* Explorer */}
        <button
          onClick={() => handleTabClick('explorer')}
          className={`w-full h-10 flex items-center justify-center transition-colors relative ${
            activeTab === 'explorer' && !sidebarCollapsed
              ? 'text-[#ffafd2] bg-[#1d1f28]'
              : 'text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28]'
          }`}
          title="Explorer & Code Editor (Ctrl+B)"
        >
          {activeTab === 'explorer' && !sidebarCollapsed && (
            <span className="absolute left-0 top-0 bottom-0 w-0.5 bg-[#ffafd2]" />
          )}
          <FolderClosed className="w-5 h-5" />
        </button>

        {/* Global Search */}
        <button
          onClick={() => setIsCommandPaletteOpen(true)}
          className="w-full h-10 flex items-center justify-center text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28] transition-colors"
          title="Global Search / Command Palette (⌘K)"
        >
          <Search className="w-5 h-5" />
        </button>

        {/* Git Source Control */}
        <button
          onClick={() => handleTabClick('git')}
          className={`w-full h-10 flex items-center justify-center transition-colors relative ${
            activeTab === 'git' && !sidebarCollapsed
              ? 'text-[#ffafd2] bg-[#1d1f28]'
              : 'text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28]'
          }`}
          title="Source Control (Git)"
        >
          {activeTab === 'git' && !sidebarCollapsed && (
            <span className="absolute left-0 top-0 bottom-0 w-0.5 bg-[#ffafd2]" />
          )}
          <GitBranch className="w-5 h-5" />
        </button>

        {/* Run & Debug */}
        <button
          onClick={runCode}
          className="w-full h-10 flex items-center justify-center text-[#dcbfc9]/70 hover:text-[#10b981] hover:bg-[#1d1f28] transition-colors"
          title="Run Code (Ctrl+Enter)"
        >
          <Play className="w-5 h-5" />
        </button>

        {/* Extensions */}
        <button
          onClick={() => handleTabClick('extensions')}
          className={`w-full h-10 flex items-center justify-center transition-colors relative ${
            activeTab === 'extensions' && !sidebarCollapsed
              ? 'text-[#ffafd2] bg-[#1d1f28]'
              : 'text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28]'
          }`}
          title="Package Extensions"
        >
          {activeTab === 'extensions' && !sidebarCollapsed && (
            <span className="absolute left-0 top-0 bottom-0 w-0.5 bg-[#ffafd2]" />
          )}
          <Boxes className="w-5 h-5" />
        </button>
      </nav>

      {/* Bottom Nav Group */}
      <div className="flex flex-col items-center gap-1.5 w-full">
        <button
          onClick={() => setActiveOutputTab('terminal')}
          className="w-full h-10 flex items-center justify-center text-[#dcbfc9]/70 hover:text-[#7bd0ff] hover:bg-[#1d1f28] transition-colors"
          title="Terminal Console"
        >
          <Terminal className="w-5 h-5" />
        </button>

        <button
          onClick={() => setIsSettingsModalOpen(true)}
          className="w-full h-10 flex items-center justify-center text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28] transition-colors"
          title="IDE Settings"
        >
          <SettingsIcon className="w-5 h-5" />
        </button>
      </div>
    </aside>
  );
};
