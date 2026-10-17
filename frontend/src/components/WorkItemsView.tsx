import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { Clock, User, Users, Lock } from 'lucide-react';
import { WorkItemPriority, WorkItemStatus, WorkItemType } from '../types';

export const WorkItemsView: React.FC = () => {
  const { data: workItems, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['work-items'],
    queryFn: () => api.getWorkItems(),
  });

  const getPriorityBadge = (priority: WorkItemPriority) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      case 'HIGH':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'MEDIUM':
        return 'bg-sky-500/10 text-sky-400 border-sky-500/30';
      case 'LOW':
        return 'bg-slate-500/10 text-slate-400 border-slate-500/30';
    }
  };

  const getStatusBadge = (status: WorkItemStatus) => {
    switch (status) {
      case 'OPEN':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
      case 'IN_PROGRESS':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'AWAITING_APPROVAL':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      case 'RESOLVED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'CLOSED':
        return 'bg-slate-600/10 text-slate-400 border-slate-600/30';
    }
  };

  const getTypeBadge = (type: WorkItemType) => {
    switch (type) {
      case 'INCIDENT':
        return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'PAYMENT':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'COMPLIANCE':
        return 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
      case 'CUSTOMER_ISSUE':
        return 'bg-orange-500/10 text-orange-400 border-orange-500/20';
      case 'TASK':
        return 'bg-teal-500/10 text-teal-400 border-teal-500/20';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-6 rounded-xl">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-white">Operations Work Items Queue</h2>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
              {workItems?.length ?? 0} Work Items Seeded
            </span>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Core operational tracking entity with optimistic locking (`@Version`), foreign keys, and auditability.
          </p>
        </div>

        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg transition border border-slate-700"
        >
          <span>Refresh Queue</span>
        </button>
      </div>

      {isLoading ? (
        <div className="p-12 text-center bg-slate-900/50 border border-slate-800 rounded-xl">
          <Clock className="w-8 h-8 text-blue-400 animate-spin mx-auto mb-3" />
          <p className="text-slate-300 font-medium">Loading operational work items from backend...</p>
        </div>
      ) : isError ? (
        <div className="p-6 bg-rose-950/30 border border-rose-800/50 rounded-xl">
          <p className="text-rose-200 font-medium">Failed to fetch work items</p>
          <p className="text-xs text-rose-400/80 mt-1">{(error as Error)?.message}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {workItems?.map((item) => (
            <div
              key={item.id}
              className="bg-slate-900 border border-slate-800 hover:border-slate-700 p-5 rounded-xl transition space-y-4"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                    #{item.id}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${getTypeBadge(item.type)}`}>
                    {item.type}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${getPriorityBadge(item.priority)}`}>
                    {item.priority}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusBadge(item.status)}`}>
                    {item.status.replace('_', ' ')}
                  </span>
                </div>

                <div className="flex items-center gap-2 text-xs text-slate-400">
                  <span className="flex items-center gap-1 bg-slate-950 px-2.5 py-1 rounded-md border border-slate-800">
                    <Lock className="w-3.5 h-3.5 text-purple-400" />
                    <span>Version: {item.version} (Optimistic Lock)</span>
                  </span>
                </div>
              </div>

              <div>
                <h3 className="text-base font-semibold text-white group-hover:text-emerald-400 transition">
                  {item.title}
                </h3>
                <p className="text-sm text-slate-400 mt-1">{item.description}</p>
              </div>

              <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 border-t border-slate-800/60 pt-3">
                <div className="flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-slate-500" />
                  <span className="text-slate-300 font-medium">{item.teamName || 'Unassigned Team'}</span>
                </div>

                <div className="flex items-center gap-1.5">
                  <User className="w-4 h-4 text-slate-500" />
                  <span>Assignee: <span className="text-slate-300 font-medium">{item.assigneeName || 'Unassigned'}</span></span>
                </div>

                <div className="flex items-center gap-1.5">
                  <Clock className="w-4 h-4 text-slate-500" />
                  <span>Due: <span className="text-slate-300">{item.dueDate || 'None'}</span></span>
                </div>

                <div className="flex items-center gap-1.5 ml-auto">
                  <span>Created by: <span className="text-slate-300">{item.createdByName}</span></span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
