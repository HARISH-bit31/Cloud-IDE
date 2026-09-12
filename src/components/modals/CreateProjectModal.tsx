import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIDE } from '../../context/IDEContext';
import { Modal } from '../common/Modal';
import { SupportedLanguage } from '../../types';
import { PROJECT_TEMPLATES } from '../../data/templates';
import { Code, Coffee, FileCode2, Sparkles, Check } from 'lucide-react';

export const CreateProjectModal: React.FC = () => {
  const navigate = useNavigate();
  const { isCreateProjectModalOpen, setIsCreateProjectModalOpen, createAndNavigateProject } = useIDE();

  const [name, setName] = useState('');
  const [language, setLanguage] = useState<SupportedLanguage>('java');
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('java-calculator');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const templates = PROJECT_TEMPLATES[language] || [];

  const handleLanguageChange = (lang: SupportedLanguage) => {
    setLanguage(lang);
    const newTemplates = PROJECT_TEMPLATES[lang] || [];
    if (newTemplates.length > 0) {
      setSelectedTemplateId(newTemplates[0].id);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please enter a valid project name');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      const created = await createAndNavigateProject(name.trim(), language, selectedTemplateId);
      setIsCreateProjectModalOpen(false);
      setName('');
      navigate(`/ide/${created.id}`);
    } catch (err: any) {
      setError(err.message || 'Failed to create project');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isCreateProjectModalOpen}
      onClose={() => {
        setIsCreateProjectModalOpen(false);
        setError('');
      }}
      title="Create New Project"
      subtitle="Initialize an isolated cloud execution sandbox with preconfigured runtime"
      maxWidth="max-w-2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Project Name */}
        <div>
          <label className="block text-xs font-semibold text-[#e1e1ef] mb-1.5 font-mono">
            Project Name <span className="text-[#ffafd2]">*</span>
          </label>
          <input
            type="text"
            placeholder="e.g. Binary Search Tree / HTTP Server"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (error) setError('');
            }}
            autoFocus
            className={`w-full px-3 py-2 rounded-lg bg-[#0c0e16] border text-xs text-[#e1e1ef] placeholder:text-[#a48a93] font-mono focus:outline-none transition-colors ${
              error ? 'border-[#ffb4ab]' : 'border-[#262a3b] focus:border-[#7bd0ff]'
            }`}
          />
          {error && <p className="text-[11px] text-[#ffb4ab] mt-1 font-mono">{error}</p>}
        </div>

        {/* Language Selection */}
        <div>
          <label className="block text-xs font-semibold text-[#e1e1ef] mb-1.5 font-mono">
            Execution Runtime
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {[
              { id: 'java', label: 'Java 21 LTS', desc: 'OpenJDK / Temurin', icon: Coffee, color: 'text-[#ffb95f]' },
              { id: 'python', label: 'Python 3.12', desc: 'CPython Fast Runner', icon: FileCode2, color: 'text-[#7bd0ff]' },
              { id: 'c', label: 'C (GCC 13.2)', desc: 'C23 Systems Sandbox', icon: Code, color: 'text-[#ffafd2]' },
              { id: 'cpp', label: 'C++ (G++ 13.2)', desc: 'C++20 Competitive Template', icon: Code, color: 'text-[#10b981]' },
            ].map((item) => {
              const Icon = item.icon;
              const isSelected = language === item.id;

              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => handleLanguageChange(item.id as SupportedLanguage)}
                  className={`flex flex-col items-start p-3 rounded-lg border text-left transition-all ${
                    isSelected
                      ? 'bg-[#1d1f28] border-[#ffafd2] shadow-sm ring-1 ring-[#ffafd2]/30'
                      : 'bg-[#0c0e16] border-[#262a3b] hover:border-[#564149] hover:bg-[#1d1f28]/50'
                  }`}
                >
                  <div className="flex items-center justify-between w-full">
                    <Icon className={`w-4 h-4 ${item.color}`} />
                    {isSelected && <Check className="w-3.5 h-3.5 text-[#ffafd2]" />}
                  </div>
                  <span className="font-mono text-xs font-semibold text-[#e1e1ef] mt-2">
                    {item.label}
                  </span>
                  <span className="text-[10px] text-[#dcbfc9]/60 leading-tight mt-0.5">
                    {item.desc}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Starter Templates */}
        <div>
          <label className="block text-xs font-semibold text-[#e1e1ef] mb-1.5 font-mono">
            Starter Template
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-48 overflow-y-auto pr-1">
            {templates.map((tpl) => {
              const isSelected = selectedTemplateId === tpl.id;

              return (
                <button
                  key={tpl.id}
                  type="button"
                  onClick={() => setSelectedTemplateId(tpl.id)}
                  className={`flex items-start gap-2.5 p-2.5 rounded-lg border text-left transition-all ${
                    isSelected
                      ? 'bg-[#1d1f28] border-[#7bd0ff] ring-1 ring-[#7bd0ff]/30'
                      : 'bg-[#0c0e16] border-[#262a3b] hover:border-[#32343e] hover:bg-[#1d1f28]/40'
                  }`}
                >
                  <Sparkles className="w-4 h-4 text-[#ffb95f] mt-0.5 flex-shrink-0" />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-xs font-semibold text-[#e1e1ef]">
                        {tpl.name}
                      </span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-[#7bd0ff]" />}
                    </div>
                    <p className="text-[11px] text-[#dcbfc9]/70 leading-snug mt-0.5">
                      {tpl.description}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-[#262a3b]">
          <button
            type="button"
            onClick={() => {
              setIsCreateProjectModalOpen(false);
              setError('');
            }}
            className="px-4 py-2 rounded-lg bg-[#0c0e16] hover:bg-[#282933] text-[#dcbfc9] hover:text-[#e1e1ef] font-mono text-xs transition-colors border border-[#262a3b]"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className={`px-5 py-2 rounded-lg text-white font-mono text-xs font-bold bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] hover:brightness-110 active:scale-95 transition-all shadow-[0_2px_12px_rgba(193,53,132,0.4)] ${
              isSubmitting ? 'opacity-60 cursor-not-allowed' : ''
            }`}
          >
            {isSubmitting ? 'Creating in Database...' : 'Create Project'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
