import { ProblemItem } from '../types';

export const INITIAL_PROBLEMS: ProblemItem[] = [
  {
    id: 'prob-1',
    file: 'Main.java',
    line: 14,
    column: 28,
    message: 'Scanner resource leak potential if unclosed before exit block',
    severity: 'warning',
    codeSnippet: 'int sum = Calculator.add(a, b);',
  },
  {
    id: 'prob-2',
    file: 'Calculator.java',
    line: 9,
    column: 5,
    message: 'Method multiply is currently unreferenced in main entry point',
    severity: 'info',
    codeSnippet: 'public static int multiply(int a, int b)',
  },
];
