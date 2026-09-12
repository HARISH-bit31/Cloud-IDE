import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useIDE } from '../../context/IDEContext';
import { Logo } from '../common/Logo';
import {
  PanelLeft,
  Search,
  Bell,
  Settings as SettingsIcon,
  CheckCircle,
  CircleDot,
  GitCommit,
  User as UserIcon,
  Code,
} from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';

export const IDEHeader: React.FC = () => {
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const {
    currentProject,
    activeFile,
    saveStatus,
    setSidebarCollapsed,
    setIsCommandPaletteOpen,
    setIsSettingsModalOpen,
    addToast,
  } = useIDE();

  return (
    <header className="fixed top-0 left-0 right-0 h-14 z-50 bg-[#0c0e16]/95 backdrop-blur-xl border-b border-[#262a3b] flex items-center justify-between px-3 shadow-[0_1px_8px_rgba(0,0,0,0.5)]">
      {/* Left: Brand + Toggle */}
      <div className="flex items-center gap-3 min-w-max">
        <button
          onClick={() => navigate('/dashboard')}
          className="focus:outline-none hover:opacity-90 transition-opacity"
          title="Back to Dashboard"
        >
          <Logo size={26} />
        </button>

        <button
          onClick={() => setSidebarCollapsed((prev) => !prev)}
          className="p-1.5 rounded text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#282933] transition-colors"
          title="Toggle Explorer (Ctrl+B)"
          type="button"
        >
          <PanelLeft className="w-4 h-4" />
        </button>
      </div>

      {/* Center: Breadcrumbs & Save Status */}
      <div className="hidden md:flex items-center gap-2 font-mono text-xs text-[#dcbfc9]/80 max-w-xl truncate px-3">
        <span
          onClick={() => navigate('/dashboard')}
          className="hover:text-[#e1e1ef] cursor-pointer transition-colors"
        >
          My Projects
        </span>
        <span className="text-[#564149]">/</span>
        <span className="hover:text-[#e1e1ef] cursor-pointer transition-colors font-medium">
          {currentProject?.name || 'Workspace'}
        </span>
        <span className="text-[#564149]">/</span>
        <span className="text-[#e1e1ef] font-semibold flex items-center gap-1">
          <Code className="w-3.5 h-3.5 text-[#ffb95f]" />
          {activeFile?.name || 'No file selected'}
        </span>

        {/* Git Branch Pill */}
        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 ml-2 rounded bg-[#1d1f28] text-[#7bd0ff] text-[10px] border border-[#32343e]">
          <GitCommit className="w-3 h-3" />
          main
        </span>

        {/* Save Status Indicator */}
        <span className="inline-flex items-center gap-1 text-[11px] ml-2">
          {saveStatus === 'saved' && (
            <span className="text-[#10b981] flex items-center gap-1">
              <CheckCircle className="w-3.5 h-3.5" />
              All changes saved
            </span>
          )}
          {saveStatus === 'saving' && (
            <span className="text-[#ffb95f] flex items-center gap-1 animate-pulse">
              <CircleDot className="w-3.5 h-3.5" />
              Saving...
            </span>
          )}
          {saveStatus === 'unsaved' && (
            <span className="text-[#7bd0ff] flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-[#7bd0ff]" />
              Unsaved changes
            </span>
          )}
        </span>
      </div>

      {/* Right: Environment Info, Command Search, Notifications, Settings, Profile */}
      <div className="flex items-center gap-2.5 min-w-max">
        {/* Runtime badge */}
        <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#191b24] border border-[#282933] text-[11px] font-mono text-[#dcbfc9]">
          <span className="w-1.5 h-1.5 rounded-full bg-[#10b981] animate-pulse"></span>
          <span>{currentProject?.version || 'Fast-Sandbox'}</span>
        </div>

        {/* Command Palette Button */}
        <button
          onClick={() => setIsCommandPaletteOpen(true)}
          className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded bg-[#1d1f28] border border-[#282933] text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:border-[#a48a93]/40 transition-all text-xs"
          type="button"
        >
          <Search className="w-3.5 h-3.5" />
          <span className="font-mono text-[11px]">Search Actions</span>
          <kbd className="px-1 py-0.5 rounded bg-[#282933] text-[9px] font-mono text-[#a48a93] border border-[#32343e]">
            ⌘K
          </kbd>
        </button>

        {/* Actions Separator */}
        <div className="flex items-center gap-1 border-l border-[#262a3b] pl-2.5">
          <button
            onClick={() => addToast('System Notifications', 'info', 'Sandbox container is healthy and running')}
            className="p-1.5 rounded text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#282933] transition-colors relative"
            title="Notifications"
            type="button"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1 right-1 w-1.5 h-1.5 bg-[#7bd0ff] rounded-full"></span>
          </button>

          <button
            onClick={() => setIsSettingsModalOpen(true)}
            className="p-1.5 rounded text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#282933] transition-colors"
            title="IDE Settings"
            type="button"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>

          {/* User Avatar */}
          <div
            onClick={() => navigate('/dashboard')}
            className="w-7 h-7 rounded-full bg-[#c13584] flex items-center justify-center cursor-pointer ml-1 ring-1 ring-[#ffafd2]/40 overflow-hidden"
            title={`Logged in as ${currentUser?.name || 'Developer'}`}
          >
            {currentUser?.avatar ? (
              <img src={currentUser.avatar} alt="User" className="w-full h-full object-cover" />
            ) : (
              <UserIcon className="w-4 h-4 text-[#ffedf2]" />
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
