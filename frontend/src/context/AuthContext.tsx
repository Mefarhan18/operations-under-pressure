import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { UserProfileResponse, MembershipRole, UserTeamMembershipDTO } from '../types';
import { api } from '../api/client';

interface AuthContextType {
  user: UserProfileResponse | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRoleInTeam: (teamId: number, roles: MembershipRole[]) => boolean;
  canViewTeam: (teamId: number) => boolean;
  canWriteTeam: (teamId: number) => boolean;
  canApproveTeam: (teamId: number) => boolean;
  writableTeams: UserTeamMembershipDTO[];
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('newtonite_token'));
  const [user, setUser] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchProfile = useCallback(async () => {
    try {
      const profile = await api.auth.getMe();
      setUser(profile);
    } catch {
      localStorage.removeItem('newtonite_token');
      setToken(null);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }

    const handleAuthInvalidated = () => {
      setToken(null);
      setUser(null);
      setIsLoading(false);
    };

    window.addEventListener('newtonite_auth_invalidated', handleAuthInvalidated);
    return () => {
      window.removeEventListener('newtonite_auth_invalidated', handleAuthInvalidated);
    };
  }, [token, fetchProfile]);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const authData = await api.auth.login({ email, password });
      localStorage.setItem('newtonite_token', authData.token);
      setToken(authData.token);
      const profile = await api.auth.getMe();
      setUser(profile);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('newtonite_token');
    setToken(null);
    setUser(null);
  };

  const hasRoleInTeam = (teamId: number, roles: MembershipRole[]): boolean => {
    if (!user) return false;
    const membership = user.memberships.find((m) => m.teamId === teamId);
    if (!membership) return false;
    return roles.includes(membership.role);
  };

  const canViewTeam = (teamId: number): boolean => {
    return hasRoleInTeam(teamId, ['VIEWER', 'MEMBER', 'LEAD', 'ADMIN']);
  };

  const canWriteTeam = (teamId: number): boolean => {
    return hasRoleInTeam(teamId, ['MEMBER', 'LEAD', 'ADMIN']);
  };

  const canApproveTeam = (teamId: number): boolean => {
    return hasRoleInTeam(teamId, ['LEAD', 'ADMIN']);
  };

  const writableTeams = user
    ? user.memberships.filter((m) => ['MEMBER', 'LEAD', 'ADMIN'].includes(m.role))
    : [];

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        logout,
        hasRoleInTeam,
        canViewTeam,
        canWriteTeam,
        canApproveTeam,
        writableTeams,
        refreshProfile: fetchProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
