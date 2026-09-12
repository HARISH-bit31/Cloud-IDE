import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIDE } from '../../context/IDEContext';
import { useAuth } from '../../auth/AuthContext';
import { projectService } from '../../services/projectService';
import { Project, SupportedLanguage } from '../../types';
import { Logo } from '../../components/common/Logo';
import { CreateProjectModal } from '../../components/modals/CreateProjectModal';
import { SettingsModal } from '../../components/modals/SettingsModal';
import { ToastContainer } from '../../components/common/ToastContainer';
import {
  Plus,
  Play,
  Coffee,
  FileCode2,
  Code,
  Clock,
  Trash2,
  Settings,
  Bell,
  Search,
  ExternalLink,
  Activity,
  Cpu,
  Server,
  Zap,
  CheckCircle2,
  FolderOpen,
  LogOut,
  Loader2,
  RefreshCw,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user: currentUser, logout } = useAuth();
  const {
    setIsCreateProjectModalOpen,
    setIsSettingsModalOpen,
    loadProject,
    addToast,
  } = useIDE();

  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoadingProjects, setIsLoadingProjects] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState<string>('all');

  const fetchProjects = useCallback(async () => {
    setIsLoadingProjects(true);
    try {
      const projs = await projectService.getProjects();
      setProjects(projs);
    } catch (err: any) {
      addToast('Failed to load projects', 'error', err.message || 'Please check backend connection');
    } finally {
      setIsLoadingProjects(false);
    }
  }, [addToast]);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  const handleOpenProject = (projectId: string) => {
    loadProject(projectId);
    navigate(`/ide/${projectId}`);
  };

  const handleDeleteProject = async (projectId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to delete this project sandbox?')) {
      try {
        await projectService.deleteProject(projectId);
        setProjects((prev) => prev.filter((p) => p.id !== projectId));
        addToast('Project deleted', 'info', 'Sandbox removed from database');
      } catch (err: any) {
        addToast('Delete failed', 'error', err.message || 'Could not delete project');
      }
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getLangBadge = (lang: SupportedLanguage) => {
    switch (lang) {
      case 'java':
        return {
          icon: Coffee,
          label: 'Java 21 LTS',
          bg: 'bg-[#ffb95f]/15 text-[#ffb95f] border-[#ffb95f]/30',
        };
      case 'python':
        return {
          icon: FileCode2,
          label: 'Python 3.12',
          bg: 'bg-[#7bd0ff]/15 text-[#7bd0ff] border-[#7bd0ff]/30',
        };
      case 'c':
        return {
          icon: Code,
          label: 'C (GCC 13)',
          bg: 'bg-[#ffafd2]/15 text-[#ffafd2] border-[#ffafd2]/30',
        };
      case 'cpp':
        return {
          icon: Code,
          label: 'C++ (G++ 13)',
          bg: 'bg-[#10b981]/15 text-[#10b981] border-[#10b981]/30',
        };
    }
  };

  const filteredProjects = projects.filter((p) => {
    const matchesSearch =
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.language.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesLang = selectedLanguage === 'all' || p.language === selectedLanguage;
    return matchesSearch && matchesLang;
  });

  return (
    <div className="min-h-screen w-full bg-[#11131c] text-[#e1e1ef] flex flex-col font-sans select-none">
      {/* Header */}
      <header className="sticky top-0 z-40 h-14 bg-[#0c0e16]/95 backdrop-blur-xl border-b border-[#262a3b] px-4 md:px-8 flex items-center justify-between shadow-[0_1px_8px_rgba(0,0,0,0.5)]">
        <div className="flex items-center gap-6">
          <Logo size={28} />
          <nav className="hidden md:flex items-center gap-4 text-xs font-mono text-[#dcbfc9]/70">
            <span className="text-[#ffafd2] font-semibold border-b-2 border-[#ffafd2] py-4">
              Dashboard
            </span>
            <span
              onClick={() => navigate('/ide')}
              className="hover:text-[#e1e1ef] cursor-pointer transition-colors"
            >
              Open IDE
            </span>
            <span
              onClick={() => addToast('Documentation', 'info', 'Cloud IDE SDK & Runner Docs')}
              className="hover:text-[#e1e1ef] cursor-pointer transition-colors"
            >
              Docs
            </span>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => addToast('System Status', 'success', 'All 4 compiler runners are operational')}
            className="p-2 rounded-lg text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28] transition-colors"
            title="System Status"
          >
            <Bell className="w-4 h-4" />
          </button>

          <button
            onClick={() => setIsSettingsModalOpen(true)}
            className="p-2 rounded-lg text-[#dcbfc9]/70 hover:text-[#e1e1ef] hover:bg-[#1d1f28] transition-colors"
            title="Preferences"
          >
            <Settings className="w-4 h-4" />
          </button>

          <div className="flex items-center gap-2 pl-2 border-l border-[#262a3b]">
            <div className="w-7 h-7 rounded-full bg-[#c13584] flex items-center justify-center font-bold text-xs ring-1 ring-[#ffafd2]/40 overflow-hidden">
              {currentUser?.avatar ? (
                <img src={currentUser.avatar} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                'AD'
              )}
            </div>
            <span className="hidden sm:inline text-xs font-medium text-[#e1e1ef]">
              {currentUser?.name || 'Developer'}
            </span>
            <button
              onClick={handleLogout}
              className="p-1.5 text-[#dcbfc9]/50 hover:text-[#ffb4ab] rounded transition-colors ml-1"
              title="Sign Out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-8 space-y-8">
        {/* Welcome Banner */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#191b24] via-[#1d1f28] to-[#14161f] border border-[#262a3b] p-6 sm:p-8 shadow-xl">
          <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-gradient-to-l from-[#c13584]/15 to-transparent pointer-events-none" />

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 relative z-10">
            <div className="space-y-1.5">
              <div className="flex items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-bold text-[#e1e1ef] tracking-tight">
                  Welcome back, <span className="bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] bg-clip-text text-transparent">{currentUser?.name || 'Alex'}</span>
                </h1>
                <span className="px-2 py-0.5 rounded-full bg-[#10b981]/15 text-[#10b981] text-[10px] font-mono border border-[#10b981]/30">
                  Online Sandbox
                </span>
              </div>
              <p className="text-xs sm:text-sm text-[#dcbfc9]/70 max-w-xl">
                Ready to execute code across isolated Java 21, Python 3.12, C23, and C++20 container environments.
              </p>
            </div>

            <button
              onClick={() => setIsCreateProjectModalOpen(true)}
              className="px-5 py-2.5 rounded-xl font-mono text-xs font-bold text-white bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-95 transition-all flex items-center justify-center gap-2 shadow-[0_4px_16px_rgba(193,53,132,0.4)] flex-shrink-0"
            >
              <Plus className="w-4 h-4" />
              <span>New Project</span>
            </button>
          </div>
        </div>

        {/* Statistics Metric Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
          {[
            { label: 'Active Sandboxes', val: `${projects.length} Workspaces`, icon: Server, color: 'text-[#7bd0ff]' },
            { label: 'Total Executions', val: '142 Runs', icon: Zap, color: 'text-[#ffb95f]' },
            { label: 'Cloud CPU Overhead', val: '2.1% Peak', icon: Cpu, color: 'text-[#10b981]' },
            { label: 'Memory Allocation', val: '44.6 MB', icon: Activity, color: 'text-[#ffafd2]' },
          ].map((stat, idx) => {
            const Icon = stat.icon;
            return (
              <div
                key={idx}
                className="p-4 rounded-xl bg-[#191b24] border border-[#262a3b] space-y-1 hover:border-[#32343e] transition-colors"
              >
                <div className="flex items-center justify-between text-[#dcbfc9]/70">
                  <span className="text-xs font-mono">{stat.label}</span>
                  <Icon className={`w-4 h-4 ${stat.color}`} />
                </div>
                <div className="text-lg sm:text-xl font-bold font-mono text-[#e1e1ef]">
                  {stat.val}
                </div>
              </div>
            );
          })}
        </div>

        {/* Projects Section */}
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <FolderOpen className="w-5 h-5 text-[#ffb95f]" />
              <h2 className="text-base font-bold text-[#e1e1ef]">Your Projects & Sandboxes</h2>
              <span className="text-xs font-mono text-[#dcbfc9]/60">({filteredProjects.length})</span>
            </div>

            {/* Filters */}
            <div className="flex items-center gap-2 flex-wrap">
              {/* Search input */}
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#191b24] border border-[#262a3b] text-xs font-mono text-[#e1e1ef] focus-within:border-[#7bd0ff]">
                <Search className="w-3.5 h-3.5 text-[#a48a93]" />
                <input
                  type="text"
                  placeholder="Search projects..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="bg-transparent focus:outline-none w-36 sm:w-44 text-xs"
                />
              </div>

              {/* Language filter buttons */}
              <div className="flex items-center gap-1 bg-[#191b24] p-1 rounded-lg border border-[#262a3b] text-xs font-mono">
                {['all', 'java', 'python', 'c', 'cpp'].map((lang) => (
                  <button
                    key={lang}
                    onClick={() => setSelectedLanguage(lang)}
                    className={`px-2.5 py-1 rounded-md transition-colors uppercase text-[10px] font-semibold ${
                      selectedLanguage === lang
                        ? 'bg-[#282933] text-[#ffafd2]'
                        : 'text-[#dcbfc9]/60 hover:text-[#e1e1ef]'
                    }`}
                  >
                    {lang}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Project Cards Grid */}
          {isLoadingProjects ? (
            <div className="flex flex-col items-center justify-center py-16 bg-[#191b24]/50 border border-[#262a3b] rounded-xl space-y-3 font-mono text-xs text-[#dcbfc9]/70">
              <Loader2 className="w-6 h-6 text-[#7bd0ff] animate-spin" />
              <span>Fetching cloud projects from database...</span>
            </div>
          ) : filteredProjects.length === 0 ? (
            <div className="text-center py-12 bg-[#191b24]/50 border border-[#262a3b] rounded-xl space-y-3 font-mono">
              <p className="text-sm text-[#dcbfc9]/60">No projects found matching your query</p>
              <button
                onClick={() => setIsCreateProjectModalOpen(true)}
                className="px-4 py-2 rounded-lg bg-[#1d1f28] text-[#ffafd2] text-xs border border-[#564149]/50 hover:bg-[#282933]"
              >
                Create your first project
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredProjects.map((project) => {
                const badge = getLangBadge(project.language);
                const BadgeIcon = badge.icon;
                const fileCount = project.files.length;

                return (
                  <div
                    key={project.id}
                    onClick={() => handleOpenProject(project.id)}
                    className="group relative p-5 rounded-xl bg-[#191b24] border border-[#262a3b] hover:border-[#564149] hover:bg-[#1d1f28]/70 transition-all cursor-pointer shadow-lg space-y-4 flex flex-col justify-between"
                  >
                    <div className="space-y-2.5">
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <span
                            className={`flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono border font-semibold ${badge.bg}`}
                          >
                            <BadgeIcon className="w-3 h-3" />
                            {badge.label}
                          </span>
                          <span className="w-1.5 h-1.5 rounded-full bg-[#10b981]" title="Healthy Sandbox" />
                        </div>

                        <button
                          onClick={(e) => handleDeleteProject(project.id, e)}
                          className="opacity-0 group-hover:opacity-100 p-1 rounded text-[#dcbfc9]/50 hover:text-[#ffb4ab] hover:bg-[#282933] transition-all"
                          title="Delete Project"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div>
                        <h3 className="text-sm font-bold text-[#e1e1ef] group-hover:text-[#ffafd2] transition-colors truncate">
                          {project.name}
                        </h3>
                        <p className="text-xs text-[#dcbfc9]/70 line-clamp-2 mt-1">
                          {project.description || `Fast sandbox execution environment for ${project.language.toUpperCase()}`}
                        </p>
                      </div>
                    </div>

                    <div className="pt-3 border-t border-[#262a3b] flex items-center justify-between text-[11px] font-mono text-[#dcbfc9]/60">
                      <div className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        <span>{new Date(project.updatedAt).toLocaleDateString()}</span>
                        <span className="text-[#564149]">•</span>
                        <span>{fileCount} {fileCount === 1 ? 'file' : 'files'}</span>
                      </div>

                      <button
                        onClick={() => handleOpenProject(project.id)}
                        className="flex items-center gap-1 text-[#7bd0ff] font-semibold hover:underline"
                      >
                        <span>Open IDE</span>
                        <ExternalLink className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Recent Activity & Sandbox Telemetry */}
        <div className="rounded-xl bg-[#191b24] border border-[#262a3b] p-5 space-y-3 font-mono text-xs">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-[#e1e1ef] flex items-center gap-2">
              <Activity className="w-4 h-4 text-[#ffafd2]" />
              <span>Recent Sandbox Executions</span>
            </h3>
            <span className="text-[10px] text-[#10b981]">Cluster Status: Operational</span>
          </div>

          <div className="space-y-2 text-[11px]">
            {[
              { time: '10:08:14', proj: 'Java Practice (Main.java)', result: '✓ Exit code 0 (1.18s)', status: 'text-[#10b981]' },
              { time: '09:42:11', proj: 'Python Calculator (main.py)', result: '✓ Finished (0.24s)', status: 'text-[#10b981]' },
              { time: '08:15:30', proj: 'C Programs (main.c)', result: '✓ GCC Compilation (0.42s)', status: 'text-[#10b981]' },
            ].map((act, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-2 rounded bg-[#0c0e16] border border-[#262a3b]/60"
              >
                <div className="flex items-center gap-3">
                  <span className="text-[#a48a93]">{act.time}</span>
                  <span className="text-[#e1e1ef] font-medium">{act.proj}</span>
                </div>
                <span className={`font-semibold ${act.status}`}>{act.result}</span>
              </div>
            ))}
          </div>
        </div>
      </main>

      {/* Modals & Toasts */}
      <CreateProjectModal />
      <SettingsModal />
      <ToastContainer />
    </div>
  );
};
