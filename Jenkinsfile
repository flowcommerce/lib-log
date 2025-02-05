
def cts = []
if (config.databasePackage) {
    cts.push(containerTemplate(name: 'postgres', image: "${config.databasePackage}:latest", alwaysPullImage: true, resourceRequestMemory: '1Gi'))
}
if (config.localstackServices) {
    cts.push(containerTemplate(name: 'localstack', image: 'localstack/localstack:0.12.12', alwaysPullImage: true, resourceRequestMemory: '1Gi', envVars: [envVar(key: 'SERVICES', value: config.localstackServices)]))
}
cts.push(containerTemplate(name: 'play', image: 'flowdocker/play_builder:latest-java17-jammy', resourceRequestMemory: '1Gi', command: 'cat', ttyEnabled: true))

def play29BranchExists = sh(script: "git ls-remote --heads origin play29 | wc -l", returnStdout: true).trim() == "1"

pipeline {
    agent {
        kubernetes {
            inheritFrom 'default'
            containerTemplates(cts)
        }
    }

    options {
        disableConcurrentBuilds()
    }

    environment {
        AWS_ACCESS_KEY = 'x'
        AWS_SECRET_KEY = 'x'
    }

    stages {
        stage('Checkout') {
            steps {
                checkoutWithTags scm
            }
        }

        stage('SBT Test') {
            steps {
                container('play') {
                    script {
                        try {
                            if (config.databasePackage) {
                                sh '''
                echo "$(date) - waiting for database to start"
                until pg_isready -h localhost
                do
                  sleep 10
                done
              '''
                            }
                            sh 'sbt clean scalafmtSbtCheck scalafmtCheckAll flowLintLib coverage test doc'
                            sh 'sbt coverageAggregate'
                        } finally {
                            postSbtReport()
                        }
                    }
                }
            }
        }

        stage('Tag new version') {
            when { branch 'main' }
            steps {
                script {
                    VERSION = new flowSemver().calculateSemver()
                    new flowSemver().commitSemver(VERSION)
                }
            }
        }

        stage('Release') {
            when { branch 'main' }
            steps {
                container('play') {
                    withCredentials([
                            usernamePassword(
                                    credentialsId: 'jenkins-x-jfrog',
                                    usernameVariable: 'ARTIFACTORY_USERNAME',
                                    passwordVariable: 'ARTIFACTORY_PASSWORD'
                            )
                    ]) {
                        sh 'sbt clean +publish'
                        syncDependencyLibrary()
                    }
                }
            }
        }

        stage('Release') {
            when { branch 'play296' }
            steps {
                container('play') {
                    withCredentials([
                            usernamePassword(
                                    credentialsId: 'jenkins-x-jfrog',
                                    usernameVariable: 'ARTIFACTORY_USERNAME',
                                    passwordVariable: 'ARTIFACTORY_PASSWORD'
                            )
                    ]) {
                        sh 'sbt clean +publish'
                        syncDependencyLibrary()
                    }
                }
            }
        }
    }
}
