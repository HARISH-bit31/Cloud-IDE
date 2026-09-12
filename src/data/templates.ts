import { SupportedLanguage } from '../types';

export interface ProjectTemplate {
  id: string;
  name: string;
  description: string;
  language: SupportedLanguage;
  icon: string;
  files: { name: string; language: SupportedLanguage | 'markdown' | 'text'; content: string }[];
}

export const PROJECT_TEMPLATES: Record<SupportedLanguage, ProjectTemplate[]> = {
  java: [
    {
      id: 'java-hello-world',
      name: 'Hello World',
      description: 'Standard starter template with basic stdout prints',
      language: 'java',
      icon: 'code',
      files: [
        {
          name: 'Main.java',
          language: 'java',
          content: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Cloud IDE!");
    }
}
`,
        },
        {
          name: 'README.md',
          language: 'markdown',
          content: `# Java Hello World Project\n\nRun with \`Ctrl + Enter\`.`,
        },
      ],
    },
    {
      id: 'java-calculator',
      name: 'Calculator',
      description: 'Arithmetic utility class with modular methods',
      language: 'java',
      icon: 'calculate',
      files: [
        {
          name: 'Main.java',
          language: 'java',
          content: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.hasNextInt() ? sc.nextInt() : 10;
        int b = sc.hasNextInt() ? sc.nextInt() : 32;
        System.out.println("Sum = " + Calculator.add(a, b));
        sc.close();
    }
}
`,
        },
        {
          name: 'Calculator.java',
          language: 'java',
          content: `public class Calculator {
    public static int add(int a, int b) { return a + b; }
    public static int multiply(int a, int b) { return a * b; }
}
`,
        },
      ],
    },
    {
      id: 'java-user-input',
      name: 'User Input',
      description: 'Interactive Scanner reading from standard input',
      language: 'java',
      icon: 'input',
      files: [
        {
          name: 'Main.java',
          language: 'java',
          content: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your name: ");
        if (sc.hasNextLine()) {
            String name = sc.nextLine();
            System.out.println("Welcome to Cloud IDE, " + name + "!");
        } else {
            System.out.println("Welcome, Developer!");
        }
        sc.close();
    }
}
`,
        },
        {
          name: 'input.txt',
          language: 'text',
          content: `Alex Developer`,
        },
      ],
    },
    {
      id: 'java-data-structures',
      name: 'Basic Data Structures',
      description: 'ArrayList, HashMap, and LinkedList implementations',
      language: 'java',
      icon: 'account_tree',
      files: [
        {
          name: 'Main.java',
          language: 'java',
          content: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Java", 98);
        scores.put("Python", 95);
        scores.put("C++", 99);

        scores.forEach((lang, score) -> 
            System.out.printf("Language: %-8s | Score: %d%n", lang, score)
        );
    }
}
`,
        },
      ],
    },
    {
      id: 'java-empty',
      name: 'Empty Project',
      description: 'A clean slate with an empty main class file',
      language: 'java',
      icon: 'note_add',
      files: [
        {
          name: 'Main.java',
          language: 'java',
          content: `public class Main {
    public static void main(String[] args) {
        // Your code here
    }
}
`,
        },
      ],
    },
  ],
  python: [
    {
      id: 'py-hello-world',
      name: 'Hello World',
      description: 'Simple Python 3 starter script',
      language: 'python',
      icon: 'code',
      files: [
        {
          name: 'main.py',
          language: 'python',
          content: `def main():
    print("Hello from Cloud IDE Python!")

if __name__ == "__main__":
    main()
`,
        },
      ],
    },
    {
      id: 'py-calculator',
      name: 'Calculator',
      description: 'Math operations and statistics functions',
      language: 'python',
      icon: 'calculate',
      files: [
        {
          name: 'main.py',
          language: 'python',
          content: `def add(a, b):
    return a + b

def multiply(a, b):
    return a * b

if __name__ == "__main__":
    print(f"10 + 25 = {add(10, 25)}")
    print(f"10 * 25 = {multiply(10, 25)}")
`,
        },
      ],
    },
    {
      id: 'py-user-input',
      name: 'User Input',
      description: 'Read from standard input with fallbacks',
      language: 'python',
      icon: 'input',
      files: [
        {
          name: 'main.py',
          language: 'python',
          content: `import sys

def main():
    input_data = sys.stdin.read().strip()
    print(f"Received stdin payload: '{input_data}'")

if __name__ == "__main__":
    main()
`,
        },
        {
          name: 'input.txt',
          language: 'text',
          content: `Cloud IDE Stdin test`,
        },
      ],
    },
    {
      id: 'py-data-structures',
      name: 'Basic Data Structures',
      description: 'Lists, dicts, tuples, and comprehensions',
      language: 'python',
      icon: 'account_tree',
      files: [
        {
          name: 'main.py',
          language: 'python',
          content: `items = [x**2 for x in range(1, 11)]
print(f"Squares from 1 to 10: {items}")
`,
        },
      ],
    },
    {
      id: 'py-empty',
      name: 'Empty Project',
      description: 'Empty Python file ready for scripting',
      language: 'python',
      icon: 'note_add',
      files: [
        {
          name: 'main.py',
          language: 'python',
          content: `# Your Python code here\n`,
        },
      ],
    },
  ],
  c: [
    {
      id: 'c-hello-world',
      name: 'Hello World',
      description: 'Standard C hello world executable',
      language: 'c',
      icon: 'code',
      files: [
        {
          name: 'main.c',
          language: 'c',
          content: `#include <stdio.h>

int main(void) {
    printf("Hello from C on Cloud IDE!\\n");
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'c-calculator',
      name: 'Calculator',
      description: 'C functions for math operations',
      language: 'c',
      icon: 'calculate',
      files: [
        {
          name: 'main.c',
          language: 'c',
          content: `#include <stdio.h>

int add(int a, int b) { return a + b; }

int main(void) {
    int res = add(15, 35);
    printf("Result: %d\\n", res);
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'c-user-input',
      name: 'User Input',
      description: 'Read inputs via scanf safely',
      language: 'c',
      icon: 'input',
      files: [
        {
          name: 'main.c',
          language: 'c',
          content: `#include <stdio.h>

int main(void) {
    int num;
    printf("Reading input number...\\n");
    if (scanf("%d", &num) == 1) {
        printf("You entered: %d\\n", num);
    } else {
        printf("Default fallback: 100\\n");
    }
    return 0;
}
`,
        },
        {
          name: 'input.txt',
          language: 'text',
          content: `42`,
        },
      ],
    },
    {
      id: 'c-data-structures',
      name: 'Basic Data Structures',
      description: 'Linked list nodes and pointers',
      language: 'c',
      icon: 'account_tree',
      files: [
        {
          name: 'main.c',
          language: 'c',
          content: `#include <stdio.h>
#include <stdlib.h>

typedef struct Node {
    int data;
    struct Node* next;
} Node;

int main(void) {
    Node* head = (Node*)malloc(sizeof(Node));
    head->data = 100;
    head->next = NULL;
    printf("Head Node value: %d\\n", head->data);
    free(head);
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'c-empty',
      name: 'Empty Project',
      description: 'Clean main.c starter',
      language: 'c',
      icon: 'note_add',
      files: [
        {
          name: 'main.c',
          language: 'c',
          content: `#include <stdio.h>

int main(void) {
    // Your code here
    return 0;
}
`,
        },
      ],
    },
  ],
  cpp: [
    {
      id: 'cpp-hello-world',
      name: 'Hello World',
      description: 'C++ iostream standard starter',
      language: 'cpp',
      icon: 'code',
      files: [
        {
          name: 'main.cpp',
          language: 'cpp',
          content: `#include <iostream>

int main() {
    std::cout << "Hello Cloud IDE C++!" << std::endl;
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'cpp-calculator',
      name: 'Calculator',
      description: 'C++ templates and arithmetic class',
      language: 'cpp',
      icon: 'calculate',
      files: [
        {
          name: 'main.cpp',
          language: 'cpp',
          content: `#include <iostream>

template <typename T>
T add(T a, T b) {
    return a + b;
}

int main() {
    std::cout << "add(10, 20) = " << add(10, 20) << "\\n";
    std::cout << "add(3.5, 4.2) = " << add(3.5, 4.2) << "\\n";
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'cpp-user-input',
      name: 'User Input',
      description: 'std::cin stream reading with fast I/O',
      language: 'cpp',
      icon: 'input',
      files: [
        {
          name: 'main.cpp',
          language: 'cpp',
          content: `#include <iostream>
#include <string>

int main() {
    std::string word;
    if (std::cin >> word) {
        std::cout << "Read from cin: " << word << "\\n";
    } else {
        std::cout << "No cin stream detected.\\n";
    }
    return 0;
}
`,
        },
        {
          name: 'input.txt',
          language: 'text',
          content: `SpeedTest`,
        },
      ],
    },
    {
      id: 'cpp-data-structures',
      name: 'Basic Data Structures',
      description: 'STL vectors, maps, and algorithms',
      language: 'cpp',
      icon: 'account_tree',
      files: [
        {
          name: 'main.cpp',
          language: 'cpp',
          content: `#include <iostream>
#include <vector>
#include <numeric>

int main() {
    std::vector<int> nums = {1, 2, 3, 4, 5};
    int sum = std::accumulate(nums.begin(), nums.end(), 0);
    std::cout << "Vector sum: " << sum << "\\n";
    return 0;
}
`,
        },
      ],
    },
    {
      id: 'cpp-empty',
      name: 'Empty Project',
      description: 'Blank C++ template',
      language: 'cpp',
      icon: 'note_add',
      files: [
        {
          name: 'main.cpp',
          language: 'cpp',
          content: `#include <iostream>

int main() {
    // Your code here
    return 0;
}
`,
        },
      ],
    },
  ],
};
