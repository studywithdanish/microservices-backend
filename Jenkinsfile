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
        DOCKER_IMAGE_NAME = 'blog-app-apis'
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

        stage('Test') {
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

        stage('Package') {
            steps {
                script {
                    runCommand('mvn package -DskipTests')
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    runCommand('docker --version')
                    runCommand("docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} -t ${DOCKER_IMAGE_NAME}:latest .")
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
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
