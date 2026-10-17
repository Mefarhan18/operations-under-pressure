import React from 'react';
import { WorkItemStatus, WorkItemPriority, WorkItemType, MembershipRole } from '../../types';

export const StatusBadge: React.FC<{ status: WorkItemStatus }> = ({ status }) => {
  const configs: Record<WorkItemStatus, { label: string; bg: string; text: string; dot: string }> = {
    OPEN: {
      label: 'Open',
      bg: 'bg-slate-800 border-slate-700',
      text: 'text-slate-300',
      dot: 'bg-slate-400',
    },
    IN_PROGRESS: {
      label: 'In Progress',
      bg: 'bg-blue-950/60 border-blue-800/60',
      text: 'text-blue-300',
      dot: 'bg-blue-400',
    },
    AWAITING_APPROVAL: {
      label: 'Awaiting Approval',
      bg: 'bg-amber-950/60 border-amber-800/60',
      text: 'text-amber-300',
      dot: 'bg-amber-400',
    },
    RESOLVED: {
      label: 'Resolved',
      bg: 'bg-emerald-950/60 border-emerald-800/60',
      text: 'text-emerald-300',
      dot: 'bg-emerald-400',
    },
    CLOSED: {
      label: 'Closed',
      bg: 'bg-zinc-900 border-zinc-800',
      text: 'text-zinc-400',
      dot: 'bg-zinc-500',
    },
  };

  const config = configs[status] || configs.OPEN;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${config.bg} ${config.text}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
};

export const PriorityBadge: React.FC<{ priority: WorkItemPriority }> = ({ priority }) => {
  const configs: Record<WorkItemPriority, { label: string; bg: string; text: string }> = {
    LOW: {
      label: 'Low',
      bg: 'bg-slate-800/70 border-slate-700/60 text-slate-300',
      text: 'text-slate-300',
    },
    MEDIUM: {
      label: 'Medium',
      bg: 'bg-blue-950/50 border-blue-800/50 text-blue-300',
      text: 'text-blue-300',
    },
    HIGH: {
      label: 'High',
      bg: 'bg-orange-950/60 border-orange-800/60 text-orange-300',
      text: 'text-orange-300',
    },
    URGENT: {
      label: 'URGENT',
      bg: 'bg-red-950/70 border-red-800/70 text-red-300 font-semibold animate-pulse',
      text: 'text-red-300',
    },
  };

  const config = configs[priority] || configs.LOW;

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${config.bg}`}>
      {config.label}
    </span>
  );
};

export const TypeBadge: React.FC<{ type: WorkItemType }> = ({ type }) => {
  const formatType = (t: WorkItemType) => {
    switch (t) {
      case 'INCIDENT':
        return 'Incident';
      case 'CUSTOMER_ISSUE':
        return 'Customer Issue';
      case 'PAYMENT':
        return 'Payment';
      case 'COMPLIANCE':
        return 'Compliance';
      case 'TASK':
        return 'Task';
      default:
        return t;
    }
  };

  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-mono bg-slate-900 border border-slate-800 text-slate-300">
      {formatType(type)}
    </span>
  );
};

export const RoleBadge: React.FC<{ role: MembershipRole }> = ({ role }) => {
  const configs: Record<MembershipRole, { bg: string; text: string }> = {
    VIEWER: { bg: 'bg-slate-800 border-slate-700 text-slate-400', text: 'Viewer' },
    MEMBER: { bg: 'bg-blue-950 border-blue-800 text-blue-300', text: 'Member' },
    LEAD: { bg: 'bg-indigo-950 border-indigo-800 text-indigo-300', text: 'Lead' },
    ADMIN: { bg: 'bg-purple-950 border-purple-800 text-purple-300', text: 'Admin' },
  };

  const config = configs[role] || configs.VIEWER;

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${config.bg}`}>
      {config.text}
    </span>
  );
};
