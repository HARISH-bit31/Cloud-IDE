import React, { useState, useRef, useEffect } from 'react';
import { useIDE } from '../../context/IDEContext';

interface CommandHistoryItem {
  id: string;
  command: string;
  output: string;
  isError?: boolean;
}

export const TerminalPane: React.FC = () => {
  const { currentProject, runCode } = useIDE();
  const [history, setHistory] = useState<CommandHistoryItem[]>([
    {
      id: 'init-1',
      command: 'uname -a',
      output: 'Linux cloud-ide-worker-pod-0 6.6.137 #1 SMP PREEMPT_DYNAMIC x86_64 GNU/Linux',
    },
    {
      id: 'init-2',
      command: 'javac -version && java -version',
      output: 'javac 21.0.2\nopenjdk version "21.0.2" 2024-01-16 LTS\nOpenJDK Runtime Environment Temurin-21.0.2+13',
    },
  ]);

  const [inputVal, setInputVal] = useState('');
  const [commandList, setCommandList] = useState<string[]>([]);
  const [historyIndex, setHistoryIndex] = useState<number>(-1);
  const bottomRef = useRef<HTMLDivElement>(null);

  const projectName = currentProject?.name ? currentProject.name.replace(/\s+/g, '') : 'Workspace';

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      const trimmed = inputVal.trim();
      if (!trimmed) return;

      setCommandList((prev) => [...prev, trimmed]);
      setHistoryIndex(-1);

      let output = '';
      let isError = false;

      const parts = trimmed.split(' ');
      const cmd = parts[0].toLowerCase();

      switch (cmd) {
        case 'clear':
          setHistory([]);
          setInputVal('');
          return;
        case 'ls':
          output =
            currentProject?.files.map((f) => f.name).join('  ') ||
            'Main.java  Calculator.java  README.md  input.txt';
          break;
        case 'pwd':
          output = `/workspace/${projectName}`;
          break;
        case 'date':
          output = new Date().toUTCString();
          break;
        case 'whoami':
          output = 'developer';
          break;
        case 'cat':
          if (parts[1]) {
            const f = currentProject?.files.find((file) => file.name === parts[1]);
            output = f ? f.content : `cat: ${parts[1]}: No such file or directory`;
            if (!f) isError = true;
          } else {
            output = 'usage: cat <filename>';
          }
          break;
        case 'run':
        case 'javac':
        case 'java':
        case 'python':
        case 'python3':
        case 'gcc':
        case 'g++':
          output = `Executing via Cloud IDE runner:\n> ${trimmed}\nCompiling and running in sandbox...`;
          runCode();
          break;
        case 'help':
          output = `Available simulated commands:
  ls           - List workspace files
  cat <file>   - Display file content
  pwd          - Print current working directory
  clear        - Clear terminal screen
  run / javac  - Trigger code compilation & execution
  whoami       - Display current shell user
  date         - Print current system date & UTC time
  help         - Show this guide`;
          break;
        default:
          output = `bash: ${cmd}: command not found. Type 'help' for available commands.`;
          isError = true;
      }

      setHistory((prev) => [
        ...prev,
        {
          id: 'hist-' + Date.now(),
          command: trimmed,
          output,
          isError,
        },
      ]);
      setInputVal('');
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (commandList.length === 0) return;
      const nextIdx = historyIndex === -1 ? commandList.length - 1 : Math.max(0, historyIndex - 1);
      setHistoryIndex(nextIdx);
      setInputVal(commandList[nextIdx]);
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (historyIndex === -1) return;
      const nextIdx = historyIndex + 1;
      if (nextIdx < commandList.length) {
        setHistoryIndex(nextIdx);
        setInputVal(commandList[nextIdx]);
      } else {
        setHistoryIndex(-1);
        setInputVal('');
      }
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-3 font-mono text-xs text-[#e1e1ef] space-y-2 bg-[#0c0d12]">
      {history.map((item) => (
        <div key={item.id} className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-[#10b981] font-semibold">cloud-ide</span>
            <span className="text-[#a48a93]">~/{projectName}</span>
            <span className="text-[#7bd0ff] font-bold">$</span>
            <span className="text-[#e1e1ef]">{item.command}</span>
          </div>
          {item.output && (
            <pre
              className={`whitespace-pre-wrap pl-3 text-[11px] leading-relaxed ${
                item.isError ? 'text-[#ffb4ab]' : 'text-[#dcbfc9]'
              }`}
            >
              {item.output}
            </pre>
          )}
        </div>
      ))}

      {/* Interactive Command Prompt Line */}
      <div className="flex items-center gap-2">
        <span className="text-[#10b981] font-semibold">cloud-ide</span>
        <span className="text-[#a48a93]">~/{projectName}</span>
        <span className="text-[#7bd0ff] font-bold">$</span>
        <input
          type="text"
          value={inputVal}
          onChange={(e) => setInputVal(e.target.value)}
          onKeyDown={handleKeyDown}
          autoFocus
          className="flex-1 bg-transparent text-[#e1e1ef] focus:outline-none font-mono text-xs caret-[#ffafd2]"
          placeholder="Type a bash command (e.g. ls, cat Main.java, help)..."
        />
      </div>
      <div ref={bottomRef} />
    </div>
  );
};
