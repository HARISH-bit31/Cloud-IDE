package com.cloudide.cloudide.config;

import com.cloudide.cloudide.entity.File;
import com.cloudide.cloudide.entity.Project;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.repository.FileRepository;
import com.cloudide.cloudide.repository.ProjectRepository;
import com.cloudide.cloudide.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            FileRepository fileRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized with data. Skipping seed.");
            return;
        }

        log.info("Seeding database with default developer user (BCrypt-encoded) and starter projects...");

        // 1. Create Default Developer User with BCrypt hashed password
        User user = User.builder()
                .name("Alex Developer")
                .email("alex.developer@cloud-ide.io")
                .password(passwordEncoder.encode("password123"))
                .build();
        User savedUser = userRepository.save(user);

        // 2. Project 1: Java Practice
        Project javaProject = Project.builder()
                .name("Java Practice")
                .description("Cloud sandbox JDK 21 LTS arithmetic and scanner pipelines")
                .language(ProgrammingLanguage.JAVA)
                .user(savedUser)
                .build();
        Project savedJavaProj = projectRepository.save(javaProject);

        fileRepository.save(File.builder()
                .filename("Main.java")
                .language(ProgrammingLanguage.JAVA)
                .project(savedJavaProj)
                .content("""
                        // Cloud IDE Fast-Runner Sandbox (JDK 21 LTS)
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

                                sc.close();
                            }
                        }
                        """)
                .build());

        fileRepository.save(File.builder()
                .filename("Calculator.java")
                .language(ProgrammingLanguage.JAVA)
                .project(savedJavaProj)
                .content("""
                        public class Calculator {
                            public static int add(int a, int b) {
                                return a + b;
                            }
                            public static int multiply(int a, int b) {
                                return a * b;
                            }
                        }
                        """)
                .build());

        fileRepository.save(File.builder()
                .filename("README.md")
                .language(ProgrammingLanguage.JAVA)
                .project(savedJavaProj)
                .content("""
                        # Java Practice
                        Welcome to Cloud IDE Java Sandbox.
                        """)
                .build());

        // 3. Project 2: Python Calculator
        Project pyProject = Project.builder()
                .name("Python Calculator")
                .description("Interactive mathematical engine and statistical utility scripts")
                .language(ProgrammingLanguage.PYTHON)
                .user(savedUser)
                .build();
        Project savedPyProj = projectRepository.save(pyProject);

        fileRepository.save(File.builder()
                .filename("main.py")
                .language(ProgrammingLanguage.PYTHON)
                .project(savedPyProj)
                .content("""
                        # Cloud IDE Python 3.12 Sandbox
                        import sys

                        def main():
                            print("🚀 Cloud IDE Python Environment v3.12")
                            print("Sum = 42")

                        if __name__ == "__main__":
                            main()
                        """)
                .build());

        // 4. Project 3: C Programs
        Project cProject = Project.builder()
                .name("C Programs")
                .description("Low-level systems programming in C23")
                .language(ProgrammingLanguage.C)
                .user(savedUser)
                .build();
        Project savedCProj = projectRepository.save(cProject);

        fileRepository.save(File.builder()
                .filename("main.c")
                .language(ProgrammingLanguage.C)
                .project(savedCProj)
                .content("""
                        #include <stdio.h>

                        int main(void) {
                            printf("=== Cloud IDE GCC 13.2 Runner ===\\n");
                            printf("Process completed cleanly.\\n");
                            return 0;
                        }
                        """)
                .build());

        log.info("Database seeding completed successfully.");
    }
}
