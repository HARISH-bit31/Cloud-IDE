import { User } from '../types';

const STORAGE_KEY_USER = 'cloud_ide_auth_user';

const DEFAULT_MOCK_USER: User = {
  id: 'usr-dev-777',
  name: 'Alex Developer',
  email: 'alex.developer@cloud-ide.io',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  role: 'Pro Developer',
};

export const authService = {
  getCurrentUser(): User | null {
    try {
      const stored = localStorage.getItem(STORAGE_KEY_USER);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch {
      // Fallback
    }
    return DEFAULT_MOCK_USER; // Default logged in for seamless dev or can be null
  },

  login(email: string, _password?: string): Promise<User> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (!email || !email.trim()) {
          reject(new Error('Please enter a valid email address'));
          return;
        }
        const user: User = {
          id: 'usr-' + Date.now(),
          name: email.split('@')[0] || 'Cloud Developer',
          email: email.trim(),
          avatar: DEFAULT_MOCK_USER.avatar,
          role: 'Pro Developer',
        };
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
        resolve(user);
      }, 300);
    });
  },

  register(name: string, email: string, _password?: string): Promise<User> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (!name.trim() || !email.trim()) {
          reject(new Error('Please fill in all required fields'));
          return;
        }
        const user: User = {
          id: 'usr-' + Date.now(),
          name: name.trim(),
          email: email.trim(),
          avatar: DEFAULT_MOCK_USER.avatar,
          role: 'Pro Developer',
        };
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
        resolve(user);
      }, 300);
    });
  },

  googleLogin(): Promise<User> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const user: User = {
          id: 'usr-google-' + Date.now(),
          name: 'Google Engineer',
          email: 'developer@google.com',
          avatar: DEFAULT_MOCK_USER.avatar,
          role: 'Enterprise Pro',
        };
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
        resolve(user);
      }, 400);
    });
  },

  logout(): void {
    localStorage.removeItem(STORAGE_KEY_USER);
  },
};
