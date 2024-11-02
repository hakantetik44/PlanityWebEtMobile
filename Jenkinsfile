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

        // Project Configuration
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')

        // Report Directories
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_RECORDS = 'target/video-records'

        // Test Output Files
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        TEST_OUTPUT_LOG = 'test_output.log'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'hakan'],
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
        booleanParam(
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo des tests'
        )
    }

    stages {
        stage('Environment Setup') {
            steps {
                script {
                    echo "🚀 Initialisation de l'environnement de test"

                    // Cleanup workspace
                    cleanWs()

                    // Git checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Create directories
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${VIDEO_RECORDS}
                        mkdir -p target/screenshots
                        touch ${CUCUMBER_JSON_PATH}
                    """

                    // Install FFmpeg for video recording if needed
                    if (params.RECORD_VIDEO) {
                        sh '''
                            which ffmpeg || (
                                echo "Installing FFmpeg..." &&
                                apt-get update &&
                                apt-get install -y ffmpeg
                            )
                        '''
                    }
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    try {
                        echo "🧪 Exécution des tests"

                        // Start video recording if enabled
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -f x11grab -video_size 1920x1080 -framerate 25 \
                                -i :0.0 -c:v libx264 -preset ultrafast \
                                ${VIDEO_RECORDS}/test-recording-${BUILD_NUMBER}.mp4 & \
                                echo \$! > video-pid.txt
                            """
                        }

                        // Run tests
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
                        error "❌ Échec de l'exécution des tests: ${e.message}"
                    } finally {
                        // Stop video recording if enabled
                        if (params.RECORD_VIDEO) {
                            sh 'kill $(cat video-pid.txt) || true'
                        }
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Generate Reports') {
            steps {
                script {
                    try {
                        echo "📊 Génération des rapports"

                        // Allure Report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Enhanced Cucumber Report
                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                [key: '👤 Exécuté par', value: currentBuild.getBuildCauses()[0].userId ?: 'System'],
                                [key: '⏱️ Durée', value: currentBuild.durationString],
                                [key: '🌡️ Environnement', value: params.BRANCH_NAME == 'main' ? 'Production' : 'Test'],
                                [key: '🎥 Vidéo', value: params.RECORD_VIDEO ? 'Activé' : 'Désactivé'],
                                [key: '🔧 Framework', value: 'Cucumber + Selenium'],
                                [key: '📝 Langue', value: 'Français'],
                                [key: '🛠️ Version Maven', value: sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()],
                                [key: '☕ Version Java', value: sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()]
                            ]
                        )

                        // Archive results
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                surefire-reports/ \
                                cucumber.json \
                                rapports-tests/ \
                                video-records/
                        """

                        archiveArtifacts(
                            artifacts: """
                                target/test-results-${BUILD_NUMBER}.zip,
                                target/cucumber.json,
                                target/surefire-reports/**/*,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${VIDEO_RECORDS}/*.mp4
                            """,
                            allowEmptyArchive: true
                        )

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur de génération des rapports: ${e.message}"
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
                def statusColor = status == 'SUCCESS' ? '\033[0;32m' : status == 'UNSTABLE' ? '\033[0;33m' : '\033[0;31m'
                def resetColor = '\033[0m'

                echo """╔═══════════════════════════════════════════════╗
║           🌟 Rapport d'Exécution Final          ║
╚═══════════════════════════════════════════════╝

📌 Informations Générales:
▪️ 🔄 Build: #${BUILD_NUMBER}
▪️ 📅 Date: ${new Date().format('dd/MM/yyyy HH:mm')}
▪️ ⏱️ Durée: ${currentBuild.durationString}

🌍 Configuration:
▪️ 🌿 Branch: ${params.BRANCH_NAME}
▪️ 📱 Platform: ${params.PLATFORM_NAME}
▪️ 🌐 Browser: ${params.BROWSER}
▪️ 🎥 Enregistrement Vidéo: ${params.RECORD_VIDEO ? 'Activé' : 'Désactivé'}

🔧 Environnement Technique:
▪️ 🛠️ Maven: ${sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()}
▪️ ☕ Java: ${sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()}

📊 Rapports Disponibles:
▪️ 📈 Allure:    ${BUILD_URL}allure/
▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
▪️ 📑 Excel:     ${BUILD_URL}artifact/${EXCEL_REPORTS}/
▪️ 🎥 Vidéos:    ${BUILD_URL}artifact/${VIDEO_RECORDS}/
▪️ 📦 Artifacts: ${BUILD_URL}artifact/

📈 Métriques:
▪️ Features: ${sh(script: 'find . -name "*.feature" | wc -l', returnStdout: true).trim()}
▪️ Scénarios: ${sh(script: 'grep -r "Scenario:" features/ | wc -l', returnStdout: true).trim()}
▪️ Tests Total: ${sh(script: 'ls -1 target/surefire-reports/*.xml 2>/dev/null | wc -l', returnStdout: true).trim()}

${emoji} Résultat Final: ${statusColor}${status}${resetColor}

🔍 Tags Principaux:
▪️ @regression
▪️ @smoke
▪️ @critical
▪️ @web
▪️ @mobile

📱 Liens Utiles:
▪️ 📚 Wiki: https://wiki.example.com/tests
▪️ 🐞 Jira: https://jira.example.com
▪️ 📊 Dashboard: ${BUILD_URL}
"""

                // Cleanup
                sh """
                    find . -type f -name "*.tmp" -delete || true
                    find . -type d -name "node_modules" -exec rm -rf {} + || true
                """
            }
        }

        success {
            echo '✅ Pipeline terminé avec succès!'
        }

        failure {
            echo '❌ Échec du pipeline!'
        }

        cleanup {
            deleteDir()
        }
    }
}