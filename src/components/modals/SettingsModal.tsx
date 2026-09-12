import React, { useState } from 'react';
import { useIDE } from '../../context/IDEContext';
import { Modal } from '../common/Modal';
import { SupportedLanguage, LayoutOrientation } from '../../types';
import { Code, Layout, Sliders, Check } from 'lucide-react';

export const SettingsModal: React.FC = () => {
  const { isSettingsModalOpen, setIsSettingsModalOpen, settings, updateSettings, addToast } = useIDE();
  const [activeSection, setActiveSection] = useState<'editor' | 'execution' | 'layout' | 'accessibility'>('editor');

  const [localSettings, setLocalSettings] = useState(settings);

  const handleSave = () => {
    updateSettings(localSettings);
    setIsSettingsModalOpen(false);
    addToast('Settings Saved', 'success', 'All developer preferences updated');
  };

  return (
    <Modal
      isOpen={isSettingsModalOpen}
      onClose={() => setIsSettingsModalOpen(false)}
      title="IDE Preferences & Settings"
      subtitle="Configure editor behavior, compilation presets, and layout ergonomics"
      maxWidth="max-w-2xl"
    >
      <div className="flex flex-col md:flex-row gap-4">
        {/* Navigation Sidebar */}
        <div className="flex md:flex-col gap-1 w-full md:w-44 border-b md:border-b-0 md:border-r border-[#262a3b] pb-2 md:pb-0 md:pr-2 select-none">
          <button
            onClick={() => setActiveSection('editor')}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-mono text-left transition-colors ${
              activeSection === 'editor'
                ? 'bg-[#1d1f28] text-[#ffafd2] font-semibold border border-[#564149]/40'
                : 'text-[#dcbfc9]/70 hover:bg-[#1d1f28] hover:text-[#e1e1ef]'
            }`}
          >
            <Code className="w-3.5 h-3.5" />
            <span>Editor</span>
          </button>

          <button
            onClick={() => setActiveSection('execution')}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-mono text-left transition-colors ${
              activeSection === 'execution'
                ? 'bg-[#1d1f28] text-[#ffafd2] font-semibold border border-[#564149]/40'
                : 'text-[#dcbfc9]/70 hover:bg-[#1d1f28] hover:text-[#e1e1ef]'
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            <span>Execution</span>
          </button>

          <button
            onClick={() => setActiveSection('layout')}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-mono text-left transition-colors ${
              activeSection === 'layout'
                ? 'bg-[#1d1f28] text-[#ffafd2] font-semibold border border-[#564149]/40'
                : 'text-[#dcbfc9]/70 hover:bg-[#1d1f28] hover:text-[#e1e1ef]'
            }`}
          >
            <Layout className="w-3.5 h-3.5" />
            <span>Layout</span>
          </button>
        </div>

        {/* Settings Body */}
        <div className="flex-1 space-y-4 font-mono text-xs">
          {activeSection === 'editor' && (
            <div className="space-y-3">
              {/* Font Size */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Font Size</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Editor font size in pixels</p>
                </div>
                <select
                  value={localSettings.fontSize}
                  onChange={(e) => setLocalSettings({ ...localSettings, fontSize: Number(e.target.value) })}
                  className="bg-[#0c0e16] border border-[#262a3b] rounded px-2 py-1 text-[#e1e1ef] focus:outline-none"
                >
                  {[12, 13, 14, 15, 16, 18, 20].map((sz) => (
                    <option key={sz} value={sz}>
                      {sz}px
                    </option>
                  ))}
                </select>
              </div>

              {/* Tab Size */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Tab Size</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Spaces per indentation level</p>
                </div>
                <select
                  value={localSettings.tabSize}
                  onChange={(e) => setLocalSettings({ ...localSettings, tabSize: Number(e.target.value) })}
                  className="bg-[#0c0e16] border border-[#262a3b] rounded px-2 py-1 text-[#e1e1ef] focus:outline-none"
                >
                  <option value={2}>2 Spaces</option>
                  <option value={4}>4 Spaces</option>
                  <option value={8}>8 Spaces</option>
                </select>
              </div>

              {/* Word Wrap */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Word Wrap</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Wrap long code lines automatically</p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    setLocalSettings({ ...localSettings, wordWrap: localSettings.wordWrap === 'on' ? 'off' : 'on' })
                  }
                  className={`px-3 py-1 rounded text-xs transition-colors border ${
                    localSettings.wordWrap === 'on'
                      ? 'bg-[#10b981]/20 border-[#10b981] text-[#10b981]'
                      : 'bg-[#0c0e16] border-[#262a3b] text-[#dcbfc9]/60'
                  }`}
                >
                  {localSettings.wordWrap === 'on' ? 'Enabled' : 'Disabled'}
                </button>
              </div>

              {/* Minimap */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Minimap</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Show code overview rail on right</p>
                </div>
                <button
                  type="button"
                  onClick={() => setLocalSettings({ ...localSettings, minimap: !localSettings.minimap })}
                  className={`px-3 py-1 rounded text-xs transition-colors border ${
                    localSettings.minimap
                      ? 'bg-[#10b981]/20 border-[#10b981] text-[#10b981]'
                      : 'bg-[#0c0e16] border-[#262a3b] text-[#dcbfc9]/60'
                  }`}
                >
                  {localSettings.minimap ? 'Enabled' : 'Disabled'}
                </button>
              </div>

              {/* Line Numbers */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Line Numbers</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Display line index in editor gutter</p>
                </div>
                <select
                  value={localSettings.lineNumbers}
                  onChange={(e) =>
                    setLocalSettings({ ...localSettings, lineNumbers: e.target.value as 'on' | 'off' })
                  }
                  className="bg-[#0c0e16] border border-[#262a3b] rounded px-2 py-1 text-[#e1e1ef] focus:outline-none"
                >
                  <option value="on">On</option>
                  <option value="off">Off</option>
                </select>
              </div>
            </div>
          )}

          {activeSection === 'execution' && (
            <div className="space-y-3">
              {/* Default Language */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Default Runtime</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Primary language for newly spawned projects</p>
                </div>
                <select
                  value={localSettings.defaultLanguage}
                  onChange={(e) =>
                    setLocalSettings({ ...localSettings, defaultLanguage: e.target.value as SupportedLanguage })
                  }
                  className="bg-[#0c0e16] border border-[#262a3b] rounded px-2 py-1 text-[#e1e1ef] focus:outline-none"
                >
                  <option value="java">Java 21 LTS</option>
                  <option value="python">Python 3.12</option>
                  <option value="c">C (GCC 13.2)</option>
                  <option value="cpp">C++ (G++ 13.2)</option>
                </select>
              </div>

              {/* Version */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Sandbox Preset</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Ephemeral container distribution</p>
                </div>
                <span className="px-2 py-1 rounded bg-[#0c0e16] text-[#7bd0ff] border border-[#262a3b]">
                  Alpine Linux 3.20
                </span>
              </div>
            </div>
          )}

          {activeSection === 'layout' && (
            <div className="space-y-3">
              {/* Default Layout */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Workspace Orientation</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Default split positioning</p>
                </div>
                <select
                  value={localSettings.defaultLayout}
                  onChange={(e) =>
                    setLocalSettings({ ...localSettings, defaultLayout: e.target.value as LayoutOrientation })
                  }
                  className="bg-[#0c0e16] border border-[#262a3b] rounded px-2 py-1 text-[#e1e1ef] focus:outline-none"
                >
                  <option value="horizontal">Horizontal (Top / Bottom)</option>
                  <option value="vertical">Vertical (Left / Right)</option>
                  <option value="editor-only">Editor Only</option>
                  <option value="output-only">Output Only</option>
                </select>
              </div>

              {/* Sidebar Width */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-[#e1e1ef]">Sidebar Width</span>
                  <p className="text-[11px] text-[#dcbfc9]/60">Initial width of Project Explorer ({localSettings.sidebarWidth}px)</p>
                </div>
                <input
                  type="range"
                  min={180}
                  max={380}
                  value={localSettings.sidebarWidth}
                  onChange={(e) => setLocalSettings({ ...localSettings, sidebarWidth: Number(e.target.value) })}
                  className="w-28 accent-[#ffafd2]"
                />
              </div>
            </div>
          )}

          {/* Action Bar */}
          <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-[#262a3b]">
            <button
              type="button"
              onClick={() => setIsSettingsModalOpen(false)}
              className="px-4 py-2 rounded-lg bg-[#0c0e16] hover:bg-[#282933] text-[#dcbfc9] hover:text-[#e1e1ef] transition-colors border border-[#262a3b]"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="px-5 py-2 rounded-lg text-white font-bold bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-95 transition-all shadow-md"
            >
              Save Preferences
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
};
