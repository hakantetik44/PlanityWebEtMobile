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
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORT_PATH = 'target/test-report.xlsx' // Define Excel report path
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
        stage('Initialization') {
            steps {
                script {
                    cleanWs()
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p target/screenshots
                        touch ${CUCUMBER_JSON_PATH}
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    try {
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS}" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "Test execution failed: ${e.message}"
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Reports') {
            steps {
                script {
                    try {
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER]
                            ]
                        )

                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                surefire-reports/ \
                                cucumber.json
                        """

                        archiveArtifacts(
                            artifacts: """
                                target/test-results-${BUILD_NUMBER}.zip,
                                target/cucumber.json,
                                target/surefire-reports/**/*
                            """,
                            allowEmptyArchive: true
                        )
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Report generation error: ${e.message}"
                    }
                }
            }
        }

        // New stage for Excel Report Generation
        stage('Generate Excel Report') {
            steps {
                script {
                    try {
                        // Assuming you have a Java class that creates an Excel report
                        // Example command to run a Java program that generates the report
                        sh """
                            java -cp target/myapp.jar com.example.ExcelReportGenerator ${EXCEL_REPORT_PATH}
                        """
                        archiveArtifacts artifacts: EXCEL_REPORT_PATH, allowEmptyArchive: true
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Excel report generation error: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def emoji = status == 'SUCCESS' ? '✅' : status == 'UNSTABLE' ? '⚠️' : '❌'

                def testResults = sh(script: 'ls -1 target/surefire-reports/*.xml 2>/dev/null | wc -l', returnStdout: true).trim()
                def testCount = testResults.toInteger()

                echo """╔═══════════════════════════════════════════╗
║              🌟 Résumé d'Exécution          ║
╚═══════════════════════════════════════════╝

🔄 Build: #${BUILD_NUMBER}
🌿 Branch: ${params.BRANCH_NAME}
📱 Platform: ${params.PLATFORM_NAME}
🌐 Browser: ${params.BROWSER}
📅 Date: ${new Date().format('dd/MM/yyyy HH:mm')}
🔗 Jenkins URL: ${env.BUILD_URL}

📊 Rapports:
🔹 Allure:    ${BUILD_URL}allure/
🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/
🔹 Artifacts: ${BUILD_URL}artifact/
🔹 Excel Report: ${BUILD_URL}${EXCEL_REPORT_PATH}

📝 Test Results:
- 📁 Nombre de fichiers de test: ${testCount}
- ✅ Résultat final: ${status}

${emoji} Statut Final: ${status}

🎉 Rapport Başlığı: 🌟 Planity Test Report

📈 **Feature Statistics**
Les graphiques suivants montrent les statistiques de passage et d’échec des fonctionnalités.
"""

                sh """
                    find . -type f -name "*.tmp" -delete || true
                    find . -type d -name "node_modules" -exec rm -rf {} + || true
                """
            }
        }

        success {
            echo '✅ Pipeline completed successfully!'
        }

        failure {
            echo '❌ Pipeline failed!'
        }

        cleanup {
            deleteDir()
        }
    }
}
