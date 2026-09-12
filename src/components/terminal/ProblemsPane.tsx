import React from 'react';
import { useIDE } from '../../context/IDEContext';
import { XCircle, AlertTriangle, Info } from 'lucide-react';
import { ProblemItem } from '../../types';

export const ProblemsPane: React.FC = () => {
  const { problems, currentProject, openFile, addToast } = useIDE();

  const handleProblemClick = (problem: ProblemItem) => {
    const file = currentProject?.files.find((f) => f.name === problem.file);
    if (file) {
      openFile(file.id);
      addToast('Navigated to Issue', 'info', `Line ${problem.line} in ${problem.file}`);
    }
  };

  if (problems.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-4 text-[#10b981] font-mono text-xs select-none">
        <p className="font-semibold">✓ No problems detected in workspace</p>
        <p className="text-[11px] text-[#dcbfc9]/60 mt-1">Linter and syntax verifier report 0 diagnostics</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-2 font-mono text-xs text-[#e1e1ef] space-y-1">
      <div className="text-[11px] text-[#dcbfc9]/70 px-2 py-1 font-semibold border-b border-[#262a3b]">
        Workspace Diagnostics ({problems.length})
      </div>

      {problems.map((item) => {
        const isError = item.severity === 'error';
        const isWarning = item.severity === 'warning';

        return (
          <div
            key={item.id}
            onClick={() => handleProblemClick(item)}
            className="flex items-start gap-2.5 p-2 rounded hover:bg-[#191b24] cursor-pointer transition-colors border border-transparent hover:border-[#262a3b]"
          >
            <div className="mt-0.5 flex-shrink-0">
              {isError && <XCircle className="w-4 h-4 text-[#ffb4ab]" />}
              {isWarning && <AlertTriangle className="w-4 h-4 text-[#ffb95f]" />}
              {!isError && !isWarning && <Info className="w-4 h-4 text-[#7bd0ff]" />}
            </div>

            <div className="flex-1 min-w-0 space-y-0.5">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-[#e1e1ef] truncate">{item.message}</span>
                <span className="text-[10px] text-[#a48a93] px-1 py-0.2 rounded bg-[#282933]">
                  {item.file}:{item.line}
                </span>
              </div>

              {item.codeSnippet && (
                <div className="text-[11px] text-[#dcbfc9]/70 bg-[#0c0e16] px-2 py-1 rounded border border-[#262a3b] mt-1">
                  <code>{item.codeSnippet}</code>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
