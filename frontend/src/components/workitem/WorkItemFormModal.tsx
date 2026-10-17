import React, { useState, useEffect } from 'react';
import { X, Loader2, AlertCircle } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import {
  WorkItemResponse,
  WorkItemType,
  WorkItemPriority,
  CreateWorkItemRequest,
  UpdateWorkItemRequest,
} from '../../types';
import { useAuth } from '../../context/AuthContext';
import axios from 'axios';

interface WorkItemFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialItem?: WorkItemResponse | null;
  onSuccess?: (item: WorkItemResponse) => void;
}

export const WorkItemFormModal: React.FC<WorkItemFormModalProps> = ({
  isOpen,
  onClose,
  initialItem,
  onSuccess,
}) => {
  const queryClient = useQueryClient();
  const { writableTeams } = useAuth();

  const isEdit = !!initialItem;

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<WorkItemType>('TASK');
  const [priority, setPriority] = useState<WorkItemPriority>('MEDIUM');
  const [teamId, setTeamId] = useState<number | ''>('');
  const [dueDate, setDueDate] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState<string>('');

  useEffect(() => {
    if (initialItem) {
      setTitle(initialItem.title || '');
      setDescription(initialItem.description || '');
      setType(initialItem.type);
      setPriority(initialItem.priority);
      setTeamId(initialItem.teamId || '');
      setDueDate(initialItem.dueDate || '');
    } else {
      setTitle('');
      setDescription('');
      setType('TASK');
      setPriority('MEDIUM');
      setTeamId(writableTeams.length > 0 ? writableTeams[0].teamId : '');
      setDueDate('');
      setIdempotencyKey(crypto.randomUUID());
    }
    setFormError(null);
  }, [initialItem, isOpen, writableTeams]);

  // Create Mutation
  const createMutation = useMutation({
    mutationFn: (payload: { item: CreateWorkItemRequest; key?: string }) =>
      api.workItems.create(payload.item, payload.key),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['work-items'] });
      queryClient.invalidateQueries({ queryKey: ['work-item-summary'] });
      if (onSuccess) onSuccess(data);
      onClose();
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) {
        setFormError(err.response?.data?.message || 'Failed to create work item');
      } else {
        setFormError('An unexpected error occurred');
      }
    },
  });

  // Update Mutation
  const updateMutation = useMutation({
    mutationFn: (payload: { id: number; data: UpdateWorkItemRequest }) =>
      api.workItems.update(payload.id, payload.data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['work-items'] });
      queryClient.invalidateQueries({ queryKey: ['work-item-summary'] });
      queryClient.invalidateQueries({ queryKey: ['work-item', data.id] });
      queryClient.invalidateQueries({ queryKey: ['work-item-activity', data.id] });
      if (onSuccess) onSuccess(data);
      onClose();
    },
    onError: (err: unknown) => {
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 409) {
          setFormError('Conflict (409): Item has been modified by another user. Please reload the latest version.');
        } else {
          setFormError(err.response?.data?.message || 'Failed to update work item');
        }
      } else {
        setFormError('An unexpected error occurred');
      }
    },
  });

  if (!isOpen) return null;

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!title.trim()) {
      setFormError('Title is required');
      return;
    }

    if (!isEdit && !teamId) {
      setFormError('Target operational team is required');
      return;
    }

    if (isEdit && initialItem) {
      updateMutation.mutate({
        id: initialItem.id,
        data: {
          version: initialItem.version,
          title: title.trim(),
          description: description.trim() || undefined,
          priority,
          dueDate: dueDate || null,
        },
      });
    } else {
      createMutation.mutate({
        item: {
          title: title.trim(),
          description: description.trim() || undefined,
          type,
          priority,
          teamId: Number(teamId),
          dueDate: dueDate || null,
        },
        key: idempotencyKey,
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-white">
              {isEdit ? `Edit Work Item #${initialItem?.id}` : 'Create New Work Item'}
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              {isEdit
                ? `Optimistic lock version: ${initialItem?.version}`
                : 'Initialize an operational task, incident, or compliance item'}
            </p>
          </div>
          <button
            onClick={onClose}
            disabled={isSubmitting}
            className="text-slate-400 hover:text-white p-1 rounded-md transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto space-y-4">
          {formError && (
            <div className="p-3 rounded-lg bg-red-950/60 border border-red-800 text-red-200 text-xs flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Title <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={255}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Critical Gateway Latency Spike in EU-West"
              disabled={isSubmitting}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Type <span className="text-red-400">*</span>
              </label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as WorkItemType)}
                disabled={isSubmitting || isEdit}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500 disabled:opacity-60"
              >
                <option value="INCIDENT">Incident</option>
                <option value="CUSTOMER_ISSUE">Customer Issue</option>
                <option value="PAYMENT">Payment Dispute</option>
                <option value="COMPLIANCE">Compliance Item</option>
                <option value="TASK">Operational Task</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Priority <span className="text-red-400">*</span>
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as WorkItemPriority)}
                disabled={isSubmitting}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>
          </div>

          {!isEdit && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Assigned Team <span className="text-red-400">*</span>
              </label>
              {writableTeams.length === 0 ? (
                <div className="text-xs text-amber-400 bg-amber-950/40 p-2.5 rounded border border-amber-800">
                  You are not a Member, Lead, or Admin in any operational team. Viewers cannot create work items.
                </div>
              ) : (
                <select
                  value={teamId}
                  onChange={(e) => setTeamId(Number(e.target.value))}
                  disabled={isSubmitting}
                  required
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="">Select Permitted Team...</option>
                  {writableTeams.map((m) => (
                    <option key={m.teamId} value={m.teamId}>
                      {m.teamName} ({m.role})
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Due Date (Optional)
            </label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              disabled={isSubmitting}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Description (Optional)
            </label>
            <textarea
              rows={4}
              maxLength={5000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide context, reproduction steps, or impact analysis..."
              disabled={isSubmitting}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          {/* Modal Footer */}
          <div className="pt-4 border-t border-slate-800 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || (!isEdit && writableTeams.length === 0)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800/50 text-white text-xs font-semibold rounded-lg shadow transition flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Saving...</span>
                </>
              ) : (
                <span>{isEdit ? 'Save Changes' : 'Create Item'}</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
