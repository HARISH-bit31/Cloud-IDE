import React from 'react';
import { useIDE } from '../../context/IDEContext';

export const LogsPane: React.FC = () => {
  const { logs } = useIDE();

  if (logs.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-4 text-[#dcbfc9]/50 font-mono text-xs select-none">
        <p>No execution logs</p>
        <p className="text-[11px] text-[#a48a93] mt-1">Logs will appear during container compilation and execution</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-3 font-mono text-xs space-y-1.5 bg-[#0c0d12]">
      {logs.map((log) => {
        const isSuccess = log.type === 'success';
        const isError = log.type === 'error';
        const isWarning = log.type === 'warning';

        return (
          <div key={log.id} className="flex items-start gap-2.5 text-[11px]">
            <span className="text-[#a48a93] select-none flex-shrink-0">[{log.timestamp}]</span>
            <span
              className={`flex-1 ${
                isSuccess
                  ? 'text-[#10b981]'
                  : isError
                  ? 'text-[#ffb4ab]'
                  : isWarning
                  ? 'text-[#ffb95f]'
                  : 'text-[#dcbfc9]'
              }`}
            >
              {log.message}
            </span>
          </div>
        );
      })}
    </div>
  );
};
