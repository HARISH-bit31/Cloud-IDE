import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Logo } from '../../components/common/Logo';
import { Eye, EyeOff, Lock, Mail, ArrowRight } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, isAuthenticated } = useAuth();
  const [email, setEmail] = useState('alex.developer@cloud-ide.io');
  const [password, setPassword] = useState('password123');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setError('Please enter both email and password');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await login(email.trim(), password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    setError('Google OAuth provider can be configured in enterprise deployment.');
  };

  return (
    <div className="min-h-screen w-full bg-[#11131c] text-[#e1e1ef] flex flex-col justify-between p-4 md:p-8 font-sans relative overflow-hidden">
      {/* Subtle Background Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-[#c13584]/20 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-[#00a6e0]/15 rounded-full blur-[120px] pointer-events-none" />

      {/* Top Header */}
      <header className="flex items-center justify-between max-w-6xl w-full mx-auto z-10">
        <Logo size={32} />
        <div className="flex items-center gap-3 font-mono text-xs text-[#dcbfc9]/70">
          <span className="hidden sm:inline">Don't have an account?</span>
          <Link
            to="/register"
            className="px-3 py-1.5 rounded-lg bg-[#1d1f28] hover:bg-[#282933] text-[#ffafd2] font-semibold border border-[#564149]/40 transition-colors"
          >
            Create Account
          </Link>
        </div>
      </header>

      {/* Center Auth Card */}
      <main className="flex-1 flex items-center justify-center py-10 z-10">
        <div className="w-full max-w-md bg-[#191b24]/90 border border-[#262a3b] rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl space-y-6">
          <div className="space-y-1.5 text-center">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-[#e1e1ef]">
              Welcome to <span className="bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] bg-clip-text text-transparent">Cloud IDE</span>
            </h1>
            <p className="text-xs text-[#dcbfc9]/70">
              High-performance browser execution sandboxes for modern engineers
            </p>
          </div>

          {error && (
            <div className="p-3 rounded-lg bg-[#93000a]/20 border border-[#ffb4ab]/40 text-[#ffb4ab] text-xs font-mono">
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            {/* Email Field */}
            <div className="space-y-1">
              <label className="block text-xs font-semibold text-[#e1e1ef] font-mono">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-[#a48a93] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="developer@cloud-ide.io"
                  required
                  className="w-full pl-9 pr-3 py-2.5 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-semibold text-[#e1e1ef] font-mono">
                  Password
                </label>
                <button
                  type="button"
                  onClick={() => alert('Mock password reset instructions sent.')}
                  className="text-[11px] text-[#ffafd2] hover:underline font-mono"
                >
                  Forgot password?
                </button>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#a48a93] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  required
                  className="w-full pl-9 pr-10 py-2.5 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#a48a93] hover:text-[#e1e1ef] transition-colors"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Remember Me */}
            <div className="flex items-center justify-between text-xs font-mono text-[#dcbfc9]/70">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="rounded bg-[#0c0e16] border-[#262a3b] text-[#c13584] focus:ring-0"
                />
                <span>Remember this workstation</span>
              </label>
            </div>

            {/* Sign In Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 px-4 rounded-lg font-mono text-xs font-bold text-white bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-[0_4px_16px_rgba(193,53,132,0.35)]"
            >
              {isLoading ? (
                <span>Authenticating sandbox...</span>
              ) : (
                <>
                  <span>Sign In to Cloud IDE</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Social login divider */}
          <div className="relative flex items-center justify-center">
            <div className="border-t border-[#262a3b] w-full" />
            <span className="bg-[#191b24] px-3 font-mono text-[10px] text-[#a48a93] uppercase">
              Or authenticate with
            </span>
          </div>

          {/* Google Sign-in Button */}
          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={isLoading}
            className="w-full py-2.5 px-4 rounded-lg bg-[#0c0e16] hover:bg-[#282933] border border-[#262a3b] text-[#e1e1ef] font-mono text-xs font-medium transition-all flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" viewBox="0 0 24 24">
              <path
                fill="#EA4335"
                d="M12 5c1.6 0 3 .6 4.1 1.7l3.1-3.1C17.3 1.8 14.8 1 12 1 7.4 1 3.5 3.6 1.6 7.4l3.7 2.9C6.2 7.4 8.8 5 12 5z"
              />
              <path
                fill="#4285F4"
                d="M23.5 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.5c-.3 1.5-1.1 2.8-2.4 3.7l3.7 2.9c2.2-2 3.7-5 3.7-8.8z"
              />
              <path
                fill="#FBBC05"
                d="M5.3 14.7c-.2-.7-.4-1.5-.4-2.7s.1-2 .4-2.7L1.6 6.4C.6 8.3 0 10.5 0 12.8s.6 4.5 1.6 6.4l3.7-4.5z"
              />
              <path
                fill="#34A853"
                d="M12 23c3.2 0 6-1.1 8-3l-3.7-2.9c-1.1.7-2.5 1.2-4.3 1.2-3.2 0-5.8-2.4-6.7-5.3L1.6 17.5C3.5 21.3 7.4 23 12 23z"
              />
            </svg>
            <span>Continue with Google</span>
          </button>
        </div>
      </main>

      {/* Footer */}
      <footer className="text-center font-mono text-[11px] text-[#dcbfc9]/50 py-2 z-10">
        <span>© 2026 Cloud IDE Platform Inc. High-speed isolated container runner.</span>
      </footer>
    </div>
  );
};
