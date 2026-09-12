import React from 'react';
import { useIDE } from '../../context/IDEContext';
import { Terminal, CheckCircle, XCircle, AlertTriangle, CheckCheck } from 'lucide-react';

export const OutputPane: React.FC = () => {
  const { currentProject, activeFile, executionResult, executionStatus } = useIDE();

  const isRunning = executionStatus === 'running';

  if (isRunning) {
    return (
      <div className="flex-1 overflow-y-auto p-3 font-mono text-xs text-[#e1e1ef] space-y-2">
        <div className="flex items-center gap-2 text-[#7bd0ff] font-semibold">
          <Terminal className="w-3.5 h-3.5" />
          <span>$ compiling & executing in isolated container...</span>
        </div>
        <div className="text-[#a48a93] text-[11px] animate-pulse">
          [INFO] Provisioning ephemeral sandbox container for {currentProject?.version || 'Runner'}...
        </div>
        <div className="flex items-center gap-2 text-[#ffafd2] text-xs pt-2">
          <span className="w-2 h-2 rounded-full bg-[#ffafd2] animate-ping"></span>
          <span>Running {activeFile?.name}... Please wait.</span>
        </div>
      </div>
    );
  }

  if (!executionResult) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-4 text-[#dcbfc9]/50 font-mono text-xs select-none">
        <p>No recent execution output</p>
        <p className="text-[11px] text-[#a48a93] mt-1">Press Ctrl+Enter or click RUN to execute code</p>
      </div>
    );
  }

  const isSuccess = executionResult.status === 'success';
  const isError = executionResult.status === 'error';
  const isStopped = executionResult.status === 'stopped';

  return (
    <div className="flex-1 overflow-y-auto p-3 font-mono text-xs text-[#e1e1ef] space-y-1.5 leading-relaxed">
      {/* Executed Shell Command */}
      <div className="flex items-center gap-2 text-[#7bd0ff] font-semibold">
        <Terminal className="w-3.5 h-3.5" />
        <span>
          {currentProject?.language === 'java'
            ? `$ javac ${activeFile?.name || 'Main.java'} && java Main`
            : currentProject?.language === 'python'
            ? `$ python -u ${activeFile?.name || 'main.py'}`
            : currentProject?.language === 'c'
            ? `$ gcc ${activeFile?.name || 'main.c'} -o main && ./main`
            : `$ g++ ${activeFile?.name || 'main.cpp'} -o main && ./main`}
        </span>
      </div>

      {/* Compiler Info */}
      <div className="text-[#a48a93] text-[11px]">
        [INFO] Executing in container ({executionResult.containerImage || 'isolated-sandbox'})...
      </div>

      {isSuccess && (
        <div className="text-[#10b981] text-[11px] flex items-center gap-1 font-medium">
          <CheckCircle className="w-3.5 h-3.5" />
          <span>[SUCCESS] Finished in {executionResult.compilationTime || 0.74}s</span>
        </div>
      )}

      {/* Program Output Divider */}
      <div className="py-1 text-[#a48a93]/60 text-[11px] font-mono">--- Program Output ---</div>

      {/* Stdout Output Stream */}
      {executionResult.stdout && (
        <pre className="text-[#e1e1ef] font-mono whitespace-pre-wrap pl-2 font-medium">
          {executionResult.stdout}
        </pre>
      )}

      {/* Stderr Error Stream */}
      {executionResult.stderr && (
        <pre className="text-[#ffb4ab] font-mono whitespace-pre-wrap pl-2 bg-[#93000a]/20 p-2 rounded border border-[#ffb4ab]/30">
          {executionResult.stderr}
        </pre>
      )}

      <div className="py-1 text-[#a48a93]/60 text-[11px] font-mono">----------------------</div>

      {/* Process Summary Badge Card */}
      <div className="flex flex-wrap items-center gap-3 p-2 rounded bg-[#191b24] text-[#dcbfc9] text-xs font-mono border border-[#262a3b]">
        {isSuccess && (
          <div className="flex items-center gap-1 text-[#10b981] font-semibold">
            <CheckCheck className="w-4 h-4" />
            <span>Process finished with exit code {executionResult.exitCode}</span>
          </div>
        )}
        {isError && (
          <div className="flex items-center gap-1 text-[#ffb4ab] font-semibold">
            <XCircle className="w-4 h-4" />
            <span>Process failed with exit code {executionResult.exitCode}</span>
          </div>
        )}
        {isStopped && (
          <div className="flex items-center gap-1 text-[#ffb95f] font-semibold">
            <AlertTriangle className="w-4 h-4" />
            <span>Execution stopped by user</span>
          </div>
        )}

        <span className="text-[#564149]">•</span>
        <span>
          Execution: <strong className="text-[#e1e1ef]">{executionResult.executionTime}s</strong>
        </span>
        <span className="text-[#564149]">•</span>
        <span>
          CPU: <strong className="text-[#e1e1ef]">{executionResult.cpuUsage}</strong>
        </span>
        <span className="text-[#564149]">•</span>
        <span>
          Peak Memory: <strong className="text-[#e1e1ef]">{executionResult.memoryUsage}</strong>
        </span>
      </div>
    </div>
  );
};
