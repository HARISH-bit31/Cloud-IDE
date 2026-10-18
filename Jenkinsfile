pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'DEPLOY_TO_LOCAL_K8S',
            defaultValue: false,
            description: 'Deploy built images to the local Docker Desktop Kubernetes cluster (cloud-ide namespace) after all quality gates pass.'
        )
    }

    triggers {
        // Safe SCM polling for local development without exposing Jenkins to the internet
        pollSCM('H/5 * * * *')
        // GitHub Push Trigger (activated when webhooks are routed to Jenkins)
        githubPush()
    }

    environment {
        // Unique build tag based on Jenkins build number
        IMAGE_TAG = "build-${env.BUILD_NUMBER ?: 'manual'}"
        // Rollback safety tracking flags
        DEPLOYMENT_ATTEMPTED = 'false'
        BACKEND_DEPLOYED = 'false'
        FRONTEND_DEPLOYED = 'false'
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
                script {
                    // Resolve OS-dependent configuration paths dynamically at runtime
                    if (!env.KUBECONFIG) {
                        env.KUBECONFIG = isUnix() ? "${env.HOME}/.kube/config" : 'C:\\Users\\Welcome\\.kube\\config'
                    }
                    if (!env.DOCKER_CONFIG) {
                        env.DOCKER_CONFIG = isUnix() ? "${env.HOME}/.docker" : 'C:\\Users\\Welcome\\.docker'
                    }
                }
                echo '=== Stage 1/14: Checking out source code and configuring environment ==='
                // Check out the configured branch from Git SCM
                checkout scm
            }
        }

        stage('Secret Scanning Gate') {
            steps {
                echo '=== Stage 2/14: Scanning repository for hardcoded secrets, keys, and tokens ==='
                script {
                    if (isUnix()) {
                        sh 'node scripts/scan_secrets.cjs'
                    } else {
                        bat 'call node scripts/scan_secrets.cjs'
                    }
                }
            }
        }

        stage('Frontend Quality & Lint') {
            steps {
                echo '=== Stage 3/14: Running frontend static analysis and TypeScript verification ==='
                script {
                    if (isUnix()) {
                        sh 'npm ci'
                        sh 'npm run lint'
                    } else {
                        bat 'call npm ci'
                        bat 'call npm run lint'
                    }
                }
            }
        }

        stage('Frontend Build') {
            steps {
                echo '=== Stage 4/14: Compiling production Vite assets ==='
                script {
                    if (isUnix()) {
                        sh 'npm run build'
                    } else {
                        bat 'call npm run build'
                    }
                }
            }
        }

        stage('Dependency Security Audit') {
            steps {
                echo '=== Stage 5/14: Auditing npm and Maven dependencies for known vulnerabilities ==='
                script {
                    if (isUnix()) {
                        sh 'node scripts/audit_dependencies.cjs'
                    } else {
                        bat 'call node scripts/audit_dependencies.cjs'
                    }
                }
            }
        }

        stage('Backend Tests') {
            steps {
                echo '=== Stage 6/14: Compiling backend and running unit/security tests ==='
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
                echo '=== Stage 7/14: Compiling execution worker and running unit/security tests ==='
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
                echo '=== Stage 8/14: Validating Docker Compose configuration schema ==='
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
                echo "=== Stage 9/14: Building production Docker images with tag: ${env.IMAGE_TAG} ==="
                script {
                    if (!env.DOCKER_CONFIG) {
                        env.DOCKER_CONFIG = isUnix() ? "${env.HOME}/.docker" : 'C:\\Users\\Welcome\\.docker'
                    }
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

        stage('Docker Security Verification') {
            steps {
                echo '=== Stage 10/14: Verifying container runtime security and isolation flags ==='
                script {
                    if (isUnix()) {
                        sh "node scripts/verify_docker_security.cjs ${env.IMAGE_TAG}"
                    } else {
                        bat "call node scripts/verify_docker_security.cjs ${env.IMAGE_TAG}"
                    }
                }
            }
        }

        stage('Kubernetes Preflight') {
            when {
                expression {
                    return params.DEPLOY_TO_LOCAL_K8S == true && (env.CHANGE_ID == null)
                }
            }
            steps {
                echo '=== Stage 11/14: Verifying Kubernetes cluster readiness, context, and namespace ==='
                script {
                    if (!env.KUBECONFIG) {
                        env.KUBECONFIG = isUnix() ? "${env.HOME}/.kube/config" : 'C:\\Users\\Welcome\\.kube\\config'
                    }
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
                expression {
                    return params.DEPLOY_TO_LOCAL_K8S == true && (env.CHANGE_ID == null)
                }
            }
            steps {
                echo "=== Stage 12/14: Updating Kubernetes deployments with image tag: ${env.IMAGE_TAG} ==="
                script {
                    def frontendImage = "cloud-ide-frontend:${env.IMAGE_TAG}"
                    def backendImage = "cloud-ide-backend:${env.IMAGE_TAG}"

                    echo "[Deploy] Updating backend deployment image to: ${backendImage}"
                    if (isUnix()) {
                        sh "kubectl set image deployment/backend backend=${backendImage} -n cloud-ide"
                    } else {
                        bat "call kubectl set image deployment/backend backend=${backendImage} -n cloud-ide"
                    }
                    env.BACKEND_DEPLOYED = 'true'
                    env.DEPLOYMENT_ATTEMPTED = 'true'

                    echo "[Deploy] Updating frontend deployment image to: ${frontendImage}"
                    if (isUnix()) {
                        sh "kubectl set image deployment/frontend frontend=${frontendImage} -n cloud-ide"
                    } else {
                        bat "call kubectl set image deployment/frontend frontend=${frontendImage} -n cloud-ide"
                    }
                    env.FRONTEND_DEPLOYED = 'true'
                }
            }
        }

        stage('Wait for Rollout Completion') {
            when {
                expression {
                    return params.DEPLOY_TO_LOCAL_K8S == true && (env.CHANGE_ID == null)
                }
            }
            steps {
                echo '=== Stage 13/14: Waiting for rolling updates to complete ==='
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
                expression {
                    return params.DEPLOY_TO_LOCAL_K8S == true && (env.CHANGE_ID == null)
                }
            }
            steps {
                echo '=== Stage 14/14: Running automated health and regression verification ==='
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
                // Rollback safety: only execute rollback if a deployment was actually attempted and modified in this build.
                // Never run rollback if earlier test, lint, security, or build stages failed.
                if (env.DEPLOYMENT_ATTEMPTED == 'true') {
                    if (!env.KUBECONFIG) {
                        env.KUBECONFIG = isUnix() ? "${env.HOME}/.kube/config" : 'C:\\Users\\Welcome\\.kube\\config'
                    }
                    echo "[Rollback] Pipeline failed after deployment was initiated. Rolling back only modified deployments..."
                    if (env.BACKEND_DEPLOYED == 'true') {
                        echo "[Rollback] Reverting backend deployment to previous revision..."
                        if (isUnix()) {
                            sh 'kubectl rollout undo deployment/backend -n cloud-ide || true'
                            sh 'kubectl rollout status deployment/backend -n cloud-ide --timeout=120s || true'
                        } else {
                            bat 'call kubectl rollout undo deployment/backend -n cloud-ide || echo Backend rollback skipped/failed'
                            bat 'call kubectl rollout status deployment/backend -n cloud-ide --timeout=120s || echo Backend status skipped/failed'
                        }
                    }
                    if (env.FRONTEND_DEPLOYED == 'true') {
                        echo "[Rollback] Reverting frontend deployment to previous revision..."
                        if (isUnix()) {
                            sh 'kubectl rollout undo deployment/frontend -n cloud-ide || true'
                            sh 'kubectl rollout status deployment/frontend -n cloud-ide --timeout=120s || true'
                        } else {
                            bat 'call kubectl rollout undo deployment/frontend -n cloud-ide || echo Frontend rollback skipped/failed'
                            bat 'call kubectl rollout status deployment/frontend -n cloud-ide --timeout=120s || echo Frontend status skipped/failed'
                        }
                    }
                    if (isUnix()) {
                        sh 'kubectl get events -n cloud-ide --sort-by=.metadata.creationTimestamp | tail -n 25 || true'
                    } else {
                        bat 'call kubectl get events -n cloud-ide --sort-by=.metadata.creationTimestamp || echo Events capture failed'
                    }
                } else {
                    echo "[Rollback Guard] Pipeline failed prior to deployment modification. No Kubernetes workloads were changed; skipping rollback."
                }
                echo "FAILURE: Cloud IDE CI/CD pipeline failed during build #${env.BUILD_NUMBER}. Check stage logs for details."
            }
        }
        success {
            script {
                if (params.DEPLOY_TO_LOCAL_K8S) {
                    echo "SUCCESS: Cloud IDE passed all quality & security gates and successfully deployed to local Kubernetes (Image Tag: ${env.IMAGE_TAG})!"
                } else {
                    echo "SUCCESS: Cloud IDE CI pipeline passed all quality & security gates for build #${env.BUILD_NUMBER} (Image Tag: ${env.IMAGE_TAG}) [Deployment was skipped: DEPLOY_TO_LOCAL_K8S=false]"
                }
            }
        }
    }
}
