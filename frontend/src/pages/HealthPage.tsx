import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { Server, Database, ShieldCheck, RefreshCw } from 'lucide-react';
import { LoadingSpinner, ErrorBanner } from '../components/common/Feedback';

export const HealthPage: React.FC = () => {
  const {
    data: health,
    isLoading,
    error,
    refetch,
    dataUpdatedAt,
  } = useQuery({
    queryKey: ['system-health'],
    queryFn: () => api.health.getHealth(),
    refetchInterval: 10000,
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">System Health & Telemetry</h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time infrastructure health, database connectivity, and backend telemetry
          </p>
        </div>
        <button
          onClick={() => refetch()}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Poll Now
        </button>
      </div>

      {error && (
        <ErrorBanner
          title="Backend Disconnected"
          message="Could not contact Spring Boot health endpoint at /api/v1/health. Ensure backend is running."
          onRetry={() => refetch()}
        />
      )}

      {isLoading ? (
        <LoadingSpinner message="Polling system diagnostics..." />
      ) : health ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* Service Status */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Service Status
              </span>
              <Server className="w-4 h-4 text-emerald-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
                </span>
                <span className="text-lg font-bold text-white uppercase">{health.status}</span>
              </div>
              <span className="text-xs text-slate-400 block mt-1">
                {health.service} ({health.environment})
              </span>
            </div>
            <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80">
              Timestamp: {new Date(health.timestamp).toLocaleString()}
            </div>
          </div>

          {/* Database Connectivity */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Database Engine
              </span>
              <Database className="w-4 h-4 text-blue-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                <span className="text-base font-bold text-slate-200">
                  {String(health.details?.database || 'CONNECTED')}
                </span>
              </div>
              <span className="text-xs text-slate-400 block mt-1 font-mono truncate">
                {String(health.details?.databaseProductName || 'MySQL')}
                {health.details?.databaseProductVersion ? ` v${health.details.databaseProductVersion}` : ''}
              </span>
            </div>
            <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 font-mono truncate">
              {String(health.details?.databaseUrl || 'jdbc:mysql://localhost:3306/newtonite_ops')}
            </div>
          </div>

          {/* Architecture & Telemetry */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Active Architecture
              </span>
              <ShieldCheck className="w-4 h-4 text-purple-400" />
            </div>
            <div className="space-y-1">
              <div className="text-sm font-semibold text-slate-200">
                Phase 3: Real-Time Operational Web
              </div>
              <p className="text-xs text-slate-400">
                Stateless JWT Bearer Auth + Optimistic Lock Protection + Append-Only Audit Trail
              </p>
            </div>
            <div className="text-[11px] text-slate-500 pt-2 border-t border-slate-800/80 flex items-center justify-between">
              <span>Auto-refresh: 10s</span>
              <span>Last update: {new Date(dataUpdatedAt).toLocaleTimeString()}</span>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
