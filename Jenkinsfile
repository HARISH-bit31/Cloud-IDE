pipeline {
    agent any

    environment {
        // Generate unique build tag from Jenkins build number
        IMAGE_TAG = "build-${env.BUILD_NUMBER ?: 'manual'}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                // Check out the configured branch from Git SCM
                checkout scm
            }
        }

        stage('Frontend Build') {
            steps {
                echo '=== Stage 1/5: Installing frontend dependencies and building production assets ==='
                script {
                    if (isUnix()) {
                        sh 'npm ci'
                        sh 'npm run build'
                    } else {
                        bat 'call npm ci'
                        bat 'call npm run build'
                    }
                }
            }
        }

        stage('Backend Tests') {
            steps {
                echo '=== Stage 2/5: Compiling backend and running unit/security tests ==='
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh 'mvn clean test'
                        } else {
                            bat 'call mvn clean test'
                        }
                    }
                }
            }
        }

        stage('Worker Tests') {
            steps {
                echo '=== Stage 3/5: Compiling execution worker and running unit/security tests ==='
                dir('execution-worker') {
                    script {
                        if (isUnix()) {
                            sh 'mvn clean test'
                        } else {
                            bat 'call mvn clean test'
                        }
                    }
                }
            }
        }

        stage('Docker Compose Validation') {
            steps {
                echo '=== Stage 4/5: Validating Docker Compose configuration schema ==='
                script {
                    if (isUnix()) {
                        sh 'docker compose config'
                    } else {
                        bat 'call docker compose config'
                    }
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                echo "=== Stage 5/5: Building production Docker images with tag: ${env.IMAGE_TAG} ==="
                script {
                    def frontendImage = "cloud-ide-frontend:${env.IMAGE_TAG}"
                    def backendImage = "cloud-ide-backend:${env.IMAGE_TAG}"

                    if (isUnix()) {
                        sh "docker build -t ${frontendImage} ."
                        sh "docker build -t ${backendImage} ./backend"
                    } else {
                        bat "call docker build -t ${frontendImage} ."
                        bat "call docker build -t ${backendImage} ./backend"
                    }
                }
            }
        }
    }

    post {
        always {
            // Collect JUnit test reports from both backend and execution-worker projects
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
        }
        success {
            echo "SUCCESS: Cloud IDE CI pipeline completed successfully for build #${env.BUILD_NUMBER} (Image Tag: ${env.IMAGE_TAG})"
        }
        failure {
            echo "FAILURE: Cloud IDE CI pipeline failed during build #${env.BUILD_NUMBER}. Check stage logs for details."
        }
    }
}
