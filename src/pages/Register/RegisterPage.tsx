import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Logo } from '../../components/common/Logo';
import { Eye, EyeOff, Lock, Mail, User, ArrowRight } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register, login } = useAuth();
  const [name, setName] = useState('Alex Developer');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !password) {
      setError('Please fill in all required fields');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await register(name.trim(), email.trim(), password);
      // Automatically log user in upon successful registration
      await login(email.trim(), password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Registration failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full bg-[#11131c] text-[#e1e1ef] flex flex-col justify-between p-4 md:p-8 font-sans relative overflow-hidden">
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-[#c13584]/20 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-[#7bd0ff]/15 rounded-full blur-[120px] pointer-events-none" />

      {/* Top Header */}
      <header className="flex items-center justify-between max-w-6xl w-full mx-auto z-10">
        <Logo size={32} />
        <div className="flex items-center gap-3 font-mono text-xs text-[#dcbfc9]/70">
          <span className="hidden sm:inline">Already have an account?</span>
          <Link
            to="/login"
            className="px-3 py-1.5 rounded-lg bg-[#1d1f28] hover:bg-[#282933] text-[#7bd0ff] font-semibold border border-[#32343e] transition-colors"
          >
            Sign In
          </Link>
        </div>
      </header>

      {/* Register Form Card */}
      <main className="flex-1 flex items-center justify-center py-8 z-10">
        <div className="w-full max-w-md bg-[#191b24]/90 border border-[#262a3b] rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl space-y-5">
          <div className="space-y-1.5 text-center">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-[#e1e1ef]">
              Create Developer Workspace
            </h1>
            <p className="text-xs text-[#dcbfc9]/70">
              Get instant access to multi-language cloud compilation sandboxes
            </p>
          </div>

          {error && (
            <div className="p-3 rounded-lg bg-[#93000a]/20 border border-[#ffb4ab]/40 text-[#ffb4ab] text-xs font-mono">
              {error}
            </div>
          )}

          <form onSubmit={handleRegister} className="space-y-3.5">
            {/* Full Name */}
            <div className="space-y-1">
              <label className="block text-xs font-semibold text-[#e1e1ef] font-mono">
                Full Name
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-[#a48a93] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Alex Developer"
                  required
                  className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
                />
              </div>
            </div>

            {/* Email Address */}
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
                  placeholder="alex.developer@cloud-ide.io"
                  required
                  className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1">
              <label className="block text-xs font-semibold text-[#e1e1ef] font-mono">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#a48a93] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  required
                  className="w-full pl-9 pr-10 py-2 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
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

            {/* Confirm Password */}
            <div className="space-y-1">
              <label className="block text-xs font-semibold text-[#e1e1ef] font-mono">
                Confirm Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#a48a93] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••••••"
                  required
                  className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#0c0e16] border border-[#262a3b] text-xs text-[#e1e1ef] placeholder:text-[#a48a93]/60 focus:outline-none focus:border-[#7bd0ff] font-mono transition-colors"
                />
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 px-4 mt-2 rounded-lg font-mono text-xs font-bold text-white bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-[0_4px_16px_rgba(193,53,132,0.35)]"
            >
              {isLoading ? (
                <span>Provisioning workspace...</span>
              ) : (
                <>
                  <span>Create Account & Launch</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        </div>
      </main>

      <footer className="text-center font-mono text-[11px] text-[#dcbfc9]/50 py-2 z-10">
        <span>© 2026 Cloud IDE Platform Inc.</span>
      </footer>
    </div>
  );
};
