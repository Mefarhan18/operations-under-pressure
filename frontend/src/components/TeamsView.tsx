import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { CheckCircle, Mail, KeyRound, Building2 } from 'lucide-react';

export const TeamsView: React.FC = () => {
  const { data: teams, isLoading: loadingTeams } = useQuery({
    queryKey: ['teams'],
    queryFn: () => api.getTeams(),
  });

  const { data: users, isLoading: loadingUsers } = useQuery({
    queryKey: ['users'],
    queryFn: () => api.getUsers(),
  });

  return (
    <div className="space-y-8">
      {/* Teams Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between bg-slate-900 border border-slate-800 p-6 rounded-xl">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-white">Operational Teams</h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                {teams?.length ?? 0} Teams
              </span>
            </div>
            <p className="text-sm text-slate-400 mt-1">
              Teams define cross-functional organizational boundaries for operational items.
            </p>
          </div>
        </div>

        {loadingTeams ? (
          <div className="p-8 text-center text-slate-400">Loading teams...</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {teams?.map((team) => (
              <div key={team.id} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-2">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-lg">
                    <Building2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-white">{team.name}</h3>
                    <span className="text-xs text-slate-500">ID #{team.id}</span>
                  </div>
                </div>
                <p className="text-sm text-slate-400 pt-1">{team.description}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Users Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between bg-slate-900 border border-slate-800 p-6 rounded-xl">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-white">Seeded Demo Users</h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">
                BCrypt Password Hashing Enforced
              </span>
            </div>
            <p className="text-sm text-slate-400 mt-1">
              User credentials stored strictly as salted BCrypt hashes. Passwords never stored plaintext.
            </p>
          </div>
        </div>

        {loadingUsers ? (
          <div className="p-8 text-center text-slate-400">Loading users...</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {users?.map((u) => (
              <div key={u.id} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-3">
                <div className="flex items-center justify-between">
                  <span className="w-9 h-9 rounded-full bg-slate-800 flex items-center justify-center text-sm font-bold text-emerald-400 border border-slate-700">
                    {u.name.charAt(0)}
                  </span>
                  <span className="flex items-center gap-1 text-xs text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">
                    <CheckCircle className="w-3 h-3" />
                    <span>Enabled</span>
                  </span>
                </div>

                <div>
                  <h4 className="font-semibold text-white">{u.name}</h4>
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-0.5">
                    <Mail className="w-3.5 h-3.5 text-slate-500" />
                    <span className="truncate">{u.email}</span>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/80 flex items-center gap-1.5 text-xs text-slate-500">
                  <KeyRound className="w-3.5 h-3.5 text-amber-500" />
                  <span>BCrypt Salted Hash</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
