import React from 'react';
import { AlertCircle, AlertTriangle, RefreshCw, Loader2, Inbox } from 'lucide-react';

interface ErrorBannerProps {
  title?: string;
  message: string;
  status?: number;
  onRetry?: () => void;
  actionText?: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
  title = 'An error occurred',
  message,
  status,
  onRetry,
  actionText = 'Try Again',
}) => {
  const isConflict = status === 409;
  const isForbidden = status === 403;

  return (
    <div
      className={`p-4 rounded-lg border text-sm flex items-start justify-between gap-3 ${
        isConflict
          ? 'bg-amber-950/40 border-amber-800 text-amber-200'
          : isForbidden
          ? 'bg-rose-950/40 border-rose-800 text-rose-200'
          : 'bg-red-950/40 border-red-800 text-red-200'
      }`}
    >
      <div className="flex items-start gap-3">
        {isConflict ? (
          <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
        ) : (
          <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
        )}
        <div>
          <h4 className="font-semibold text-white">{title}</h4>
          <p className="mt-0.5 text-slate-300">{message}</p>
        </div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          {actionText}
        </button>
      )}
    </div>
  );
};

export const LoadingSpinner: React.FC<{ message?: string; fullPage?: boolean }> = ({
  message = 'Loading data...',
  fullPage = false,
}) => {
  const content = (
    <div className="flex flex-col items-center justify-center p-8 text-slate-400">
      <Loader2 className="w-8 h-8 animate-spin text-emerald-500 mb-2" />
      <p className="text-sm">{message}</p>
    </div>
  );

  if (fullPage) {
    return <div className="min-h-screen flex items-center justify-center bg-slate-950">{content}</div>;
  }
  return content;
};

export const EmptyState: React.FC<{
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
}> = ({ title, description, action }) => {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center border border-dashed border-slate-800 rounded-lg bg-slate-900/30">
      <div className="w-12 h-12 rounded-full bg-slate-800/80 flex items-center justify-center text-slate-400 mb-3">
        <Inbox className="w-6 h-6" />
      </div>
      <h3 className="text-base font-medium text-slate-200">{title}</h3>
      {description && <p className="text-sm text-slate-400 mt-1 max-w-sm">{description}</p>}
      {action && (
        <button
          onClick={action.onClick}
          className="mt-4 px-4 py-2 rounded-md bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition"
        >
          {action.label}
        </button>
      )}
    </div>
  );
};
