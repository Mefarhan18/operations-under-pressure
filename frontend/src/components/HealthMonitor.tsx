import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { Activity, Database, CheckCircle2, XCircle, RefreshCw, Server, Cpu, ShieldCheck } from 'lucide-react';

export const HealthMonitor: React.FC = () => {
  const { data: health, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['system-health'],
    queryFn: () => api.getHealth(),
    refetchInterval: 10000,
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-6 rounded-xl">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-white">System Connectivity & Health Monitor</h2>
            <span className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Live Polling (10s)
            </span>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Real-time verification of React frontend, Spring Boot 3 backend, and MySQL database persistence.
          </p>
        </div>

        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg transition border border-slate-700 disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-emerald-400' : ''}`} />
          <span>{isFetching ? 'Checking...' : 'Refresh Now'}</span>
        </button>
      </div>

      {isLoading ? (
        <div className="p-12 text-center bg-slate-900/50 border border-slate-800 rounded-xl">
          <RefreshCw className="w-8 h-8 text-emerald-400 animate-spin mx-auto mb-3" />
          <p className="text-slate-300 font-medium">Connecting to Newtonite Ops Backend...</p>
          <p className="text-xs text-slate-500 mt-1">Checking /api/v1/health endpoint</p>
        </div>
      ) : isError ? (
        <div className="p-6 bg-rose-950/30 border border-rose-800/50 rounded-xl">
          <div className="flex items-start gap-3">
            <XCircle className="w-6 h-6 text-rose-400 mt-0.5 shrink-0" />
            <div>
              <h3 className="text-base font-semibold text-rose-200">Backend Service Unreachable</h3>
              <p className="text-sm text-rose-300/80 mt-1">
                Unable to reach the Spring Boot backend at <code className="bg-rose-900/40 px-1 py-0.5 rounded text-xs">/api/v1/health</code>.
              </p>
              <p className="text-xs text-rose-400/80 mt-2">
                Details: {(error as Error)?.message || 'Connection refused or offline'}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Backend Status</span>
              <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-lg">
                <Server className="w-5 h-5" />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span className="text-2xl font-bold text-white">{health?.status}</span>
            </div>
            <p className="text-xs text-slate-400 mt-2">
              Service: <span className="text-slate-200 font-mono">{health?.service}</span>
            </p>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Database Engine</span>
              <div className="p-2 bg-sky-500/10 text-sky-400 rounded-lg">
                <Database className="w-5 h-5" />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span className="text-2xl font-bold text-white">
                {String(health?.details?.database || 'CONNECTED')}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-2 truncate">
              {String(health?.details?.databaseProductName || 'MySQL')} {String(health?.details?.databaseProductVersion || '')}
            </p>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Runtime Target</span>
              <div className="p-2 bg-purple-500/10 text-purple-400 rounded-lg">
                <Cpu className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-white">Java 21</div>
            <p className="text-xs text-slate-400 mt-2">
              Spring Boot 3.3.4 (Hibernate 6)
            </p>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Security Layer</span>
              <div className="p-2 bg-amber-500/10 text-amber-400 rounded-lg">
                <ShieldCheck className="w-5 h-5" />
              </div>
            </div>
            <div className="text-2xl font-bold text-amber-400">BCrypt Ready</div>
            <p className="text-xs text-slate-400 mt-2">
              Auth slated for Phase 2 implementation
            </p>
          </div>
        </div>
      )}

      {health?.details && (
        <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl">
          <h3 className="text-sm font-semibold text-slate-200 mb-3 flex items-center gap-2">
            <Activity className="w-4 h-4 text-emerald-400" />
            Raw Health Response Payload
          </h3>
          <pre className="p-4 bg-slate-950 rounded-lg text-xs font-mono text-emerald-400 overflow-x-auto border border-slate-800">
            {JSON.stringify(health, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
};
