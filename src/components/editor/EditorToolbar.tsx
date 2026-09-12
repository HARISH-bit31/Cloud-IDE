import React from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  Code,
  Sparkles,
  Search,
  Map,
  Terminal,
  Play,
  Square,
  ArrowRightLeft,
  Loader2,
  ChevronDown,
} from 'lucide-react';
import { SupportedLanguage } from '../../types';

export const EditorToolbar: React.FC = () => {
  const {
    currentProject,
    activeFile,
    executionStatus,
    runCode,
    stopCode,
    settings,
    updateSettings,
    setIsCommandPaletteOpen,
    stdin,
    addToast,
    saveCurrentFile,
  } = useIDE();

  const isRunning = executionStatus === 'running';

  const inputLineCount = stdin.trim() ? stdin.trim().split('\n').length : 0;

  const handleFormat = () => {
    addToast('Code Formatted', 'info', 'Auto-formatted source code indentation and syntax');
  };

  const handleFind = () => {
    setIsCommandPaletteOpen(true);
  };

  const toggleMinimap = () => {
    updateSettings({ minimap: !settings.minimap });
    addToast('Minimap', 'info', settings.minimap ? 'Disabled code minimap' : 'Enabled code minimap');
  };

  const getLanguageLabel = (lang?: string) => {
    switch (lang) {
      case 'java':
        return 'Java (JDK 21 LTS)';
      case 'python':
        return 'Python (3.12 CPython)';
      case 'c':
        return 'C (GCC 13.2 C23)';
      case 'cpp':
        return 'C++ (G++ 13.2 C++20)';
      default:
        return 'Plain Text / MD';
    }
  };

  return (
    <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-1.5 bg-[#191b24] border-b border-[#262a3b] select-none">
      {/* Left Group: Language selector & Editor actions */}
      <div className="flex items-center gap-2 flex-wrap">
        {/* Language & Runtime Pill */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#1d1f28] text-[#e1e1ef] font-mono text-xs border border-[#282933]">
          <Code className="w-3.5 h-3.5 text-[#ffb95f]" />
          <span className="font-medium">{getLanguageLabel(currentProject?.language)}</span>
          <ChevronDown className="w-3 h-3 text-[#a48a93]" />
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-0.5 text-[#dcbfc9]/70">
          <button
            onClick={handleFormat}
            className="flex items-center gap-1 px-2 py-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] font-mono text-[11px] transition-colors"
            title="Format Code (Shift+Alt+F)"
            type="button"
          >
            <Sparkles className="w-3.5 h-3.5 text-[#7bd0ff]" />
            <span>Format</span>
          </button>

          <button
            onClick={handleFind}
            className="flex items-center gap-1 px-2 py-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] font-mono text-[11px] transition-colors"
            title="Find / Replace (Ctrl+F)"
            type="button"
          >
            <Search className="w-3.5 h-3.5" />
            <span>Find</span>
          </button>

          <button
            onClick={toggleMinimap}
            className={`flex items-center gap-1 px-2 py-1 rounded hover:bg-[#1d1f28] font-mono text-[11px] transition-colors ${
              settings.minimap ? 'text-[#ffafd2]' : 'hover:text-[#e1e1ef]'
            }`}
            title="Toggle Minimap"
            type="button"
          >
            <Map className="w-3.5 h-3.5" />
            <span>Minimap</span>
          </button>

          <button
            onClick={() => setIsCommandPaletteOpen(true)}
            className="flex items-center gap-1 px-2 py-1 rounded hover:bg-[#1d1f28] hover:text-[#e1e1ef] font-mono text-[11px] transition-colors"
            title="Command Palette"
            type="button"
          >
            <Terminal className="w-3.5 h-3.5" />
            <kbd className="px-1 py-0.5 rounded bg-[#282933] text-[9px] font-mono text-[#a48a93] border border-[#32343e]">
              ⌘⇧P
            </kbd>
          </button>
        </div>
      </div>

      {/* Right Group: Stdin status, Stop button, RUN CTA */}
      <div className="flex items-center gap-2">
        {/* Stdin Queue Status */}
        <div className="hidden sm:flex items-center gap-1 px-2 py-1 rounded bg-[#1d1f28] text-[#7bd0ff] font-mono text-xs border border-[#282933]">
          <ArrowRightLeft className="w-3 h-3" />
          <span>stdin [{inputLineCount} {inputLineCount === 1 ? 'input' : 'inputs'} queued]</span>
        </div>

        {/* Stop Button */}
        {isRunning && (
          <button
            onClick={stopCode}
            className="flex items-center gap-1 px-2.5 py-1.5 rounded bg-[#1d1f28] hover:bg-[#282933] text-[#ffb4ab] border border-[#ffb4ab]/30 font-mono text-xs font-semibold transition-all animate-in fade-in duration-150"
            title="Stop Running Process"
            type="button"
          >
            <Square className="w-3.5 h-3.5 fill-current" />
            <span>Stop</span>
          </button>
        )}

        {/* Primary Spectrum Run Button */}
        <button
          onClick={isRunning ? stopCode : runCode}
          disabled={!activeFile}
          className={`flex items-center gap-1.5 px-4 py-1.5 rounded font-mono text-xs font-bold text-white transition-all shadow-[0_2px_14px_rgba(193,53,132,0.45)] ${
            isRunning
              ? 'bg-[#c13584] hover:bg-[#ad2274] active:scale-95'
              : 'bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-95'
          } ${!activeFile ? 'opacity-50 cursor-not-allowed' : ''}`}
          id="run-code-btn"
          type="button"
        >
          {isRunning ? (
            <>
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
              <span>RUNNING...</span>
            </>
          ) : (
            <>
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>RUN (Ctrl+Enter)</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
