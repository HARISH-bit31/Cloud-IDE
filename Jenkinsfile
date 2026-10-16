pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'DEPLOY_TO_LOCAL_K8S',
            defaultValue: false,
            description: 'Deploy built images to the local Docker Desktop Kubernetes cluster (cloud-ide namespace) after all tests pass.'
        )
    }

    environment {
        // Generate unique build tag from Jenkins build number
        IMAGE_TAG = "build-${env.BUILD_NUMBER ?: 'manual'}"
        // Ensure kubectl and docker find the correct configuration in Windows service or local agent contexts
        KUBECONFIG = env.KUBECONFIG ?: (isUnix() ? "${env.HOME}/.kube/config" : 'C:\\Users\\Welcome\\.kube\\config')
        DOCKER_CONFIG = env.DOCKER_CONFIG ?: (isUnix() ? "${env.HOME}/.docker" : 'C:\\Users\\Welcome\\.docker')
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
                echo '=== Stage 2/10: Installing frontend dependencies and building production assets ==='
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
                echo '=== Stage 3/10: Compiling backend and running unit/security tests ==='
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
                echo '=== Stage 4/10: Compiling execution worker and running unit/security tests ==='
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
                echo '=== Stage 5/10: Validating Docker Compose configuration schema ==='
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
                echo "=== Stage 6/10: Building production Docker images with tag: ${env.IMAGE_TAG} ==="
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

        stage('Kubernetes Preflight') {
            when {
                expression { params.DEPLOY_TO_LOCAL_K8S == true }
            }
            steps {
                echo '=== Stage 7/10: Verifying Kubernetes cluster readiness, context, and namespace ==='
                script {
                    if (isUnix()) {
                        sh 'kubectl version --client'
                        sh 'kubectl config current-context'
                        sh 'kubectl get namespace cloud-ide'
                        sh 'kubectl get deployment backend frontend -n cloud-ide'
                        sh "docker image inspect cloud-ide-frontend:${env.IMAGE_TAG} > /dev/null"
                        sh "docker image inspect cloud-ide-backend:${env.IMAGE_TAG} > /dev/null"
                    } else {
                        bat 'call kubectl version --client'
                        bat 'call kubectl config current-context'
                        bat 'call kubectl get namespace cloud-ide'
                        bat 'call kubectl get deployment backend frontend -n cloud-ide'
                        bat "call docker image inspect cloud-ide-frontend:${env.IMAGE_TAG} > nul"
                        bat "call docker image inspect cloud-ide-backend:${env.IMAGE_TAG} > nul"
                    }
                }
            }
        }

        stage('Deploy Frontend and Backend') {
            when {
                expression { params.DEPLOY_TO_LOCAL_K8S == true }
            }
            steps {
                echo "=== Stage 8/10: Updating Kubernetes deployments with image tag: ${env.IMAGE_TAG} ==="
                script {
                    def frontendImage = "cloud-ide-frontend:${env.IMAGE_TAG}"
                    def backendImage = "cloud-ide-backend:${env.IMAGE_TAG}"

                    if (isUnix()) {
                        sh "kubectl set image deployment/frontend frontend=${frontendImage} -n cloud-ide"
                        sh "kubectl set image deployment/backend backend=${backendImage} -n cloud-ide"
                    } else {
                        bat "call kubectl set image deployment/frontend frontend=${frontendImage} -n cloud-ide"
                        bat "call kubectl set image deployment/backend backend=${backendImage} -n cloud-ide"
                    }
                }
            }
        }

        stage('Wait for Rollout Completion') {
            when {
                expression { params.DEPLOY_TO_LOCAL_K8S == true }
            }
            steps {
                echo '=== Stage 9/10: Waiting for rolling updates to complete ==='
                script {
                    if (isUnix()) {
                        sh 'kubectl rollout status deployment/backend -n cloud-ide --timeout=180s'
                        sh 'kubectl rollout status deployment/frontend -n cloud-ide --timeout=180s'
                        sh 'kubectl get pods -n cloud-ide -l "app in (frontend, backend)"'
                    } else {
                        bat 'call kubectl rollout status deployment/backend -n cloud-ide --timeout=180s'
                        bat 'call kubectl rollout status deployment/frontend -n cloud-ide --timeout=180s'
                        bat 'call kubectl get pods -n cloud-ide -l "app in (frontend, backend)"'
                    }
                }
            }
        }

        stage('Health and Regression Verification') {
            when {
                expression { params.DEPLOY_TO_LOCAL_K8S == true }
            }
            steps {
                echo '=== Stage 10/10: Running automated health and regression verification ==='
                script {
                    if (isUnix()) {
                        sh 'node scripts/smoke_test.cjs'
                    } else {
                        bat 'call node scripts/smoke_test.cjs'
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
        failure {
            script {
                if (params.DEPLOY_TO_LOCAL_K8S) {
                    echo "[Rollback] Pipeline failed during or after deployment. Initiating automated rollback to previous deployment revision..."
                    if (isUnix()) {
                        sh 'kubectl rollout undo deployment/backend -n cloud-ide || true'
                        sh 'kubectl rollout undo deployment/frontend -n cloud-ide || true'
                        sh 'kubectl rollout status deployment/backend -n cloud-ide --timeout=120s || true'
                        sh 'kubectl rollout status deployment/frontend -n cloud-ide --timeout=120s || true'
                        sh 'kubectl get events -n cloud-ide --sort-by=.metadata.creationTimestamp | tail -n 25 || true'
                    } else {
                        bat 'call kubectl rollout undo deployment/backend -n cloud-ide || echo Backend rollback skipped/failed'
                        bat 'call kubectl rollout undo deployment/frontend -n cloud-ide || echo Frontend rollback skipped/failed'
                        bat 'call kubectl rollout status deployment/backend -n cloud-ide --timeout=120s || echo Backend status skipped/failed'
                        bat 'call kubectl rollout status deployment/frontend -n cloud-ide --timeout=120s || echo Frontend status skipped/failed'
                        bat 'call kubectl get events -n cloud-ide --sort-by=.metadata.creationTimestamp || echo Events capture failed'
                    }
                }
                echo "FAILURE: Cloud IDE CI/CD pipeline failed during build #${env.BUILD_NUMBER}. Check stage logs for details."
            }
        }
        success {
            script {
                if (params.DEPLOY_TO_LOCAL_K8S) {
                    echo "SUCCESS: Cloud IDE built, tested, and successfully deployed to local Kubernetes (Image Tag: ${env.IMAGE_TAG})!"
                } else {
                    echo "SUCCESS: Cloud IDE CI pipeline completed successfully for build #${env.BUILD_NUMBER} (Image Tag: ${env.IMAGE_TAG}) [Deployment was skipped: DEPLOY_TO_LOCAL_K8S=false]"
                }
            }
        }
    }
}
