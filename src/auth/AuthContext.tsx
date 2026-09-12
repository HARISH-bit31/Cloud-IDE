import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { User } from '../types';
import { authApi } from '../api/authApi';
import { getToken, setToken, clearToken } from '../api/client';
import { AuthUserResponse, LoginResponse } from '../types/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<LoginResponse>;
  register: (name: string, email: string, password: string) => Promise<AuthUserResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const mapAuthUserToUser = (authUser: AuthUserResponse): User => {
  return {
    id: String(authUser.id),
    name: authUser.name,
    email: authUser.email,
    avatar: authUser.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    role: authUser.role || 'Cloud Developer',
  };
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [tokenState, setTokenState] = useState<string | null>(() => getToken());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(() => {
    clearToken();
    setTokenState(null);
    setUser(null);
  }, []);

  // Initialize session on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = getToken();
      if (!storedToken) {
        setIsLoading(false);
        return;
      }

      try {
        const currentUser = await authApi.getCurrentUser();
        setUser(mapAuthUserToUser(currentUser));
        setTokenState(storedToken);
      } catch (err) {
        console.warn('Session restoration failed or token expired:', err);
        logout();
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, [logout]);

  // Listen to 401 unauthorized events from API client
  useEffect(() => {
    const handleUnauthorized = () => {
      logout();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [logout]);

  const login = async (email: string, password: string): Promise<LoginResponse> => {
    setIsLoading(true);
    try {
      const res = await authApi.login({ email, password });
      setToken(res.token);
      setTokenState(res.token);
      setUser(mapAuthUserToUser(res.user));
      return res;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (name: string, email: string, password: string): Promise<AuthUserResponse> => {
    setIsLoading(true);
    try {
      return await authApi.register({ name, email, password });
    } finally {
      setIsLoading(false);
    }
  };

  const isAuthenticated = Boolean(user && tokenState);

  return (
    <AuthContext.Provider
      value={{
        user,
        token: tokenState,
        isAuthenticated,
        isLoading,
        login,
        register,
        logout,
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
