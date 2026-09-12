import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Logo } from '../common/Logo';
import { Loader2 } from 'lucide-react';

interface ProtectedRouteProps {
  children?: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen w-screen bg-[#11131c] flex flex-col items-center justify-center p-4 select-none">
        <div className="space-y-4 text-center">
          <Logo size={40} />
          <div className="flex items-center justify-center gap-2 text-xs font-mono text-[#dcbfc9]/70 pt-2">
            <Loader2 className="w-4 h-4 text-[#7bd0ff] animate-spin" />
            <span>Restoring secure cloud sandbox session...</span>
          </div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};
