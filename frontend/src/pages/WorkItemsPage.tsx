import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Search,
  Plus,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  User,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { WorkItemStatus, WorkItemPriority } from '../types';
import { StatusBadge, PriorityBadge, TypeBadge } from '../components/common/Badges';
import { LoadingSpinner, ErrorBanner, EmptyState } from '../components/common/Feedback';
import { WorkItemFormModal } from '../components/workitem/WorkItemFormModal';

export const WorkItemsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user, writableTeams } = useAuth();

  // Extract query parameters from URL for bookmarkability & sync
  const statusParam = (searchParams.get('status') as WorkItemStatus) || '';
  const priorityParam = (searchParams.get('priority') as WorkItemPriority) || '';
  const teamIdParam = searchParams.get('teamId') ? Number(searchParams.get('teamId')) : '';
  const assigneeIdParam = searchParams.get('assigneeId') ? Number(searchParams.get('assigneeId')) : '';
  const searchParam = searchParams.get('search') || '';
  const pageParam = searchParams.get('page') ? Number(searchParams.get('page')) : 0;
  const sizeParam = searchParams.get('size') ? Number(searchParams.get('size')) : 10;

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [searchInput, setSearchInput] = useState(searchParam);

  // TanStack Query with server-side pagination & filtering
  const { data, isLoading, isFetching, error, refetch } = useQuery({
    queryKey: [
      'work-items',
      {
        page: pageParam,
        size: sizeParam,
        status: statusParam || undefined,
        priority: priorityParam || undefined,
        teamId: teamIdParam || undefined,
        assigneeId: assigneeIdParam || undefined,
        search: searchParam || undefined,
      },
    ],
    queryFn: () =>
      api.workItems.getAll({
        page: pageParam,
        size: sizeParam,
        status: statusParam ? statusParam : undefined,
        priority: priorityParam ? priorityParam : undefined,
        teamId: teamIdParam ? teamIdParam : undefined,
        assigneeId: assigneeIdParam ? assigneeIdParam : undefined,
        search: searchParam ? searchParam : undefined,
        sortBy: 'updatedAt',
        sortDir: 'desc',
      }),
  });

  const updateFilters = (newParams: Record<string, string | number | undefined>) => {
    const updated = new URLSearchParams(searchParams);
    Object.entries(newParams).forEach(([k, v]) => {
      if (v === undefined || v === '') {
        updated.delete(k);
      } else {
        updated.set(k, String(v));
      }
    });
    // Reset to page 0 on any filter change (unless page is explicitly changed)
    if (!('page' in newParams)) {
      updated.set('page', '0');
    }
    setSearchParams(updated);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateFilters({ search: searchInput.trim() || undefined });
  };

  const resetFilters = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams());
  };

  const activeFilterCount = [
    statusParam,
    priorityParam,
    teamIdParam,
    assigneeIdParam,
    searchParam,
  ].filter(Boolean).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Work Items</h1>
          <p className="text-xs text-slate-400 mt-1">
            Browse, filter, assign, and track operational items across permitted teams
          </p>
        </div>
        {writableTeams.length > 0 && (
          <button
            onClick={() => setCreateModalOpen(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-md transition"
          >
            <Plus className="w-4 h-4" />
            Create Work Item
          </button>
        )}
      </div>

      {/* Filter & Search Toolbar */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm space-y-3">
        <div className="flex flex-col lg:flex-row gap-3">
          {/* Search Bar */}
          <form onSubmit={handleSearchSubmit} className="flex-1 relative flex">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search title, description, keywords..."
              className="w-full pl-9 pr-20 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            <button
              type="submit"
              className="absolute right-1.5 top-1.5 px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded transition"
            >
              Search
            </button>
          </form>

          {/* Quick Filter Selects */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Status Filter */}
            <select
              value={statusParam}
              onChange={(e) => updateFilters({ status: e.target.value || undefined })}
              className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="">Status: All</option>
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="AWAITING_APPROVAL">Awaiting Approval</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>

            {/* Priority Filter */}
            <select
              value={priorityParam}
              onChange={(e) => updateFilters({ priority: e.target.value || undefined })}
              className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="">Priority: All</option>
              <option value="URGENT">URGENT</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>

            {/* Team Filter */}
            {user?.memberships && user.memberships.length > 1 && (
              <select
                value={teamIdParam}
                onChange={(e) => updateFilters({ teamId: e.target.value || undefined })}
                className="px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="">Team: All</option>
                {user.memberships.map((m) => (
                  <option key={m.teamId} value={m.teamId}>
                    {m.teamName}
                  </option>
                ))}
              </select>
            )}

            {/* Assignee Filter Quick Toggle */}
            <button
              type="button"
              onClick={() =>
                updateFilters({
                  assigneeId: assigneeIdParam === user?.id ? undefined : user?.id,
                })
              }
              className={`px-3 py-2 rounded-lg text-xs font-medium border flex items-center gap-1.5 transition ${
                assigneeIdParam === user?.id
                  ? 'bg-blue-950 border-blue-700 text-blue-300'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              <User className="w-3.5 h-3.5" />
              Assigned to Me
            </button>

            {/* Reset Filters */}
            {activeFilterCount > 0 && (
              <button
                type="button"
                onClick={resetFilters}
                className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition"
                title="Reset filters"
              >
                <RotateCcw className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Results Metadata */}
        <div className="flex items-center justify-between text-xs text-slate-500 pt-1 border-t border-slate-800/40">
          <div className="flex items-center gap-2">
            <span>
              Showing {data ? data.content.length : 0} of {data?.totalElements ?? 0} items
            </span>
            {isFetching && <span className="text-emerald-500 font-mono animate-pulse">Refreshing...</span>}
          </div>
          {activeFilterCount > 0 && (
            <span className="text-[11px] text-emerald-400">
              {activeFilterCount} active filter{activeFilterCount > 1 ? 's' : ''} applied
            </span>
          )}
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <ErrorBanner
          title="Error loading work items"
          message="Could not fetch operational items from server."
          onRetry={() => refetch()}
        />
      )}

      {/* Table Container */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="py-16">
            <LoadingSpinner message="Querying work items..." />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="p-8">
            <EmptyState
              title="No work items found"
              description={
                activeFilterCount > 0
                  ? 'Try broadening your search keywords or resetting filters.'
                  : 'Create your first work item to kick off operational workflows.'
              }
              action={
                activeFilterCount > 0
                  ? { label: 'Clear Filters', onClick: resetFilters }
                  : writableTeams.length > 0
                  ? { label: 'Create Work Item', onClick: () => setCreateModalOpen(true) }
                  : undefined
              }
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-950/80 border-b border-slate-800 text-slate-400 uppercase font-mono tracking-wider text-[10px]">
                  <th className="py-3 px-4">Item</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Team</th>
                  <th className="py-3 px-4">Priority</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Assignee</th>
                  <th className="py-3 px-4">Due Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {data.content.map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => navigate(`/items/${item.id}`)}
                    className="hover:bg-slate-800/40 cursor-pointer transition group"
                  >
                    {/* Item ID + Title */}
                    <td className="py-3.5 px-4 max-w-xs">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-slate-500 text-[11px]">#{item.id}</span>
                        <span className="font-medium text-slate-200 group-hover:text-emerald-400 transition truncate">
                          {item.title}
                        </span>
                      </div>
                    </td>

                    {/* Type */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <TypeBadge type={item.type} />
                    </td>

                    {/* Team */}
                    <td className="py-3.5 px-4 text-slate-300 whitespace-nowrap">
                      {item.teamName || '—'}
                    </td>

                    {/* Priority */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <PriorityBadge priority={item.priority} />
                    </td>

                    {/* Status */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <StatusBadge status={item.status} />
                    </td>

                    {/* Assignee */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {item.assigneeName ? (
                        <span className="text-slate-300 font-medium">{item.assigneeName}</span>
                      ) : (
                        <span className="inline-block px-2 py-0.5 rounded text-[10px] font-mono bg-slate-950 border border-slate-800 text-slate-500">
                          Unassigned
                        </span>
                      )}
                    </td>

                    {/* Due Date */}
                    <td className="py-3.5 px-4 whitespace-nowrap text-slate-400">
                      {item.dueDate ? item.dueDate : '—'}
                    </td>

                    {/* Action */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <span className="text-xs text-emerald-400 group-hover:underline font-medium">
                        View Details →
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Server-Side Pagination Bar */}
        {data && data.totalPages > 1 && (
          <div className="px-4 py-3 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <div>
              Page <span className="font-semibold text-slate-200">{data.page + 1}</span> of{' '}
              <span className="font-semibold text-slate-200">{data.totalPages}</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                disabled={data.page === 0 || isFetching}
                onClick={() => updateFilters({ page: data.page - 1 })}
                className="px-3 py-1.5 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:hover:bg-slate-800 text-slate-300 text-xs font-medium transition flex items-center gap-1"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                Previous
              </button>
              <button
                disabled={data.last || isFetching}
                onClick={() => updateFilters({ page: data.page + 1 })}
                className="px-3 py-1.5 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:hover:bg-slate-800 text-slate-300 text-xs font-medium transition flex items-center gap-1"
              >
                Next
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      <WorkItemFormModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={(newItem) => navigate(`/items/${newItem.id}`)}
      />
    </div>
  );
};
