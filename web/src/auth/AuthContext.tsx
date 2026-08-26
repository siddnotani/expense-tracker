import { createContext, useContext, useMemo, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../lib/api';

export type User = {
  id: string;
  email: string;
  displayName: string;
};

type LoginInput = {
  email: string;
  password: string;
};

type RegisterInput = LoginInput & {
  displayName: string;
};

type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  login: (input: LoginInput) => Promise<User | null>;
  register: (input: RegisterInput) => Promise<User | null>;
  logout: () => Promise<null>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

async function getMe(): Promise<User | null> {
  try {
    return await api<User>('/api/auth/me');
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: getMe, retry: false });
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const meQuery = useMe();
  const loginMutation = useMutation({
    mutationFn: (input: LoginInput) =>
      api<User>('/api/auth/login', { method: 'POST', body: input }),
    onSuccess: (user) => queryClient.setQueryData(['me'], user),
  });
  const registerMutation = useMutation({
    mutationFn: (input: RegisterInput) =>
      api<User>('/api/auth/register', { method: 'POST', body: input }),
    onSuccess: (user) => queryClient.setQueryData(['me'], user),
  });
  const logoutMutation = useMutation({
    mutationFn: () => api<null>('/api/auth/logout', { method: 'POST' }),
    onSuccess: () => queryClient.setQueryData(['me'], null),
  });

  const value = useMemo(
    () => ({
      user: meQuery.data ?? null,
      isLoading: meQuery.isLoading,
      login: loginMutation.mutateAsync,
      register: registerMutation.mutateAsync,
      logout: logoutMutation.mutateAsync,
    }),
    [
      meQuery.data,
      meQuery.isLoading,
      loginMutation.mutateAsync,
      registerMutation.mutateAsync,
      logoutMutation.mutateAsync,
    ],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return value;
}
