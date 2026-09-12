import React from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  Terminal as TerminalIcon,
  AlertTriangle,
  FileText,
  Ban,
  Copy,
  Maximize2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { OutputPane } from './OutputPane';
import { TerminalPane } from './TerminalPane';
import { ProblemsPane } from './ProblemsPane';
import { LogsPane } from './LogsPane';
import { StdinPane } from './StdinPane';
import { OutputTabType } from '../../types';

export const ConsoleSection: React.FC<{ isMaximized?: boolean; onToggleMaximize?: () => void }> = ({
  isMaximized = false,
  onToggleMaximize,
}) => {
  const {
    activeOutputTab,
    setActiveOutputTab,
    clearOutput,
    executionResult,
    problems,
    addToast,
    layout,
    setLayout,
  } = useIDE();

  const handleCopyOutput = () => {
    if (executionResult?.stdout) {
      navigator.clipboard.writeText(executionResult.stdout);
      addToast('Output Copied', 'success', 'Terminal stdout copied to clipboard');
    } else {
      addToast('No Output', 'info', 'No text to copy');
    }
  };

  const errorOrWarningCount = problems.length;

  return (
    <div className="flex flex-col h-full bg-[#0c0e16] overflow-hidden select-none" id="console-section">
      {/* Console Control Bar */}
      <div className="flex items-center justify-between h-8 px-2 bg-[#1d1f28] text-[#e1e1ef] border-b border-[#262a3b]">
        {/* Left Console Tabs */}
        <div className="flex items-center gap-1 h-full">
          {/* OUTPUT */}
          <button
            onClick={() => setActiveOutputTab('output')}
            className={`flex items-center gap-1.5 h-full px-3 font-mono text-[11px] font-semibold rounded-t transition-colors ${
              activeOutputTab === 'output'
                ? 'bg-[#0c0e16] text-[#e1e1ef] border-t-2 border-[#10b981]'
                : 'text-[#dcbfc9]/70 hover:bg-[#282933] hover:text-[#e1e1ef]'
            }`}
            type="button"
          >
            <span className="w-2 h-2 rounded-full bg-[#10b981]"></span>
            <span>OUTPUT</span>
          </button>

          {/* TERMINAL */}
          <button
            onClick={() => setActiveOutputTab('terminal')}
            className={`flex items-center gap-1.5 h-full px-3 font-mono text-[11px] font-semibold rounded-t transition-colors ${
              activeOutputTab === 'terminal'
                ? 'bg-[#0c0e16] text-[#e1e1ef] border-t-2 border-[#7bd0ff]'
                : 'text-[#dcbfc9]/70 hover:bg-[#282933] hover:text-[#e1e1ef]'
            }`}
            type="button"
          >
            <TerminalIcon className="w-3.5 h-3.5" />
            <span>TERMINAL</span>
          </button>

          {/* PROBLEMS */}
          <button
            onClick={() => setActiveOutputTab('problems')}
            className={`flex items-center gap-1.5 h-full px-3 font-mono text-[11px] font-semibold rounded-t transition-colors ${
              activeOutputTab === 'problems'
                ? 'bg-[#0c0e16] text-[#e1e1ef] border-t-2 border-[#ffb95f]'
                : 'text-[#dcbfc9]/70 hover:bg-[#282933] hover:text-[#e1e1ef]'
            }`}
            type="button"
          >
            <AlertTriangle className="w-3.5 h-3.5 text-[#ffb95f]" />
            <span>PROBLEMS</span>
            {errorOrWarningCount > 0 && (
              <span className="px-1 rounded bg-[#ffb95f]/20 text-[#ffb95f] font-mono text-[9px]">
                {errorOrWarningCount}
              </span>
            )}
          </button>

          {/* LOGS */}
          <button
            onClick={() => setActiveOutputTab('logs')}
            className={`flex items-center gap-1.5 h-full px-3 font-mono text-[11px] font-semibold rounded-t transition-colors ${
              activeOutputTab === 'logs'
                ? 'bg-[#0c0e16] text-[#e1e1ef] border-t-2 border-[#ffafd2]'
                : 'text-[#dcbfc9]/70 hover:bg-[#282933] hover:text-[#e1e1ef]'
            }`}
            type="button"
          >
            <FileText className="w-3.5 h-3.5" />
            <span>LOGS</span>
          </button>
        </div>

        {/* Right Console Tool Actions */}
        <div className="flex items-center gap-1 text-[#dcbfc9]/70">
          <button
            onClick={clearOutput}
            className="flex items-center gap-1 px-2 py-0.5 rounded hover:bg-[#282933] hover:text-[#e1e1ef] font-mono text-[10px] transition-colors"
            title="Clear Console"
            type="button"
          >
            <Ban className="w-3 h-3" />
            <span className="hidden sm:inline">Clear</span>
          </button>

          <button
            onClick={handleCopyOutput}
            className="flex items-center gap-1 px-2 py-0.5 rounded hover:bg-[#282933] hover:text-[#e1e1ef] font-mono text-[10px] transition-colors"
            title="Copy Output"
            type="button"
          >
            <Copy className="w-3 h-3" />
            <span className="hidden sm:inline">Copy</span>
          </button>

          <span className="text-[#564149]">|</span>

          {/* Auto-scroll badge */}
          <div className="flex items-center gap-1 px-1.5 py-0.5 rounded bg-[#282933] text-[#10b981] font-mono text-[10px]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#10b981]"></span>
            <span className="hidden md:inline">Auto-scroll ON</span>
          </div>

          <button
            onClick={onToggleMaximize}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title={isMaximized ? 'Restore Height' : 'Maximize Height'}
            type="button"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={() => {
              setLayout(layout === 'editor-only' ? 'horizontal' : 'editor-only');
            }}
            className="p-1 rounded hover:bg-[#282933] hover:text-[#e1e1ef] transition-colors"
            title="Toggle Console View"
            type="button"
          >
            {layout === 'editor-only' ? (
              <ChevronUp className="w-3.5 h-3.5" />
            ) : (
              <ChevronDown className="w-3.5 h-3.5" />
            )}
          </button>
        </div>
      </div>

      {/* Two-Column Console Body */}
      <div className="flex flex-1 min-h-0 overflow-hidden bg-[#0c0d12]">
        {/* Active Tab View */}
        <div className="flex-1 flex min-w-0 overflow-hidden">
          {activeOutputTab === 'output' && <OutputPane />}
          {activeOutputTab === 'terminal' && <TerminalPane />}
          {activeOutputTab === 'problems' && <ProblemsPane />}
          {activeOutputTab === 'logs' && <LogsPane />}
        </div>

        {/* Standard Input Side Panel */}
        <div className="hidden md:flex">
          <StdinPane />
        </div>
      </div>
    </div>
  );
};
