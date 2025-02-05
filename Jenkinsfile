@Library('lib-jenkins-pipeline@main') _

def cts = []
cts.push(containerTemplate(name: 'play', image: 'flowdocker/play_builder:latest-java17-jammy', resourceRequestMemory: '1Gi', command: 'cat', ttyEnabled: true))

def play29BranchExists() {
    return sh(
            script: "git ls-remote --heads origin play296 | wc -l",
            returnStdout: true
    ).trim() == "1"
}

def buildingOnPlay296Branch() {
    return (env.CHANGE_BRANCH ?: env.BRANCH_NAME) == 'play296'
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
                script {
                    if (buildingOnPlay296Branch()) {
                        echo "Merging main into ${env.BRANCH_NAME}..."
                        sh """
                            git fetch origin main
                            git status
                            git merge origin/main --no-edit || echo "No changes to merge"
    
                            if git rev-parse origin/"\${env.BRANCH_NAME}" | grep -q \$(git rev-parse HEAD); then
                                echo "No new changes merged, skipping push."
                            else
                                echo "New changes merged, pushing to origin/\${env.BRANCH_NAME}..."
                                git push origin "\${env.BRANCH_NAME}"
                            fi
                        """
                    }
                }
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
                anyOf {
                    branch 'main';
                    expression { (env.CHANGE_BRANCH ?: env.BRANCH_NAME) == 'play296' }
                }
            }
            steps {
                script {
                    VERSION = new flowSemver().calculateSemver()
                    new flowSemver().commitSemver(VERSION)
                }
            }
        }

        stage('Release') {
            when {
                anyOf {
                    branch 'main';
                    expression { (env.CHANGE_BRANCH ?: env.BRANCH_NAME) == 'play296' }
                }
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
                        sh 'sbt clean +publish'
                        syncDependencyLibrary()
                    }
                }
            }
        }
    }
}
