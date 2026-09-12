import React, { useState } from 'react';
import { useIDE } from '../../context/IDEContext';
import { ArrowRightLeft, Send, Sparkles, Loader2, RotateCcw } from 'lucide-react';

export const StdinPane: React.FC = () => {
  const {
    currentProject,
    stdin,
    setStdin,
    executionStatus,
    sendInteractiveInput,
    addToast,
  } = useIDE();

  const [liveInput, setLiveInput] = useState('');
  const [isSending, setIsSending] = useState(false);

  const isRunning = executionStatus === 'running';

  const handleReset = () => {
    setStdin('');
    setLiveInput('');
    addToast('Stdin Cleared', 'info');
  };

  const handleLoadSample = () => {
    if (currentProject?.language === 'java') {
      setStdin('10\n32');
    } else if (currentProject?.language === 'python') {
      setStdin('15\n27');
    } else if (currentProject?.language === 'c') {
      setStdin('42\n58');
    } else {
      setStdin('100\n200');
    }
    addToast('Sample Stdin Loaded', 'success', 'Added test input parameters');
  };

  const handleSendInteractive = async () => {
    if (!liveInput.trim() && liveInput !== '\n') {
      return;
    }
    setIsSending(true);
    try {
      const ok = await sendInteractiveInput(liveInput);
      if (ok) {
        setLiveInput('');
      } else {
        addToast('Input not sent', 'warning', 'Execution session may have finished');
      }
    } catch {
      addToast('Send failed', 'error', 'Could not pipe input to container');
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (isRunning && e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendInteractive();
    }
  };

  const getInputHint = () => {
    switch (currentProject?.language) {
      case 'java':
        return 'Scanner.nextInt() or nextLine()';
      case 'python':
        return 'input() or sys.stdin.read()';
      case 'c':
        return 'scanf("%d", &val)';
      case 'cpp':
        return 'std::cin >> val';
      default:
        return 'standard input streams';
    }
  };

  return (
    <div className="flex flex-col w-72 bg-[#191b24] p-2.5 space-y-2 border-l border-[#262a3b] select-none">
      {/* Stdin Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <ArrowRightLeft className={`w-3.5 h-3.5 ${isRunning ? 'text-[#ffb95f]' : 'text-[#7bd0ff]'}`} />
          <span className="font-mono text-xs font-semibold text-[#e1e1ef]">
            Standard Input (stdin)
          </span>
        </div>
        {isRunning ? (
          <span className="flex items-center gap-1 px-1.5 py-0.5 rounded bg-[#ffb95f]/20 text-[#ffb95f] font-mono text-[9px] font-semibold animate-pulse border border-[#ffb95f]/30">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ffb95f]"></span>
            <span>Running...</span>
          </span>
        ) : (
          <span className="px-1.5 py-0.5 rounded bg-[#10b981]/20 text-[#10b981] font-mono text-[9px] font-semibold border border-[#10b981]/30">
            Interactive
          </span>
        )}
      </div>

      {/* Description / Instructions */}
      {isRunning ? (
        <div className="p-1.5 rounded bg-[#0c0e16] border border-[#ffb95f]/30 text-[11px] text-[#ffb95f] leading-tight space-y-1">
          <div className="flex items-center gap-1 font-semibold text-[10px]">
            <Loader2 className="w-3 h-3 animate-spin text-[#ffb95f]" />
            <span>Program is running... Waiting for input:</span>
          </div>
          <p className="text-[10px] text-[#dcbfc9]/80 font-mono">
            Type value below and press <kbd className="bg-[#1d1f28] px-1 py-0.5 rounded text-white text-[9px] border border-[#262a3b]">Enter ↵</kbd>
          </p>
        </div>
      ) : (
        <p className="text-[11px] text-[#dcbfc9]/70 leading-tight">
          Passed to{' '}
          <code className="font-mono text-[#7bd0ff] bg-[#1d1f28] px-1 py-0.5 rounded text-[10px]">
            {getInputHint()}
          </code>
        </p>
      )}

      {/* Raw Stdin Text Area */}
      <div className="flex-1 min-h-0 relative flex flex-col">
        {isRunning ? (
          <textarea
            value={liveInput}
            onChange={(e) => setLiveInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type input here and press Enter..."
            autoFocus
            className="w-full h-full p-2 rounded bg-[#0c0e16] font-mono text-xs text-[#ffb95f] focus:outline-none focus:ring-1 focus:ring-[#ffb95f] resize-none leading-5 shadow-inner border border-[#ffb95f]/40 placeholder:text-[#dcbfc9]/40"
            spellCheck={false}
          />
        ) : (
          <textarea
            value={stdin}
            onChange={(e) => setStdin(e.target.value)}
            placeholder="Pre-supply input before Run (or enter interactively while running)..."
            className="w-full h-full p-2 rounded bg-[#0c0e16] font-mono text-xs text-[#7bd0ff] focus:outline-none focus:ring-1 focus:ring-[#7bd0ff] resize-none leading-5 shadow-inner border border-[#262a3b] placeholder:text-[#dcbfc9]/40"
            spellCheck={false}
          />
        )}
      </div>

      {/* Control Actions */}
      <div className="flex items-center justify-between gap-1 pt-1">
        {isRunning ? (
          <>
            <button
              onClick={() => setLiveInput('')}
              className="px-2 py-1 rounded bg-[#1d1f28] hover:bg-[#282933] text-[#dcbfc9] hover:text-[#e1e1ef] font-mono text-[10px] transition-colors border border-[#262a3b] flex items-center gap-1"
              type="button"
              title="Clear input box"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Clear</span>
            </button>
            <button
              onClick={handleSendInteractive}
              disabled={isSending || !liveInput.trim()}
              className="flex-1 px-3 py-1 rounded bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-95 disabled:opacity-50 disabled:pointer-events-none text-white font-mono text-[10px] font-bold transition-all flex items-center justify-center gap-1.5 shadow-[0_2px_8px_rgba(193,53,132,0.4)]"
              type="button"
            >
              {isSending ? (
                <Loader2 className="w-3 h-3 animate-spin" />
              ) : (
                <Send className="w-3 h-3" />
              )}
              <span>Send Input</span>
            </button>
          </>
        ) : (
          <>
            <button
              onClick={handleReset}
              className="px-2.5 py-1 rounded bg-[#1d1f28] hover:bg-[#282933] text-[#dcbfc9] hover:text-[#e1e1ef] font-mono text-[10px] transition-colors border border-[#262a3b]"
              type="button"
            >
              Reset
            </button>
            <button
              onClick={handleLoadSample}
              className="px-2.5 py-1 rounded bg-[#1d1f28] hover:bg-[#282933] text-[#7bd0ff] font-mono text-[10px] font-semibold transition-colors border border-[#262a3b] flex items-center gap-1"
              type="button"
            >
              <Sparkles className="w-3 h-3" />
              <span>Load Sample</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
};
