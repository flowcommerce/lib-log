@Library('lib-jenkins-pipeline@main') _

def cts = []
cts.push(containerTemplate(name: 'play', image: 'flowdocker/play_builder:latest-java17-jammy', resourceRequestMemory: '1Gi', command: 'cat', ttyEnabled: true))

def play29BranchExists() {
    return sh(
            script: "git ls-remote --heads origin play296 | wc -l",
            returnStdout: true
    ).trim() == "1"
}

def branchIsPlay29() {
    return (env.CHANGE_BRANCH ?: env.BRANCH_NAME) == 'play296'
}

def getLatestCleanVersion() {
    return sh(
            script: "git describe --tags --match \"[0-9]*.[0-9]*.[0-9]*\" | sed 's/-.*//'",
            returnStdout: true
    ).trim()
}

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
            when {
                branch 'main'
            }
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

        stage('Release play296 version') {
            when {
                expression { branchIsPlay29() }
            }
            steps {
                container('play') {
                    withCredentials([
                            usernamePassword(
                                    credentialsId: 'jenkins-x-jfrog',
                                    usernameVariable: 'ARTIFACTORY_USERNAME',
                                    passwordVariable: 'ARTIFACTORY_PASSWORD'
                            )
                    ]) {
                        echo "Publishing version ${getLatestCleanVersion()} of play296 library"
                        sh "sbt -Dversion=${getLatestCleanVersion()} clean +publish"
                        syncDependencyLibrary()
                    }
                }
            }
        }

        stage("Update play296 branch") {
            when {
                allOf {
                    branch 'main';
                    expression { play29BranchExists() }
                }
            }
            steps {
                script {
                    echo "Merging main into play296..."
                    sh """
                        git fetch origin main
                        git checkout play296
                        git merge origin/main -X ours --no-edit || echo "No changes to merge"
                
                        if git rev-parse origin/play296 | grep -q \$(git rev-parse HEAD); then
                            echo "No new changes merged, skipping push."
                        else
                            echo "New changes merged, pushing to origin/play296..."
                            git push origin play296
                        fi
                    """
                }
            }
        }
    }
}
