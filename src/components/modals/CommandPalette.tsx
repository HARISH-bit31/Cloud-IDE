import React, { useState, useEffect } from 'react';
import { useIDE } from '../../context/IDEContext';
import {
  Play,
  Square,
  Save,
  Sparkles,
  PanelLeft,
  Terminal,
  Columns2,
  Rows2,
  Settings,
  PlusCircle,
  FileCode2,
  Search,
} from 'lucide-react';

interface CommandItem {
  id: string;
  title: string;
  category: string;
  shortcut?: string;
  icon: React.ReactNode;
  action: () => void;
}

export const CommandPalette: React.FC = () => {
  const {
    isCommandPaletteOpen,
    setIsCommandPaletteOpen,
    runCode,
    stopCode,
    saveCurrentFile,
    setSidebarCollapsed,
    setActiveOutputTab,
    setLayout,
    setIsSettingsModalOpen,
    setIsCreateProjectModalOpen,
    currentProject,
    openFile,
    addToast,
  } = useIDE();

  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);

  const commands: CommandItem[] = [
    {
      id: 'cmd-run',
      title: 'Run Code Execution',
      category: 'Runner',
      shortcut: 'Ctrl+Enter',
      icon: <Play className="w-4 h-4 text-[#10b981]" />,
      action: () => {
        runCode();
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-stop',
      title: 'Stop Running Execution',
      category: 'Runner',
      shortcut: 'Ctrl+C',
      icon: <Square className="w-4 h-4 text-[#ffb4ab]" />,
      action: () => {
        stopCode();
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-save',
      title: 'Save Current File',
      category: 'File',
      shortcut: 'Ctrl+S',
      icon: <Save className="w-4 h-4 text-[#7bd0ff]" />,
      action: () => {
        saveCurrentFile();
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-format',
      title: 'Format Document Code',
      category: 'Editor',
      shortcut: 'Shift+Alt+F',
      icon: <Sparkles className="w-4 h-4 text-[#ffb95f]" />,
      action: () => {
        addToast('Formatted', 'info', 'Auto-formatted source code');
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-sidebar',
      title: 'Toggle Project Explorer Sidebar',
      category: 'View',
      shortcut: 'Ctrl+B',
      icon: <PanelLeft className="w-4 h-4 text-[#e1e1ef]" />,
      action: () => {
        setSidebarCollapsed((prev) => !prev);
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-terminal',
      title: 'Toggle Terminal Panel',
      category: 'Terminal',
      shortcut: 'Ctrl+`',
      icon: <Terminal className="w-4 h-4 text-[#7bd0ff]" />,
      action: () => {
        setActiveOutputTab('terminal');
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-layout-h',
      title: 'Layout: Horizontal Split (Editor Top / Output Bottom)',
      category: 'Layout',
      icon: <Rows2 className="w-4 h-4 text-[#dcbfc9]" />,
      action: () => {
        setLayout('horizontal');
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-layout-v',
      title: 'Layout: Vertical Split (Editor Left / Output Right)',
      category: 'Layout',
      icon: <Columns2 className="w-4 h-4 text-[#dcbfc9]" />,
      action: () => {
        setLayout('vertical');
        setIsCommandPaletteOpen(false);
      },
    },
    {
      id: 'cmd-settings',
      title: 'Open Preferences & Settings',
      category: 'Preferences',
      shortcut: '⌘,',
      icon: <Settings className="w-4 h-4 text-[#ffafd2]" />,
      action: () => {
        setIsCommandPaletteOpen(false);
        setIsSettingsModalOpen(true);
      },
    },
    {
      id: 'cmd-new-proj',
      title: 'Create New Project...',
      category: 'Project',
      shortcut: 'Ctrl+Shift+N',
      icon: <PlusCircle className="w-4 h-4 text-[#10b981]" />,
      action: () => {
        setIsCommandPaletteOpen(false);
        setIsCreateProjectModalOpen(true);
      },
    },
    // Dynamic project files
    ...(currentProject?.files.map((file) => ({
      id: `cmd-file-${file.id}`,
      title: `Open File: ${file.name}`,
      category: 'File Tree',
      icon: <FileCode2 className="w-4 h-4 text-[#ffb95f]" />,
      action: () => {
        openFile(file.id);
        setIsCommandPaletteOpen(false);
      },
    })) || []),
  ];

  const filtered = commands.filter(
    (c) =>
      c.title.toLowerCase().includes(query.toLowerCase()) ||
      c.category.toLowerCase().includes(query.toLowerCase())
  );

  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isCommandPaletteOpen) return;

      if (e.key === 'Escape') {
        setIsCommandPaletteOpen(false);
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev + 1) % (filtered.length || 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev - 1 + filtered.length) % (filtered.length || 1));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filtered[selectedIndex]) {
          filtered[selectedIndex].action();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isCommandPaletteOpen, filtered, selectedIndex, setIsCommandPaletteOpen]);

  if (!isCommandPaletteOpen) return null;

  return (
    <div className="fixed inset-0 z-[110] flex items-start justify-center pt-20 px-4 bg-black/70 backdrop-blur-md animate-in fade-in duration-150">
      <div className="w-full max-w-xl bg-[#191b24] border border-[#32343e] rounded-xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-150 flex flex-col">
        {/* Search Input */}
        <div className="flex items-center gap-3 px-4 py-3 border-b border-[#262a3b] bg-[#1d1f28]">
          <Search className="w-4 h-4 text-[#ffafd2]" />
          <input
            autoFocus
            type="text"
            placeholder="Type a command or search files (e.g. Run, Save, Java, Format)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="flex-1 bg-transparent font-mono text-xs text-[#e1e1ef] placeholder:text-[#a48a93] focus:outline-none"
          />
          <kbd className="px-1.5 py-0.5 rounded bg-[#282933] text-[9px] font-mono text-[#a48a93] border border-[#32343e]">
            ESC
          </kbd>
        </div>

        {/* Command List */}
        <div className="max-h-80 overflow-y-auto p-2 space-y-1 font-mono text-xs">
          {filtered.length === 0 ? (
            <div className="p-4 text-center text-[#dcbfc9]/60">No commands matching "{query}"</div>
          ) : (
            filtered.map((cmd, idx) => {
              const isSelected = idx === selectedIndex;

              return (
                <div
                  key={cmd.id}
                  onClick={() => cmd.action()}
                  onMouseEnter={() => setSelectedIndex(idx)}
                  className={`flex items-center justify-between px-3 py-2 rounded-lg cursor-pointer transition-colors ${
                    isSelected
                      ? 'bg-[#1d1f28] text-[#ffafd2] border border-[#564149]/40'
                      : 'text-[#e1e1ef] hover:bg-[#1d1f28]/60'
                  }`}
                >
                  <div className="flex items-center gap-3 truncate">
                    {cmd.icon}
                    <div className="flex flex-col truncate">
                      <span className="font-medium truncate">{cmd.title}</span>
                      <span className="text-[10px] text-[#dcbfc9]/60">{cmd.category}</span>
                    </div>
                  </div>

                  {cmd.shortcut && (
                    <kbd className="px-1.5 py-0.5 rounded bg-[#0c0e16] text-[10px] text-[#a48a93] border border-[#262a3b]">
                      {cmd.shortcut}
                    </kbd>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Footer info */}
        <div className="flex items-center justify-between px-3 py-1.5 bg-[#0c0e16] border-t border-[#262a3b] text-[10px] font-mono text-[#a48a93]">
          <span>Navigation: ↑ ↓ arrows</span>
          <span>Execute: Enter</span>
        </div>
      </div>
    </div>
  );
};
