import React, { useRef, useEffect } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import { useIDE } from '../../context/IDEContext';

export const MonacoEditorPane: React.FC = () => {
  const {
    activeFile,
    codeBuffer,
    updateCode,
    saveCurrentFile,
    runCode,
    settings,
    registerEditorValueGetter,
  } = useIDE();

  const editorRef = useRef<any>(null);
  const runCodeRef = useRef(runCode);
  const saveCurrentFileRef = useRef(saveCurrentFile);

  useEffect(() => {
    runCodeRef.current = runCode;
  }, [runCode]);

  useEffect(() => {
    saveCurrentFileRef.current = saveCurrentFile;
  }, [saveCurrentFile]);

  const getMonacoLanguage = (lang?: string) => {
    switch (lang) {
      case 'java':
        return 'java';
      case 'python':
        return 'python';
      case 'c':
        return 'c';
      case 'cpp':
        return 'cpp';
      case 'markdown':
        return 'markdown';
      default:
        return 'plaintext';
    }
  };

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;

    if (registerEditorValueGetter) {
      registerEditorValueGetter(() => editor.getValue());
    }

    // Define custom Stitch Dark Theme
    monaco.editor.defineTheme('stitch-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'comment', foreground: 'dcbfc9', fontStyle: 'italic' },
        { token: 'keyword', foreground: 'c13584', fontStyle: 'bold' },
        { token: 'string', foreground: 'a7f3d0' },
        { token: 'number', foreground: 'fcb045' },
        { token: 'type', foreground: 'ffb95f' },
        { token: 'identifier', foreground: 'e1e1ef' },
        { token: 'delimiter', foreground: 'a48a93' },
        { token: 'function', foreground: '7bd0ff' },
      ],
      colors: {
        'editor.background': '#0c0e16',
        'editor.foreground': '#e1e1ef',
        'editorCursor.foreground': '#ffafd2',
        'editor.lineHighlightBackground': '#1d1f2866',
        'editorLineNumber.foreground': '#71717a',
        'editorLineNumber.activeForeground': '#ffafd2',
        'editor.selectionBackground': '#c1358444',
        'editor.inactiveSelectionBackground': '#c1358422',
        'editorIndentGuide.background1': '#262a3b',
        'editorIndentGuide.activeBackground1': '#564149',
        'editorOverviewRuler.border': '#262a3b',
        'editorGutter.background': '#0c0e16',
      },
    });

    monaco.editor.setTheme('stitch-dark');

    // Add Ctrl+Enter Run shortcut with live reference
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      runCodeRef.current();
    });

    // Add Ctrl+S Save shortcut with live reference
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
      saveCurrentFileRef.current();
    });
  };

  if (!activeFile) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center bg-[#0c0e16] text-[#dcbfc9]/60 p-6 select-none font-mono text-xs">
        <p>No file selected</p>
        <p className="text-[11px] text-[#a48a93] mt-1">Select a file from the explorer or create a new file</p>
      </div>
    );
  }

  return (
    <div className="flex-1 min-h-0 relative bg-[#0c0e16] overflow-hidden">
      <Editor
        height="100%"
        language={getMonacoLanguage(activeFile.language)}
        value={codeBuffer}
        theme="stitch-dark"
        onChange={(value) => updateCode(value || '')}
        onMount={handleEditorDidMount}
        options={{
          fontSize: settings.fontSize,
          tabSize: settings.tabSize,
          wordWrap: settings.wordWrap,
          minimap: {
            enabled: settings.minimap,
            maxColumn: 80,
            scale: 1,
            showSlider: 'mouseover',
          },
          lineNumbers: settings.lineNumbers,
          fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
          fontLigatures: true,
          smoothScrolling: true,
          cursorBlinking: 'smooth',
          cursorSmoothCaretAnimation: 'on',
          scrollBeyondLastLine: false,
          automaticLayout: true,
          renderLineHighlight: 'all',
          padding: {
            top: 12,
            bottom: 12,
          },
        }}
      />
    </div>
  );
};
