import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { Shield, Users, Mail, CheckCircle2 } from 'lucide-react';
import { RoleBadge } from '../components/common/Badges';
import { LoadingSpinner, ErrorBanner } from '../components/common/Feedback';

export const AccountPage: React.FC = () => {
  const { user } = useAuth();

  const { data: teams, isLoading: teamsLoading, error: teamsError } = useQuery({
    queryKey: ['teams'],
    queryFn: () => api.teams.getAll(),
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-white tracking-tight">Teams & Account Profile</h1>
        <p className="text-xs text-slate-400 mt-1">
          Review your operational credentials, department memberships, and role-based permissions
        </p>
      </div>

      {teamsError && (
        <ErrorBanner
          title="Could not load team metadata"
          message="Failed to fetch complete organizational directory."
        />
      )}

      {/* User Information Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-emerald-600/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 font-bold text-xl">
              {user?.name?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">{user?.name}</h2>
              <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                <span>{user?.email}</span>
                <span className="text-slate-600">•</span>
                <span className="text-emerald-400 font-medium">Account Active</span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-slate-400">Operator ID: #{user?.id}</span>
          </div>
        </div>

        {/* Permissions Breakdown Matrix */}
        <div className="mt-6">
          <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
            <Shield className="w-4 h-4 text-emerald-400" />
            Active Team Memberships & Role Authorities
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {user?.memberships.map((m) => {
              const isLeadOrAdmin = m.role === 'LEAD' || m.role === 'ADMIN';
              const isMember = m.role === 'MEMBER' || isLeadOrAdmin;

              return (
                <div
                  key={m.teamId}
                  className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-semibold text-slate-200 block">{m.teamName}</span>
                      <span className="text-[11px] text-slate-500 font-mono">Team ID: #{m.teamId}</span>
                    </div>
                    <RoleBadge role={m.role} />
                  </div>

                  <div className="space-y-1.5 pt-2 border-t border-slate-800/80 text-xs">
                    <div className="flex items-center gap-2 text-slate-300">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                      <span>Inspect & monitor team work items</span>
                    </div>
                    <div
                      className={`flex items-center gap-2 ${
                        isMember ? 'text-slate-300' : 'text-slate-600 line-through'
                      }`}
                    >
                      <CheckCircle2
                        className={`w-3.5 h-3.5 ${isMember ? 'text-emerald-500' : 'text-slate-700'} shrink-0`}
                      />
                      <span>Create, claim, and update work items</span>
                    </div>
                    <div
                      className={`flex items-center gap-2 ${
                        isLeadOrAdmin ? 'text-slate-300' : 'text-slate-600 line-through'
                      }`}
                    >
                      <CheckCircle2
                        className={`w-3.5 h-3.5 ${
                          isLeadOrAdmin ? 'text-emerald-500' : 'text-slate-700'
                        }`}
                      />
                      <span>Approve transitions to RESOLVED status</span>
                    </div>
                    {m.role === 'ADMIN' && (
                      <div className="flex items-center gap-2 text-purple-300">
                        <CheckCircle2 className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                        <span>Administrative management</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Directory of Teams */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
          <Users className="w-4 h-4 text-blue-400" />
          Newtonite Operational Directory
        </h3>

        {teamsLoading ? (
          <LoadingSpinner message="Loading operational directory..." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {teams?.map((t) => (
              <div
                key={t.id}
                className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 text-xs space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-slate-200 text-sm">{t.name}</span>
                  <span className="text-[10px] text-slate-500 font-mono">#{t.id}</span>
                </div>
                <p className="text-slate-400 leading-relaxed">{t.description}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
