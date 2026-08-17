pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        BACKEND_IMAGE_NAME = 'blog-app-apis'
        GATEWAY_IMAGE_NAME = 'blog-api-gateway'
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

        stage('Package Applications') {
            steps {
                script {
                    runCommand('mvn package -DskipTests')
                    runCommand('mvn -f gateway-service/pom.xml package -DskipTests')
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    runCommand('docker --version')
                    runCommand("docker build -t ${BACKEND_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${BACKEND_IMAGE_NAME}:latest .")
                    runCommand("docker build -t ${GATEWAY_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${GATEWAY_IMAGE_NAME}:latest gateway-service")
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar,gateway-service/target/*.jar', fingerprint: true
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
