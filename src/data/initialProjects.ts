import { Project } from '../types';

export const INITIAL_PROJECTS: Project[] = [
  {
    id: 'java-practice-101',
    name: 'Java Practice',
    description: 'Cloud sandbox JDK 21 LTS arithmetic and scanner pipelines',
    language: 'java',
    version: 'JDK 21 LTS (Temurin)',
    createdAt: '2026-09-01T10:00:00.000Z',
    updatedAt: '2026-09-12T04:37:36.000Z',
    status: 'idle',
    files: [
      {
        id: 'file-java-main',
        name: 'Main.java',
        language: 'java',
        content: `// Cloud IDE Fast-Runner Sandbox (JDK 21 LTS)
import java.util.Scanner;

public class Main {
    /** Execution entry point */
    public static void main(String[] args) {
        System.out.println("Hello Cloud IDE!");
        Scanner sc = new Scanner(System.in);

        // Read parameters safely from pipeline stdin
        int a = sc.hasNextInt() ? sc.nextInt() : 10;
        int b = sc.hasNextInt() ? sc.nextInt() : 32;

        System.out.printf("Executing Calculator.add(%d, %d)...%n", a, b);
        int sum = Calculator.add(a, b);
        System.out.println("Sum = " + sum);

        // Fast stream cleanup
        sc.close();
    }
}
`,
      },
      {
        id: 'file-java-calc',
        name: 'Calculator.java',
        language: 'java',
        content: `public class Calculator {
    /** Adds two integers */
    public static int add(int a, int b) {
        return a + b;
    }

    /** Multiplies two integers */
    public static int multiply(int a, int b) {
        return a * b;
    }
}
`,
      },
      {
        id: 'file-java-ds',
        name: 'DataStructures.java',
        language: 'java',
        content: `import java.util.*;

public class DataStructures {
    public static void demo() {
        List<String> list = new ArrayList<>(List.of("Vite", "React", "Monaco", "Stitch"));
        System.out.println("Loaded items: " + list);
    }
}
`,
      },
      {
        id: 'file-java-input',
        name: 'input.txt',
        language: 'text',
        content: `10
32`,
      },
      {
        id: 'file-java-readme',
        name: 'README.md',
        language: 'markdown',
        content: `# Java Practice Project

Welcome to the **Cloud IDE** Java environment!

### Execution Instructions:
1. Hit \`Ctrl + Enter\` or click the **RUN** button in the top right.
2. Provide input via the **Standard Input (stdin)** panel.
3. Observe live compiler output, terminal stdout, and runtime telemetry.
`,
      },
    ],
  },
  {
    id: 'python-calc-202',
    name: 'Python Calculator',
    description: 'Interactive mathematical engine and statistical utility scripts',
    language: 'python',
    version: 'Python 3.12 (CPython)',
    createdAt: '2026-09-05T12:30:00.000Z',
    updatedAt: '2026-09-11T18:20:00.000Z',
    status: 'idle',
    files: [
      {
        id: 'file-py-main',
        name: 'main.py',
        language: 'python',
        content: `# Cloud IDE Python 3.12 Sandbox
import sys

def main():
    print("🚀 Cloud IDE Python Environment v3.12")
    
    # Read two numbers from stdin
    try:
        raw = sys.stdin.read().split()
        a = int(raw[0]) if len(raw) > 0 else 15
        b = int(raw[1]) if len(raw) > 1 else 27
    except Exception:
        a, b = 15, 27
        
    print(f"Calculating sum of {a} and {b}...")
    total = a + b
    print(f"Result: {total}")
    print("✓ Finished successfully.")

if __name__ == "__main__":
    main()
`,
      },
      {
        id: 'file-py-utils',
        name: 'utils.py',
        language: 'python',
        content: `def power(base: float, exponent: int) -> float:
    return base ** exponent

def factorial(n: int) -> int:
    if n <= 1:
        return 1
    return n * factorial(n - 1)
`,
      },
      {
        id: 'file-py-readme',
        name: 'README.md',
        language: 'markdown',
        content: `# Python Calculator
High speed Python 3.12 execution sandbox on Cloud IDE.
`,
      },
    ],
  },
  {
    id: 'c-programs-303',
    name: 'C Programs',
    description: 'Low-level systems programming and memory manipulation in C23',
    language: 'c',
    version: 'GCC 13.2 (x86_64)',
    createdAt: '2026-09-08T09:15:00.000Z',
    updatedAt: '2026-09-10T14:40:00.000Z',
    status: 'idle',
    files: [
      {
        id: 'file-c-main',
        name: 'main.c',
        language: 'c',
        content: `#include <stdio.h>
#include <stdlib.h>

int main(void) {
    printf("=== Cloud IDE GCC 13.2 Runner ===\\n");
    
    int x = 20;
    int y = 22;
    printf("Pointer addition test: %d + %d = %d\\n", x, y, x + y);
    printf("Process completed cleanly.\\n");
    
    return 0;
}
`,
      },
      {
        id: 'file-c-readme',
        name: 'README.md',
        language: 'markdown',
        content: `# C Programs
Optimized Native C execution sandbox with Clang/GCC support.
`,
      },
    ],
  },
  {
    id: 'cpp-algorithms-404',
    name: 'Algorithms',
    description: 'Competitive programming templates and data structure benchmarks in C++20',
    language: 'cpp',
    version: 'G++ 13.2 (C++20)',
    createdAt: '2026-09-09T16:00:00.000Z',
    updatedAt: '2026-09-12T01:10:00.000Z',
    status: 'idle',
    files: [
      {
        id: 'file-cpp-main',
        name: 'main.cpp',
        language: 'cpp',
        content: `#include <iostream>
#include <vector>
#include <algorithm>

int main() {
    std::ios_base::sync_with_stdio(false);
    std::cin.tie(NULL);

    std::cout << "🚀 Cloud IDE C++20 Fast I/O\\n";
    std::vector<int> nums = {42, 13, 89, 7, 64, 21};
    std::sort(nums.begin(), nums.end());

    std::cout << "Sorted array: ";
    for (int n : nums) {
        std::cout << n << " ";
    }
    std::cout << "\\n✓ Binary search available.\\n";

    return 0;
}
`,
      },
    ],
  },
];
