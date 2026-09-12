import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { IDEProvider } from './context/IDEContext';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { LoginPage } from './pages/Login/LoginPage';
import { RegisterPage } from './pages/Register/RegisterPage';
import { DashboardPage } from './pages/Dashboard/DashboardPage';
import { IDEPage } from './pages/IDE/IDEPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <IDEProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/ide"
              element={
                <ProtectedRoute>
                  <IDEPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/ide/:projectId"
              element={
                <ProtectedRoute>
                  <IDEPage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </IDEProvider>
    </AuthProvider>
  );
};

export default App;
