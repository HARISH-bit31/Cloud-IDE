import React from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  Terminal,
  GitBranch,
  XCircle,
  AlertTriangle,
  Columns2,
  Rows2,
  Maximize2,
  Minimize2,
  Bell,
} from 'lucide-react';

export const IDEFooter: React.FC = () => {
  const {
    currentProject,
    layout,
    setLayout,
    setActiveOutputTab,
    problems,
    addToast,
  } = useIDE();

  const errorCount = problems.filter((p) => p.severity === 'error').length;
  const warningCount = problems.filter((p) => p.severity === 'warning').length;

  return (
    <footer className="fixed bottom-0 left-0 right-0 h-7 z-50 bg-[#0c0e16] border-t border-[#262a3b] flex items-center justify-between px-3 font-mono text-[11px] text-[#dcbfc9]/70 select-none">
      {/* Left section: Terminal, Git, Diagnostics, Encoding */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => setActiveOutputTab('terminal')}
          className="flex items-center gap-1 hover:text-[#e1e1ef] transition-colors"
          type="button"
        >
          <Terminal className="w-3.5 h-3.5 text-[#7bd0ff]" />
          <span>Bash</span>
        </button>

        <button
          onClick={() => addToast('Git Sync', 'info', 'Repository is synchronized with origin/main')}
          className="flex items-center gap-1 hover:text-[#e1e1ef] transition-colors"
          type="button"
        >
          <GitBranch className="w-3.5 h-3.5" />
          <span>main</span>
          <span className="text-[10px] text-[#10b981]">↑0 ↓0</span>
        </button>

        <button
          onClick={() => setActiveOutputTab('problems')}
          className="flex items-center gap-2 hover:text-[#e1e1ef] transition-colors"
        >
          <span className="inline-flex items-center gap-0.5 text-[#ffb4ab]">
            <XCircle className="w-3 h-3" />
            {errorCount}
          </span>
          <span className="inline-flex items-center gap-0.5 text-[#ffb95f]">
            <AlertTriangle className="w-3 h-3" />
            {warningCount}
          </span>
        </button>

        <span className="text-[#564149]">|</span>
        <span className="hidden sm:inline">UTF-8</span>
        <span className="hidden sm:inline">Spaces: 4</span>
      </div>

      {/* Center section: Layout Presets */}
      <div className="hidden md:flex items-center gap-1.5">
        <span className="text-[10px] uppercase text-[#a48a93]/60 tracking-wider">Preset:</span>
        <button
          onClick={() => {
            setLayout('horizontal');
            addToast('Layout Changed', 'info', 'Horizontal Split (Editor top, Output bottom)');
          }}
          className={`p-1 rounded transition-colors ${
            layout === 'horizontal'
              ? 'bg-[#282933] text-[#7bd0ff]'
              : 'hover:bg-[#282933] text-[#dcbfc9]/70 hover:text-[#e1e1ef]'
          }`}
          title="Horizontal Split (Editor top, Output bottom)"
          type="button"
        >
          <Rows2 className="w-3.5 h-3.5" />
        </button>

        <button
          onClick={() => {
            setLayout('vertical');
            addToast('Layout Changed', 'info', 'Vertical Split (Editor left, Output right)');
          }}
          className={`p-1 rounded transition-colors ${
            layout === 'vertical'
              ? 'bg-[#282933] text-[#7bd0ff]'
              : 'hover:bg-[#282933] text-[#dcbfc9]/70 hover:text-[#e1e1ef]'
          }`}
          title="Vertical Split (Editor left, Output right)"
          type="button"
        >
          <Columns2 className="w-3.5 h-3.5" />
        </button>

        <button
          onClick={() => {
            setLayout(layout === 'editor-only' ? 'horizontal' : 'editor-only');
            addToast('Layout Mode', 'info', layout === 'editor-only' ? 'Restored Split Layout' : 'Editor Maximize');
          }}
          className={`p-1 rounded transition-colors ${
            layout === 'editor-only'
              ? 'bg-[#282933] text-[#ffafd2]'
              : 'hover:bg-[#282933] text-[#dcbfc9]/70 hover:text-[#e1e1ef]'
          }`}
          title="Maximize Editor"
          type="button"
        >
          <Maximize2 className="w-3.5 h-3.5" />
        </button>

        <button
          onClick={() => {
            setLayout(layout === 'output-only' ? 'horizontal' : 'output-only');
            addToast('Layout Mode', 'info', layout === 'output-only' ? 'Restored Split Layout' : 'Output Console Maximize');
          }}
          className={`p-1 rounded transition-colors ${
            layout === 'output-only'
              ? 'bg-[#282933] text-[#ffafd2]'
              : 'hover:bg-[#282933] text-[#dcbfc9]/70 hover:text-[#e1e1ef]'
          }`}
          title="Maximize Console"
          type="button"
        >
          <Minimize2 className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Right section: Cursor position, SDK, Server latency */}
      <div className="flex items-center gap-3">
        <span className="cursor-pointer hover:text-[#e1e1ef]">Ln 14, Col 28</span>
        <span className="text-[#7bd0ff] font-medium hidden sm:inline">
          {currentProject?.version || 'Sandbox VM'}
        </span>
        <span className="flex items-center gap-1 text-[#10b981]">
          <span className="w-1.5 h-1.5 rounded-full bg-[#10b981] animate-pulse"></span>
          <span>18ms (us-east)</span>
        </span>
        <button
          onClick={() => addToast('Cloud Runner', 'info', 'Container sandbox status: Healthy')}
          className="hover:text-[#e1e1ef] transition-colors"
        >
          <Bell className="w-3.5 h-3.5" />
        </button>
      </div>
    </footer>
  );
};
