import React from 'react';
import { useIDE } from '../../context/IDEContext';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useIDE();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-10 right-5 z-[999] flex flex-col gap-2 max-w-sm pointer-events-none">
      {toasts.map((t) => {
        const isSuccess = t.type === 'success';
        const isError = t.type === 'error';
        const isWarning = t.type === 'warning';

        return (
          <div
            key={t.id}
            className={`pointer-events-auto flex items-start gap-3 p-3 rounded-lg border shadow-xl backdrop-blur-xl transition-all animate-in slide-in-from-right-4 duration-200 ${
              isSuccess
                ? 'bg-[#191b24]/95 border-[#10b981]/40 text-[#e1e1ef]'
                : isError
                ? 'bg-[#191b24]/95 border-[#ffb4ab]/40 text-[#ffb4ab]'
                : isWarning
                ? 'bg-[#191b24]/95 border-[#ffb95f]/40 text-[#ffb95f]'
                : 'bg-[#191b24]/95 border-[#32343e] text-[#e1e1ef]'
            }`}
          >
            <div className="mt-0.5 flex-shrink-0">
              {isSuccess && <CheckCircle2 className="w-4 h-4 text-[#10b981]" />}
              {isError && <XCircle className="w-4 h-4 text-[#ef4444]" />}
              {isWarning && <AlertTriangle className="w-4 h-4 text-[#ffb95f]" />}
              {!isSuccess && !isError && !isWarning && <Info className="w-4 h-4 text-[#7bd0ff]" />}
            </div>

            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-[#e1e1ef]">{t.title}</p>
              {t.message && <p className="text-[11px] text-[#dcbfc9]/80 mt-0.5 truncate">{t.message}</p>}
            </div>

            <button
              onClick={() => removeToast(t.id)}
              className="p-1 -mr-1 text-[#dcbfc9]/60 hover:text-[#e1e1ef] hover:bg-[#282933] rounded transition-colors"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        );
      })}
    </div>
  );
};
