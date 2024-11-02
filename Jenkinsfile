pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'hakan'],
            description: 'Sélectionnez la branche à tester'
        )
        choice(
            name: 'PLATFORM_NAME',
            choices: ['Web', 'Android', 'iOS'],
            description: 'Sélectionnez la plateforme de test'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'safari'],
            description: 'Sélectionnez le navigateur (pour Web uniquement)'
        )
    }

    stages {
        stage('Branch Selection') {
            steps {
                script {
                    // Mevcut branch'leri getir
                    sh "git fetch --all"
                    def branchOutput = sh(
                        script: 'git branch -r | grep -v HEAD | sed "s/origin\\///"',
                        returnStdout: true
                    ).trim()
                    echo "🌿 Available branches: ${branchOutput}"

                    // Seçilen branch'e geç
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])
                }
            }
        }

        stage('Test Environment Setup') {
            steps {
                script {
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p target/screenshots
                    """
                }
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        sh """
                            ${M2_HOME}/bin/mvn test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DscreenshotsDir=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """

                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("Test execution failed: ${e.message}")
                    }
                }
            }
        }

        stage('Reports') {
            steps {
                script {
                    try {
                        // Allure Report
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Report
                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: '🌟 Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': '🌿 Branch', 'value': params.BRANCH_NAME],
                                ['key': '🚀 Platform', 'value': params.PLATFORM_NAME],
                                ['key': '🌐 Browser', 'value': params.BROWSER]
                            ]

                        // Archive test results
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/
                        """

                        archiveArtifacts artifacts: """
                            target/test-results-${BUILD_NUMBER}.zip,
                            target/cucumber.json
                        """, allowEmptyArchive: true

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur rapports: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def statusEmoji = status == 'SUCCESS' ? '✅' : status == 'UNSTABLE' ? '⚠️' : '❌'

                echo """╔═══════════════════════════════════════════╗
║             Résumé d'Exécution              ║
╚═══════════════════════════════════════════╝

🎯 Build: #${BUILD_NUMBER}
🌿 Branch: ${params.BRANCH_NAME}
🕒 Durée: ${currentBuild.durationString}
📱 Plateforme: ${params.PLATFORM_NAME}
🌐 Navigateur: ${params.BROWSER}

📊 Rapports:
🔹 Allure:    ${BUILD_URL}allure/
🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/

${statusEmoji} Statut Final: ${status}
"""
            }
        }

        success {
            echo '✅ Tests completed successfully!'
        }

        failure {
            echo '❌ Tests failed!'
        }

        cleanup {
            cleanWs()
        }
    }
}