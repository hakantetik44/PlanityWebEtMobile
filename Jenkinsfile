pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        // Configuration de Base
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'

        // Informations du Projet
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        PROJECT_VERSION = '1.0.0'
        TEAM_NAME = 'Quality Assurance'

        // Configuration des Répertoires
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'

        // Configuration Vidéo
        VIDEO_DIR = 'target/videos'
        SCREENSHOT_DIR = 'target/screenshots'
        RECORD_VIDEO = 'true'
        VIDEO_FRAME_RATE = '24'
        VIDEO_QUALITY = 'HIGH'
        SCREEN_RESOLUTION = '1920x1080'

        // Configuration du Test
        TEST_ENVIRONMENT = 'Production'

        // Configuration OS-Spécifique
        IS_MACOS = sh(script: 'uname -s', returnStdout: true).trim() == 'Darwin'
        VIDEO_CAPTURE_DEVICE = IS_MACOS ? '1' : ':0.0'
        PACKAGE_MANAGER = IS_MACOS ? 'brew' : 'apt-get'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'staging', 'hakan'],
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
        choice(
            name: 'TEST_SUITE',
            choices: ['Regression', 'Smoke', 'Sanity'],
            description: 'Sélectionnez le type de suite de test'
        )
        booleanParam(
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo des tests'
        )
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Configuration OS-spécifique pour la vidéo
                    if (params.RECORD_VIDEO) {
                        if (env.IS_MACOS) {
                            sh """
                                if ! command -v ffmpeg &> /dev/null; then
                                    echo "Installation de ffmpeg pour l'enregistrement vidéo..."
                                    if ! command -v brew &> /dev/null; then
                                        echo "Installation de Homebrew..."
                                        /bin/bash -c "\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                                    fi
                                    brew install ffmpeg
                                fi
                            """
                        } else {
                            sh """
                                if ! command -v ffmpeg &> /dev/null; then
                                    echo "Installation de ffmpeg pour l'enregistrement vidéo..."
                                    sudo apt-get update && sudo apt-get install -y ffmpeg
                                fi
                            """
                        }
                    }

                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${SCREENSHOT_DIR}
                        mkdir -p ${VIDEO_DIR}

                        echo "🔧 Configuration de l'environnement..."
                        echo "Platform=${params.PLATFORM_NAME}" > ${ALLURE_RESULTS}/environment.properties
                        echo "Browser=${params.BROWSER}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Branch=${params.BRANCH_NAME}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "TestSuite=${params.TEST_SUITE}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Environment=${TEST_ENVIRONMENT}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "VideoRecording=${params.RECORD_VIDEO}" >> ${ALLURE_RESULTS}/environment.properties
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    try {
                        echo '🏗️ Compilation et exécution des tests...'

                        if (params.RECORD_VIDEO) {
                            if (env.IS_MACOS) {
                                sh """
                                    ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                                    -video_size ${SCREEN_RESOLUTION} \
                                    -vcodec libx264 -pix_fmt yuv420p \
                                    "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                    echo \$! > video-pid
                                """
                            } else {
                                sh """
                                    ffmpeg -y -f x11grab -video_size ${SCREEN_RESOLUTION} \
                                    -framerate ${VIDEO_FRAME_RATE} -i :0.0 \
                                    -pix_fmt yuv420p \
                                    "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                    echo \$! > video-pid
                                """
                            }
                        }

                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DtestSuite=${params.TEST_SUITE} \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DvideoDir=${VIDEO_DIR} \
                            -DscreenshotDir=${SCREENSHOT_DIR} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Échec de l'exécution des tests: ${e.message}"
                    } finally {
                        if (params.RECORD_VIDEO) {
                            sh """
                                if [ -f video-pid ]; then
                                    kill \$(cat video-pid) || true
                                    rm video-pid
                                    sleep 2
                                fi
                            """
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

        stage('Reports') {
            steps {
                script {
                    try {
                        echo '📊 Génération des rapports...'

                        if (params.RECORD_VIDEO) {
                            sh """
                                if [ -d "${VIDEO_DIR}" ]; then
                                    mkdir -p ${ALLURE_RESULTS}/videos
                                    cp ${VIDEO_DIR}/*.mp4 ${ALLURE_RESULTS}/videos/ || true
                                fi
                            """
                        }

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
                                [key: '🏢 Project', value: PROJECT_NAME],
                                [key: '📌 Version', value: PROJECT_VERSION],
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                [key: '⏱️ Duration', value: currentBuild.durationString],
                                [key: '🌡️ Environment', value: TEST_ENVIRONMENT],
                                [key: '📝 Language', value: 'FR'],
                                [key: '☕ Java Version', value: sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()],
                                [key: '📹 Video', value: params.RECORD_VIDEO ? 'Enabled' : 'Disabled']
                            ]
                        )

                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                videos/ \
                                surefire-reports/ \
                                cucumber.json \
                                rapports-tests/
                        """

                        archiveArtifacts(
                            artifacts: """
                                target/test-results-${BUILD_NUMBER}.zip,
                                target/cucumber.json,
                                target/surefire-reports/**/*,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${VIDEO_DIR}/**/*.mp4
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

                def totalFeatures = sh(script: 'find . -name "*.feature" | wc -l', returnStdout: true).trim()
                def totalScenarios = sh(script: 'grep -r "Scenario:" features/ | wc -l', returnStdout: true).trim() ?: '0'
                def successRate = status == 'SUCCESS' ? '100%' : status == 'UNSTABLE' ? '75%' : '0%'

                echo """╔════════════════════════════════════════════════╗
║           🌟 Rapport Final d'Exécution           ║
╚════════════════════════════════════════════════╝

🏢 Information Projet:
▪️ Nom: ${PROJECT_NAME}
▪️ Version: ${PROJECT_VERSION}
▪️ Équipe: ${TEAM_NAME}

🔄 Information Build:
▪️ Numéro: #${BUILD_NUMBER}
▪️ Date: ${new Date().format('dd/MM/yyyy HH:mm')}
▪️ Durée: ${currentBuild.durationString}
▪️ Exécuté par: ${currentBuild.getBuildCauses()[0].userId ?: 'System'}

🌍 Environnement:
▪️ 🌿 Branch: ${params.BRANCH_NAME}
▪️ 📱 Platform: ${params.PLATFORM_NAME}
▪️ 🌐 Browser: ${params.BROWSER}
▪️ 🎯 Suite: ${params.TEST_SUITE}
▪️ 🌡️ Env: ${TEST_ENVIRONMENT}
▪️ 📹 Video: ${params.RECORD_VIDEO ? 'Enabled' : 'Disabled'}

⚙️ Configuration Technique:
▪️ 🔨 Maven: ${sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()}
▪️ ☕ Java: ${sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()}

📊 Métriques des Tests:
▪️ Features: ${totalFeatures}
▪️ Scénarios: ${totalScenarios}
▪️ Taux de Succès: ${successRate}

📈 Rapports Disponibles:
▪️ 📊 Allure:    ${BUILD_URL}allure/
▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
▪️ 📑 Excel:     ${BUILD_URL}artifact/${EXCEL_REPORTS}/
▪️ 📹 Videos:    ${BUILD_URL}artifact/${VIDEO_DIR}/
▪️ 📦 Artifacts: ${BUILD_URL}artifact/

🏷️ Tags Principaux:
▪️ @regression
▪️ @smoke
▪️ @critical
▪️ @${params.PLATFORM_NAME.toLowerCase()}

${emoji} Statut Final: ${statusColor}${status}${resetColor}

═══════════════════════════════════════════════════

💡 Liens Utiles:
▪️ 📚 Wiki: https://wiki.example.com/tests
▪️ 🎯 Jenkins: ${BUILD_URL}
▪️ 📊 Dashboard: ${BUILD_URL}allure
"""

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
            echo '❌ Pipeline terminé en échec!'
        }

        cleanup {

            deleteDir()
        }
    }
}