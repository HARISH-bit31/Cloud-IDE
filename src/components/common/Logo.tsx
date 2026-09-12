import React from 'react';

interface LogoProps {
  className?: string;
  size?: number;
  showText?: boolean;
  versionBadge?: string;
}

export const Logo: React.FC<LogoProps> = ({
  className = '',
  size = 28,
  showText = true,
  versionBadge = 'v2.4 Pro',
}) => {
  return (
    <div className={`flex items-center gap-2.5 min-w-max select-none ${className}`}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="flex-shrink-0"
      >
        <defs>
          <linearGradient id="cloudGrad" x1="4" y1="8" x2="44" y2="40" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#833ab4" />
            <stop offset="35%" stopColor="#c13584" />
            <stop offset="70%" stopColor="#fd1d1d" />
            <stop offset="100%" stopColor="#fcb045" />
          </linearGradient>
          <linearGradient id="bracketGrad" x1="14" y1="18" x2="34" y2="30" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#ffffff" />
            <stop offset="100%" stopColor="#e2e8f0" />
          </linearGradient>
        </defs>
        {/* Sleek Cloud Outline */}
        <path
          d="M15.5 35C11.9 35 9 32.1 9 28.5C9 25.2 11.4 22.5 14.6 22.1C15.7 16.9 20.3 13 25.8 13C32.1 13 37.3 17.8 37.9 24C41.3 24.5 44 27.5 44 31C44 34.9 40.9 38 37 38H15.5"
          stroke="url(#cloudGrad)"
          strokeWidth="3.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Inner Code Brackets < / > */}
        <path
          d="M19 25L15 28.5L19 32"
          stroke="url(#bracketGrad)"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M29 25L33 28.5L29 32"
          stroke="url(#bracketGrad)"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path d="M25 24L23 33" stroke="url(#cloudGrad)" strokeWidth="2.2" strokeLinecap="round" />
      </svg>

      {showText && (
        <span className="font-bold text-[18px] tracking-tight bg-gradient-to-r from-[#833ab4] via-[#c13584] to-[#fcb045] bg-clip-text text-transparent">
          Cloud IDE
        </span>
      )}

      {versionBadge && (
        <span className="px-1.5 py-0.5 rounded bg-[#282933] text-[#ffafd2] font-mono text-[10px] font-semibold border border-[#564149]/50">
          {versionBadge}
        </span>
      )}
    </div>
  );
};
