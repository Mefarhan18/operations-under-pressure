export type WorkItemType = 'INCIDENT' | 'CUSTOMER_ISSUE' | 'PAYMENT' | 'COMPLIANCE' | 'TASK';
export type WorkItemPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type WorkItemStatus = 'OPEN' | 'IN_PROGRESS' | 'AWAITING_APPROVAL' | 'RESOLVED' | 'CLOSED';
export type MembershipRole = 'VIEWER' | 'MEMBER' | 'LEAD' | 'ADMIN';

export interface HealthResponse {
  status: string;
  service: string;
  environment: string;
  timestamp: string;
  details: {
    database?: string;
    databaseProductName?: string;
    databaseProductVersion?: string;
    databaseUrl?: string;
    phase?: string;
    serviceName?: string;
    [key: string]: unknown;
  };
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  enabled: boolean;
  createdAt: string;
}

export interface TeamResponse {
  id: number;
  name: string;
  description: string;
}

export interface MembershipResponse {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  teamId: number;
  teamName: string;
  role: MembershipRole;
  createdAt: string;
}

export interface UserTeamMembershipDTO {
  teamId: number;
  teamName: string;
  role: MembershipRole;
}

export interface UserProfileResponse {
  id: number;
  name: string;
  email: string;
  enabled: boolean;
  createdAt: string;
  memberships: UserTeamMembershipDTO[];
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  name: string;
  email: string;
}

export interface WorkItemResponse {
  id: number;
  title: string;
  description: string;
  type: WorkItemType;
  priority: WorkItemPriority;
  status: WorkItemStatus;
  teamId: number | null;
  teamName: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  createdById: number;
  createdByName: string;
  dueDate: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ActivityEventResponse {
  id: number;
  workItemId: number;
  actorId: number;
  actorName: string;
  actorEmail: string;
  eventType: string;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
}

export interface CommentResponse {
  id: number;
  workItemId: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  body: string;
  createdAt: string;
}

export interface WorkItemSummaryResponse {
  openCount: number;
  urgentCount: number;
  assignedToMeCount: number;
  awaitingApprovalCount: number;
}

export interface CreateWorkItemRequest {
  title: string;
  description?: string;
  type: WorkItemType;
  priority: WorkItemPriority;
  teamId: number;
  dueDate?: string | null;
}

export interface UpdateWorkItemRequest {
  version: number;
  title?: string;
  description?: string;
  priority?: WorkItemPriority;
  dueDate?: string | null;
}

export interface TransitionWorkItemRequest {
  targetStatus: WorkItemStatus;
  version: number;
}

export interface CreateCommentRequest {
  body: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  details?: Record<string, string>;
}
