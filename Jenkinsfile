pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        BACKEND_IMAGE_NAME = 'blog-app-apis'
        GATEWAY_IMAGE_NAME = 'blog-api-gateway'
        IDENTITY_IMAGE_NAME = 'blog-identity-service'
        POST_IMAGE_NAME = 'blog-post-service'
        CONTENT_IMAGE_NAME = 'blog-content-service'
        DOCKER_IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Java And Maven') {
            steps {
                script {
                    runCommand('java -version')
                    runCommand('mvn -version')
                }
            }
        }

        stage('Test Backend') {
            steps {
                script {
                    runCommand('mvn clean test')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Gateway') {
            steps {
                script {
                    runCommand('mvn -f gateway-service/pom.xml clean test')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'gateway-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Identity Service') {
            steps {
                script {
                    runCommand('mvn -f identity-service/pom.xml clean test')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'identity-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Post Service') {
            steps {
                script {
                    runCommand('mvn -f post-service/pom.xml clean test')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'post-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Content Service') {
            steps {
                script {
                    runCommand('mvn -f content-service/pom.xml clean test')
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'content-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Validate Deployment Configuration') {
            steps {
                script {
                    runCommand('docker compose config --quiet')
                    runCommand('kubectl kustomize deploy/k8s')
                }
            }
        }

        stage('Package Applications') {
            steps {
                script {
                    runCommand('mvn package -DskipTests')
                    runCommand('mvn -f gateway-service/pom.xml package -DskipTests')
                    runCommand('mvn -f identity-service/pom.xml package -DskipTests')
                    runCommand('mvn -f post-service/pom.xml package -DskipTests')
                    runCommand('mvn -f content-service/pom.xml package -DskipTests')
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    runCommand('docker --version')
                    runCommand("docker build -t ${BACKEND_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${BACKEND_IMAGE_NAME}:latest .")
                    runCommand("docker build -t ${GATEWAY_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${GATEWAY_IMAGE_NAME}:latest gateway-service")
                    runCommand("docker build -t ${IDENTITY_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${IDENTITY_IMAGE_NAME}:latest identity-service")
                    runCommand("docker build -t ${POST_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${POST_IMAGE_NAME}:latest post-service")
                    runCommand("docker build -t ${CONTENT_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${CONTENT_IMAGE_NAME}:latest content-service")
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar,gateway-service/target/*.jar,identity-service/target/*.jar,post-service/target/*.jar,content-service/target/*.jar', fingerprint: true
        }
        cleanup {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}

void runCommand(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}
