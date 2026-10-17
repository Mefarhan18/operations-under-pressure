import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  Calendar,
  Clock,
  User,
  Shield,
  Layers,
  Edit,
  UserPlus,
  Send,
  AlertTriangle,
  RefreshCw,
  Loader2,
  CheckCircle,
  MessageSquare,
  History,
  Lock,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { StatusBadge, PriorityBadge, TypeBadge } from '../components/common/Badges';
import { LoadingSpinner, ErrorBanner } from '../components/common/Feedback';
import { WorkItemFormModal } from '../components/workitem/WorkItemFormModal';
import { WorkItemStatus } from '../types';
import axios from 'axios';

export const WorkItemDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const itemId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { canWriteTeam, canApproveTeam } = useAuth();

  const [editModalOpen, setEditModalOpen] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [conflictError, setConflictError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // 1. Work Item Query
  const {
    data: item,
    isLoading: itemLoading,
    error: itemError,
    refetch: refetchItem,
  } = useQuery({
    queryKey: ['work-item', itemId],
    queryFn: () => api.workItems.getById(itemId),
    enabled: !isNaN(itemId),
  });

  // 2. Activity History Query
  const {
    data: activity,
    isLoading: activityLoading,
    refetch: refetchActivity,
  } = useQuery({
    queryKey: ['work-item-activity', itemId],
    queryFn: () => api.workItems.getActivity(itemId),
    enabled: !isNaN(itemId),
  });

  // 3. Comments Query
  const {
    data: comments,
    isLoading: commentsLoading,
    refetch: refetchComments,
  } = useQuery({
    queryKey: ['work-item-comments', itemId],
    queryFn: () => api.workItems.getComments(itemId),
    enabled: !isNaN(itemId),
  });

  // Reload everything cleanly
  const handleReloadAll = () => {
    setConflictError(null);
    setActionError(null);
    refetchItem();
    refetchActivity();
    refetchComments();
  };

  // Claim Mutation
  const claimMutation = useMutation({
    mutationFn: () => api.workItems.claim(itemId),
    onSuccess: () => {
      setConflictError(null);
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['work-item', itemId] });
      queryClient.invalidateQueries({ queryKey: ['work-item-activity', itemId] });
      queryClient.invalidateQueries({ queryKey: ['work-items'] });
      queryClient.invalidateQueries({ queryKey: ['work-item-summary'] });
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 409) {
          setConflictError(
            err.response.data?.message ||
              'Conflict (409): Work item was already claimed by another user or the item version is stale.'
          );
        } else {
          setActionError(err.response?.data?.message || 'Failed to claim work item.');
        }
      } else {
        setActionError('An unexpected error occurred while claiming.');
      }
    },
  });

  // Transition Mutation
  const transitionMutation = useMutation({
    mutationFn: (targetStatus: WorkItemStatus) => {
      if (!item) throw new Error('Work item not loaded');
      return api.workItems.transition(itemId, {
        targetStatus,
        version: item.version,
      });
    },
    onSuccess: () => {
      setConflictError(null);
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['work-item', itemId] });
      queryClient.invalidateQueries({ queryKey: ['work-item-activity', itemId] });
      queryClient.invalidateQueries({ queryKey: ['work-items'] });
      queryClient.invalidateQueries({ queryKey: ['work-item-summary'] });
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 409) {
          setConflictError(
            err.response.data?.message ||
              'Conflict (409): State conflict or stale version detected. The item has been modified by someone else.'
          );
        } else if (err.response?.status === 403) {
          setActionError(
            err.response.data?.message ||
              'Forbidden (403): You do not have permission to execute this status transition.'
          );
        } else {
          setActionError(err.response?.data?.message || 'Failed to update work item status.');
        }
      } else {
        setActionError('An unexpected error occurred while transitioning status.');
      }
    },
  });

  // Comment Mutation
  const commentMutation = useMutation({
    mutationFn: (body: string) => api.workItems.addComment(itemId, { body }),
    onSuccess: () => {
      setCommentText('');
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['work-item-comments', itemId] });
      queryClient.invalidateQueries({ queryKey: ['work-item-activity', itemId] });
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) {
        setActionError(err.response?.data?.message || 'Failed to post comment.');
      } else {
        setActionError('An unexpected error occurred while posting comment.');
      }
    },
  });

  if (itemLoading) {
    return <LoadingSpinner message={`Loading Work Item #${itemId}...`} fullPage={false} />;
  }

  if (itemError || !item) {
    const isForbidden = axios.isAxiosError(itemError) && itemError.response?.status === 403;
    return (
      <div className="space-y-4">
        <button
          onClick={() => navigate('/items')}
          className="text-xs text-slate-400 hover:text-white flex items-center gap-1.5"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Work Items
        </button>
        <ErrorBanner
          title={isForbidden ? 'Access Denied (403)' : 'Work Item Not Found'}
          message={
            isForbidden
              ? 'You do not hold active membership in this work item’s operational team. Access is restricted.'
              : 'The requested work item could not be retrieved from the server.'
          }
          status={isForbidden ? 403 : 404}
          onRetry={() => navigate('/items')}
          actionText="Return to Items"
        />
      </div>
    );
  }

  const teamId = item.teamId ?? 0;
  const canWrite = canWriteTeam(teamId);
  const canApprove = canApproveTeam(teamId);
  const isUnassigned = item.assigneeId === null;
  const isActionPending =
    claimMutation.isPending || transitionMutation.isPending || commentMutation.isPending;

  const handleAddComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    commentMutation.mutate(commentText.trim());
  };

  return (
    <div className="space-y-6">
      {/* Top Navigation & Breadcrumbs */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/items')}
          className="text-xs text-slate-400 hover:text-white flex items-center gap-1.5 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Work Items
        </button>
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono text-slate-500">v{item.version}</span>
          <button
            onClick={handleReloadAll}
            title="Reload latest state"
            className="p-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-800 text-xs flex items-center gap-1 transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Refresh</span>
          </button>
        </div>
      </div>

      {/* Conflict / 409 Alert Banner */}
      {conflictError && (
        <div className="p-4 rounded-xl bg-amber-950/50 border border-amber-800 text-amber-200 text-sm flex items-start justify-between gap-4 animate-in fade-in">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-white">Concurrency Conflict (HTTP 409)</h4>
              <p className="mt-0.5 text-slate-300 text-xs">{conflictError}</p>
              <p className="mt-1 text-slate-400 text-[11px]">
                Another operator modified this item or claimed it ahead of your request. Please reload to sync with the latest state.
              </p>
            </div>
          </div>
          <button
            onClick={handleReloadAll}
            className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold shadow transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Reload Latest Data
          </button>
        </div>
      )}

      {/* Generic Action Error Banner */}
      {actionError && (
        <ErrorBanner
          title="Operation Failed"
          message={actionError}
          onRetry={() => setActionError(null)}
          actionText="Dismiss"
        />
      )}

      {/* Item Title & High-Level Metadata Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-xs text-slate-400">#{item.id}</span>
              <TypeBadge type={item.type} />
              <PriorityBadge priority={item.priority} />
              <StatusBadge status={item.status} />
              <span className="text-[11px] font-mono text-slate-500 px-2 py-0.5 rounded bg-slate-950 border border-slate-800">
                Lock v{item.version}
              </span>
            </div>
            <h1 className="text-xl font-bold text-white tracking-tight">{item.title}</h1>
          </div>

          {/* Quick Edit Button */}
          {canWrite && item.status !== 'CLOSED' && (
            <button
              onClick={() => setEditModalOpen(true)}
              className="self-start lg:self-center inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition"
            >
              <Edit className="w-3.5 h-3.5" />
              Edit Work Item
            </button>
          )}
        </div>
      </div>

      {/* Two Column Layout: Main Details + Action Center */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 cols): Description, Metadata, Comments */}
        <div className="lg:col-span-2 space-y-6">
          {/* Description Section */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
              Description & Context
            </h3>
            <p className="text-sm text-slate-200 leading-relaxed whitespace-pre-wrap">
              {item.description || (
                <span className="italic text-slate-500">No detailed description provided.</span>
              )}
            </p>
          </div>

          {/* Metadata Grid */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
            <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
              Operational Metadata
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs">
              <div>
                <span className="text-slate-500 block">Assigned Team</span>
                <span className="text-slate-200 font-medium mt-0.5 flex items-center gap-1">
                  <Layers className="w-3.5 h-3.5 text-slate-400" />
                  {item.teamName || 'None'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Assignee</span>
                <span className="text-slate-200 font-medium mt-0.5 flex items-center gap-1">
                  <User className="w-3.5 h-3.5 text-slate-400" />
                  {item.assigneeName || <span className="text-amber-400 italic">Unassigned</span>}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Created By</span>
                <span className="text-slate-200 font-medium mt-0.5">{item.createdByName}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Due Date</span>
                <span className="text-slate-200 font-medium mt-0.5 flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  {item.dueDate || 'No due date'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Created At</span>
                <span className="text-slate-400 mt-0.5">
                  {new Date(item.createdAt).toLocaleString()}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Last Updated</span>
                <span className="text-slate-400 mt-0.5">
                  {new Date(item.updatedAt).toLocaleString()}
                </span>
              </div>
            </div>
          </div>

          {/* Discussion & Comments */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-white">Operational Discussion</h3>
              <span className="text-xs text-slate-500 font-mono">
                ({comments ? comments.length : 0})
              </span>
            </div>

            {/* Comment Submission Form */}
            {canWrite ? (
              <form onSubmit={handleAddComment} className="space-y-2">
                <textarea
                  rows={2}
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Post an operational update, blocker, or review note..."
                  disabled={isActionPending}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-xs text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={isActionPending || !commentText.trim()}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800/40 text-white text-xs font-semibold shadow transition"
                  >
                    {commentMutation.isPending ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Send className="w-3.5 h-3.5" />
                    )}
                    <span>Post Comment</span>
                  </button>
                </div>
              </form>
            ) : (
              <div className="text-xs text-slate-500 p-3 rounded bg-slate-950/50 border border-slate-800">
                You have Viewer access only and cannot submit comments.
              </div>
            )}

            {/* Comment Thread List */}
            {commentsLoading ? (
              <LoadingSpinner message="Loading discussion..." />
            ) : !comments || comments.length === 0 ? (
              <p className="text-xs text-slate-500 italic py-2">No comments posted yet.</p>
            ) : (
              <div className="space-y-3 pt-2">
                {comments.map((c) => (
                  <div
                    key={c.id}
                    className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80 space-y-1.5"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-slate-200">{c.authorName}</span>
                        <span className="text-[10px] text-slate-500">{c.authorEmail}</span>
                      </div>
                      <span className="text-[10px] text-slate-500">
                        {new Date(c.createdAt).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                    <p className="text-xs text-slate-300 whitespace-pre-wrap">{c.body}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column (1 col): Action Center & Audit History */}
        <div className="space-y-6">
          {/* Action Center Card */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-800">
              <Shield className="w-4 h-4 text-emerald-400" />
              <h3 className="text-sm font-semibold text-white">Operational Actions</h3>
            </div>

            {/* Claim Action */}
            {isUnassigned && (
              <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 space-y-2">
                <span className="text-xs font-semibold text-slate-200 block">Item Unassigned</span>
                <p className="text-[11px] text-slate-400">
                  Atomically claim ownership of this item to start triage and resolution.
                </p>
                {canWrite ? (
                  <button
                    onClick={() => claimMutation.mutate()}
                    disabled={isActionPending}
                    className="w-full py-2 px-3 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800/50 text-white text-xs font-semibold shadow transition flex items-center justify-center gap-2"
                  >
                    {claimMutation.isPending ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <UserPlus className="w-3.5 h-3.5" />
                    )}
                    <span>Claim Work Item</span>
                  </button>
                ) : (
                  <span className="text-[11px] text-slate-500 italic block">
                    Viewer access: claiming requires Member or Lead role.
                  </span>
                )}
              </div>
            )}

            {/* Status Transitions */}
            <div className="space-y-2">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block">
                Workflow State Transitions
              </span>

              {item.status === 'OPEN' && (
                <div>
                  {canWrite ? (
                    <button
                      onClick={() => transitionMutation.mutate('IN_PROGRESS')}
                      disabled={isActionPending}
                      className="w-full py-2 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800/50 text-white text-xs font-semibold shadow transition flex items-center justify-center gap-2"
                    >
                      {transitionMutation.isPending ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <CheckCircle className="w-3.5 h-3.5" />
                      )}
                      <span>Start Progress (OPEN → IN_PROGRESS)</span>
                    </button>
                  ) : (
                    <p className="text-xs text-slate-500 italic">Viewers cannot transition status.</p>
                  )}
                </div>
              )}

              {item.status === 'IN_PROGRESS' && (
                <div>
                  {canWrite ? (
                    <button
                      onClick={() => transitionMutation.mutate('AWAITING_APPROVAL')}
                      disabled={isActionPending}
                      className="w-full py-2 px-3 rounded-lg bg-amber-600 hover:bg-amber-500 disabled:bg-amber-800/50 text-white text-xs font-semibold shadow transition flex items-center justify-center gap-2"
                    >
                      {transitionMutation.isPending ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Clock className="w-3.5 h-3.5" />
                      )}
                      <span>Submit for Approval (→ AWAITING_APPROVAL)</span>
                    </button>
                  ) : (
                    <p className="text-xs text-slate-500 italic">Viewers cannot transition status.</p>
                  )}
                </div>
              )}

              {item.status === 'AWAITING_APPROVAL' && (
                <div className="space-y-2">
                  {canApprove ? (
                    <button
                      onClick={() => transitionMutation.mutate('RESOLVED')}
                      disabled={isActionPending}
                      className="w-full py-2 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800/50 text-white text-xs font-semibold shadow transition flex items-center justify-center gap-2"
                    >
                      {transitionMutation.isPending ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <CheckCircle className="w-3.5 h-3.5" />
                      )}
                      <span>Approve & Resolve (→ RESOLVED)</span>
                    </button>
                  ) : (
                    <div className="p-3 rounded-lg bg-slate-950/80 border border-slate-800 text-xs text-amber-400 flex items-start gap-2">
                      <Lock className="w-4 h-4 shrink-0 mt-0.5" />
                      <span>
                        Approval is locked. Only a <strong>Lead</strong> or <strong>Admin</strong> in{' '}
                        {item.teamName} can approve and resolve this item.
                      </span>
                    </div>
                  )}
                </div>
              )}

              {item.status === 'RESOLVED' && (
                <div>
                  {canWrite ? (
                    <button
                      onClick={() => transitionMutation.mutate('CLOSED')}
                      disabled={isActionPending}
                      className="w-full py-2 px-3 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-white text-xs font-semibold border border-slate-700 transition flex items-center justify-center gap-2"
                    >
                      {transitionMutation.isPending ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <CheckCircle className="w-3.5 h-3.5 text-zinc-400" />
                      )}
                      <span>Close Work Item (RESOLVED → CLOSED)</span>
                    </button>
                  ) : (
                    <p className="text-xs text-slate-500 italic">Viewers cannot close items.</p>
                  )}
                </div>
              )}

              {item.status === 'CLOSED' && (
                <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 text-xs text-slate-400 text-center">
                  Work item is closed. Workflow complete.
                </div>
              )}
            </div>
          </div>

          {/* Activity Audit Timeline */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-3">
            <div className="flex items-center gap-2 pb-2 border-b border-slate-800">
              <History className="w-4 h-4 text-slate-400" />
              <h3 className="text-sm font-semibold text-white">Audit Trail History</h3>
            </div>

            {activityLoading ? (
              <LoadingSpinner message="Loading audit history..." />
            ) : !activity || activity.length === 0 ? (
              <p className="text-xs text-slate-500 italic">No activity recorded yet.</p>
            ) : (
              <div className="space-y-3 relative pl-4 before:absolute before:left-1 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-800">
                {activity.map((event) => (
                  <div key={event.id} className="relative text-xs">
                    <span className="absolute -left-[17px] top-1 w-2 h-2 rounded-full bg-emerald-500 ring-4 ring-slate-900" />
                    <div className="font-semibold text-slate-200">
                      {event.eventType.replace(/_/g, ' ')}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-0.5">
                      by <span className="text-slate-300 font-medium">{event.actorName}</span>
                    </div>
                    {(event.oldValue || event.newValue) && (
                      <div className="text-[10px] text-slate-500 mt-0.5 font-mono">
                        {event.oldValue && <span>{event.oldValue} → </span>}
                        <span>{event.newValue}</span>
                      </div>
                    )}
                    <div className="text-[10px] text-slate-600 mt-0.5">
                      {new Date(event.createdAt).toLocaleString()}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Edit Modal */}
      <WorkItemFormModal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        initialItem={item}
        onSuccess={() => refetchItem()}
      />
    </div>
  );
};
