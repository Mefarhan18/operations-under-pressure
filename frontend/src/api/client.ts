import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import {
  HealthResponse,
  UserResponse,
  TeamResponse,
  WorkItemResponse,
  UserProfileResponse,
  AuthResponse,
  PagedResponse,
  ActivityEventResponse,
  CommentResponse,
  WorkItemSummaryResponse,
  CreateWorkItemRequest,
  UpdateWorkItemRequest,
  TransitionWorkItemRequest,
  CreateCommentRequest,
  WorkItemStatus,
  WorkItemPriority,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Interceptor: Attach JWT bearer token if present in localStorage
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('newtonite_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor: Global handling of 401 Unauthorized (session expired / revoked)
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isLoginEndpoint = error.config?.url?.includes('/api/auth/login');
    if (error.response?.status === 401 && !isLoginEndpoint) {
      // Clear token and fire auth-invalidated event for React state
      localStorage.removeItem('newtonite_token');
      window.dispatchEvent(new CustomEvent('newtonite_auth_invalidated'));
    }
    return Promise.reject(error);
  }
);

export interface WorkItemFilterParams {
  teamId?: number;
  status?: WorkItemStatus;
  priority?: WorkItemPriority;
  assigneeId?: number;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const api = {
  // Legacy / top-level helpers
  getHealth: async (): Promise<HealthResponse> => {
    const response = await apiClient.get<HealthResponse>('/api/v1/health');
    return response.data;
  },
  getUsers: async (): Promise<UserResponse[]> => {
    const response = await apiClient.get<UserResponse[]>('/api/users');
    return response.data;
  },
  getTeams: async (): Promise<TeamResponse[]> => {
    const response = await apiClient.get<TeamResponse[]>('/api/teams');
    return response.data;
  },
  getWorkItems: async (): Promise<WorkItemResponse[]> => {
    const response = await apiClient.get<PagedResponse<WorkItemResponse>>('/api/items');
    return response.data.content;
  },

  // Authentication
  auth: {
    login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
      const response = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
      return response.data;
    },
    getMe: async (): Promise<UserProfileResponse> => {
      const response = await apiClient.get<UserProfileResponse>('/api/me');
      return response.data;
    },
  },

  // Work Items
  workItems: {
    getAll: async (params?: WorkItemFilterParams): Promise<PagedResponse<WorkItemResponse>> => {
      const response = await apiClient.get<PagedResponse<WorkItemResponse>>('/api/items', { params });
      return response.data;
    },

    getById: async (id: number): Promise<WorkItemResponse> => {
      const response = await apiClient.get<WorkItemResponse>(`/api/items/${id}`);
      return response.data;
    },

    create: async (payload: CreateWorkItemRequest, idempotencyKey?: string): Promise<WorkItemResponse> => {
      const headers: Record<string, string> = {};
      if (idempotencyKey) {
        headers['Idempotency-Key'] = idempotencyKey;
      }
      const response = await apiClient.post<WorkItemResponse>('/api/items', payload, { headers });
      return response.data;
    },

    update: async (id: number, payload: UpdateWorkItemRequest): Promise<WorkItemResponse> => {
      const response = await apiClient.patch<WorkItemResponse>(`/api/items/${id}`, payload);
      return response.data;
    },

    claim: async (id: number): Promise<WorkItemResponse> => {
      const response = await apiClient.post<WorkItemResponse>(`/api/items/${id}/claim`);
      return response.data;
    },

    transition: async (id: number, payload: TransitionWorkItemRequest): Promise<WorkItemResponse> => {
      const response = await apiClient.post<WorkItemResponse>(`/api/items/${id}/transition`, payload);
      return response.data;
    },

    getActivity: async (id: number): Promise<ActivityEventResponse[]> => {
      const response = await apiClient.get<ActivityEventResponse[]>(`/api/items/${id}/activity`);
      return response.data;
    },

    getComments: async (id: number): Promise<CommentResponse[]> => {
      const response = await apiClient.get<CommentResponse[]>(`/api/items/${id}/comments`);
      return response.data;
    },

    addComment: async (id: number, payload: CreateCommentRequest): Promise<CommentResponse> => {
      const response = await apiClient.post<CommentResponse>(`/api/items/${id}/comments`, payload);
      return response.data;
    },

    getSummary: async (): Promise<WorkItemSummaryResponse> => {
      const response = await apiClient.get<WorkItemSummaryResponse>('/api/items/summary');
      return response.data;
    },
  },

  // Teams & Users & Health
  teams: {
    getAll: async (): Promise<TeamResponse[]> => {
      const response = await apiClient.get<TeamResponse[]>('/api/teams');
      return response.data;
    },
    getById: async (id: number): Promise<TeamResponse> => {
      const response = await apiClient.get<TeamResponse>(`/api/teams/${id}`);
      return response.data;
    },
  },

  users: {
    getAll: async (): Promise<UserResponse[]> => {
      const response = await apiClient.get<UserResponse[]>('/api/users');
      return response.data;
    },
  },

  health: {
    getHealth: async (): Promise<HealthResponse> => {
      const response = await apiClient.get<HealthResponse>('/api/v1/health');
      return response.data;
    },
  },
};
