import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  FolderOpen,
  AlertTriangle,
  UserCheck,
  Clock,
  Plus,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { StatusBadge, PriorityBadge, TypeBadge } from '../components/common/Badges';
import { LoadingSpinner, ErrorBanner } from '../components/common/Feedback';
import { WorkItemFormModal } from '../components/workitem/WorkItemFormModal';
import { WorkItemResponse } from '../types';

export const DashboardPage: React.FC = () => {
  const { user, writableTeams } = useAuth();
  const navigate = useNavigate();
  const [createModalOpen, setCreateModalOpen] = useState(false);

  // 1. Summary Counts Query
  const {
    data: summary,
    isLoading: summaryLoading,
    error: summaryError,
    refetch: refetchSummary,
  } = useQuery({
    queryKey: ['work-item-summary'],
    queryFn: () => api.workItems.getSummary(),
    refetchInterval: 30000,
  });

  // 2. Assigned to Me Queue (active items)
  const { data: assignedToMeData, isLoading: assignedLoading } = useQuery({
    queryKey: ['work-items', 'assigned-to-me', user?.id],
    queryFn: () =>
      api.workItems.getAll({
        assigneeId: user?.id,
        size: 5,
        sortBy: 'updatedAt',
        sortDir: 'desc',
      }),
    enabled: !!user?.id,
  });

  // 3. Unassigned Urgent Queue
  const { data: urgentData, isLoading: urgentLoading } = useQuery({
    queryKey: ['work-items', 'unassigned-urgent'],
    queryFn: () =>
      api.workItems.getAll({
        priority: 'URGENT',
        size: 5,
        sortBy: 'createdAt',
        sortDir: 'desc',
      }),
  });

  // 4. Awaiting Approval Queue
  const { data: awaitingApprovalData, isLoading: awaitingLoading } = useQuery({
    queryKey: ['work-items', 'awaiting-approval'],
    queryFn: () =>
      api.workItems.getAll({
        status: 'AWAITING_APPROVAL',
        size: 5,
        sortBy: 'updatedAt',
        sortDir: 'desc',
      }),
  });

  // Unassigned urgent filtered
  const unassignedUrgent = urgentData?.content.filter((item) => item.assigneeId === null) || [];
  const assignedToMe = assignedToMeData?.content || [];
  const awaitingApproval = awaitingApprovalData?.content || [];

  return (
    <div className="space-y-6">
      {/* Page Title & Quick Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Operations Dashboard</h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time operational triage, cross-team queues, and mission-critical workflows
          </p>
        </div>
        {writableTeams.length > 0 && (
          <button
            onClick={() => setCreateModalOpen(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md shadow-emerald-950 transition"
          >
            <Plus className="w-4 h-4" />
            Create Work Item
          </button>
        )}
      </div>

      {summaryError && (
        <ErrorBanner
          title="Failed to load summary counts"
          message="Could not retrieve dashboard statistics from the server."
          onRetry={refetchSummary}
        />
      )}

      {/* 4 Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Open Items */}
        <div
          onClick={() => navigate('/items?status=OPEN')}
          className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 cursor-pointer transition shadow-sm"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Open Items</span>
            <div className="p-2 rounded-lg bg-slate-800 text-slate-300">
              <FolderOpen className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-white">
              {summaryLoading ? '—' : summary?.openCount ?? 0}
            </span>
            <span className="text-[11px] text-slate-500 block mt-0.5">Across permitted teams</span>
          </div>
        </div>

        {/* Urgent Items */}
        <div
          onClick={() => navigate('/items?priority=URGENT')}
          className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-red-900/50 cursor-pointer transition shadow-sm"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Urgent Incidents</span>
            <div className="p-2 rounded-lg bg-red-950/60 border border-red-800/60 text-red-400">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-red-400">
              {summaryLoading ? '—' : summary?.urgentCount ?? 0}
            </span>
            <span className="text-[11px] text-red-400/70 block mt-0.5">High-priority triage</span>
          </div>
        </div>

        {/* Assigned to Me */}
        <div
          onClick={() => navigate(`/items?assigneeId=${user?.id}`)}
          className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-blue-900/50 cursor-pointer transition shadow-sm"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Assigned to Me</span>
            <div className="p-2 rounded-lg bg-blue-950/60 border border-blue-800/60 text-blue-400">
              <UserCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-blue-400">
              {summaryLoading ? '—' : summary?.assignedToMeCount ?? 0}
            </span>
            <span className="text-[11px] text-slate-500 block mt-0.5">Active operational ownership</span>
          </div>
        </div>

        {/* Awaiting Approval */}
        <div
          onClick={() => navigate('/items?status=AWAITING_APPROVAL')}
          className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-amber-900/50 cursor-pointer transition shadow-sm"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Awaiting Approval</span>
            <div className="p-2 rounded-lg bg-amber-950/60 border border-amber-800/60 text-amber-400">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-amber-400">
              {summaryLoading ? '—' : summary?.awaitingApprovalCount ?? 0}
            </span>
            <span className="text-[11px] text-slate-500 block mt-0.5">Lead / Admin sign-off required</span>
          </div>
        </div>
      </div>

      {/* Target Operational Queues Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Queue 1: Assigned to Me */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-blue-400" />
                <h3 className="text-sm font-semibold text-white">Assigned to Me</h3>
              </div>
              <button
                onClick={() => navigate(`/items?assigneeId=${user?.id}`)}
                className="text-xs text-slate-400 hover:text-white flex items-center gap-1"
              >
                View all <ArrowRight className="w-3 h-3" />
              </button>
            </div>

            {assignedLoading ? (
              <LoadingSpinner message="Loading assigned items..." />
            ) : assignedToMe.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500 border border-dashed border-slate-800 rounded-lg">
                <CheckCircle2 className="w-6 h-6 mx-auto mb-1 text-slate-600" />
                No active items currently assigned to you.
              </div>
            ) : (
              <div className="space-y-2">
                {assignedToMe.map((item) => (
                  <ItemRow key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Queue 2: Unassigned Urgent */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-red-400" />
                <h3 className="text-sm font-semibold text-white">Unassigned Urgent</h3>
              </div>
              <button
                onClick={() => navigate('/items?priority=URGENT')}
                className="text-xs text-slate-400 hover:text-white flex items-center gap-1"
              >
                View all <ArrowRight className="w-3 h-3" />
              </button>
            </div>

            {urgentLoading ? (
              <LoadingSpinner message="Loading urgent items..." />
            ) : unassignedUrgent.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500 border border-dashed border-slate-800 rounded-lg">
                <ShieldCheck className="w-6 h-6 mx-auto mb-1 text-emerald-600" />
                Zero unassigned urgent incidents!
              </div>
            ) : (
              <div className="space-y-2">
                {unassignedUrgent.map((item) => (
                  <ItemRow key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Queue 3: Awaiting My Approval */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-400" />
                <h3 className="text-sm font-semibold text-white">Awaiting Approval</h3>
              </div>
              <button
                onClick={() => navigate('/items?status=AWAITING_APPROVAL')}
                className="text-xs text-slate-400 hover:text-white flex items-center gap-1"
              >
                View all <ArrowRight className="w-3 h-3" />
              </button>
            </div>

            {awaitingLoading ? (
              <LoadingSpinner message="Loading approval queue..." />
            ) : awaitingApproval.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500 border border-dashed border-slate-800 rounded-lg">
                <CheckCircle2 className="w-6 h-6 mx-auto mb-1 text-slate-600" />
                No pending items awaiting approval.
              </div>
            ) : (
              <div className="space-y-2">
                {awaitingApproval.map((item) => (
                  <ItemRow key={item.id} item={item} onClick={() => navigate(`/items/${item.id}`)} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Work Item Create Modal */}
      <WorkItemFormModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={(newItem) => navigate(`/items/${newItem.id}`)}
      />
    </div>
  );
};

// Sub-component for an item queue row
const ItemRow: React.FC<{ item: WorkItemResponse; onClick: () => void }> = ({ item, onClick }) => {
  return (
    <div
      onClick={onClick}
      className="p-3 bg-slate-950/70 hover:bg-slate-800/80 border border-slate-800/80 hover:border-slate-700 rounded-lg cursor-pointer transition flex items-center justify-between gap-3"
    >
      <div className="min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <TypeBadge type={item.type} />
          <PriorityBadge priority={item.priority} />
          <span className="text-[11px] text-slate-500 font-mono">#{item.id}</span>
        </div>
        <p className="text-xs font-medium text-slate-200 truncate">{item.title}</p>
        <span className="text-[10px] text-slate-500 block truncate mt-0.5">
          {item.teamName || 'Unassigned Team'}
        </span>
      </div>
      <div className="shrink-0 flex flex-col items-end gap-1">
        <StatusBadge status={item.status} />
        {item.dueDate && (
          <span className="text-[10px] text-slate-500">Due {item.dueDate}</span>
        )}
      </div>
    </div>
  );
};
