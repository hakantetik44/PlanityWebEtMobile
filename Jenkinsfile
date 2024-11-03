
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

        // Répertoires
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_DIR = 'target/videos'
        SCREENSHOT_DIR = 'target/screenshots'

        // Configuration des Tests
        TEST_ENVIRONMENT = 'Production'
        VIDEO_FRAME_RATE = '30'
        SCREEN_RESOLUTION = '1920x1080'
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

    options {
        timeout(time: 2, unit: 'HOURS')
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    // Détection de l'OS et configuration de ffmpeg
                    def isMac = sh(script: 'uname', returnStdout: true).trim() == 'Darwin'

                    if (params.RECORD_VIDEO) {
                        if (isMac) {
                            sh '''
                                if ! command -v ffmpeg &> /dev/null; then
                                    echo "❌ ffmpeg n'est pas installé. Installation nécessaire."
                                    if command -v brew &> /dev/null; then
                                        brew install ffmpeg
                                    else
                                        echo "⚠️ Homebrew n'est pas installé. Installation manuelle de ffmpeg requise."
                                        exit 1
                                    fi
                                else
                                    echo "✅ ffmpeg est déjà installé"
                                fi
                            '''
                        }
                    }

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${VIDEO_DIR}
                        mkdir -p ${SCREENSHOT_DIR}

                        echo "🔧 Configuration de l'environnement..."
                        cat << EOF > ${ALLURE_RESULTS}/environment.properties
Platform=${params.PLATFORM_NAME}
Browser=${params.BROWSER}
Branch=${params.BRANCH_NAME}
TestSuite=${params.TEST_SUITE}
Environment=${TEST_ENVIRONMENT}
VideoRecording=${params.RECORD_VIDEO}
EOF
                    """
                }
            }
        }
stage('Build & Test') {
            steps {
                script {
                    try {
                        // Démarrage de l'enregistrement vidéo
                        if (params.RECORD_VIDEO) {
                            def isMac = sh(script: 'uname', returnStdout: true).trim() == 'Darwin'
                            if (isMac) {
                                sh """
                                    ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                                    -video_size ${SCREEN_RESOLUTION} \
                                    -vcodec libx264 -pix_fmt yuv420p \
                                    "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                    echo \$! > video-pid
                                """
                            } else {
                                sh """
                                    ffmpeg -f x11grab -video_size ${SCREEN_RESOLUTION} \
                                    -framerate ${VIDEO_FRAME_RATE} -i :0.0 \
                                    -vcodec libx264 -pix_fmt yuv420p \
                                    "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                    echo \$! > video-pid
                                """
                            }
                        }

                        echo '🏗️ Compilation et exécution des tests...'
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DtestSuite=${params.TEST_SUITE} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Échec de l'exécution des tests: ${e.message}"
                    } finally {
                        // Arrêt de l'enregistrement vidéo
                        if (params.RECORD_VIDEO) {
                            sh '''
                                if [ -f video-pid ]; then
                                    kill $(cat video-pid) || true
                                    rm video-pid
                                    sleep 2
                                fi
                            '''
                        }
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: "${VIDEO_DIR}/**/*.mp4", allowEmptyArchive: true
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        echo '📊 Génération des rapports...'

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
                                [key: '👥 Team', value: TEAM_NAME],
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                [key: '⏱️ Duration', value: currentBuild.durationString],
                                [key: '🌡️ Environment', value: TEST_ENVIRONMENT],
                                [key: '📹 Video', value: params.RECORD_VIDEO ? 'Activé' : 'Désactivé'],
                                [key: '📝 Language', value: 'FR']
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
▪️ 📹 Vidéo: ${params.RECORD_VIDEO ? 'Activé' : 'Désactivé'}

📈 Rapports Disponibles:
▪️ 📊 Allure:    ${BUILD_URL}allure/
▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
▪️ 📹 Vidéos:    ${BUILD_URL}artifact/${VIDEO_DIR}/
▪️ 📦 Artifacts: ${BUILD_URL}artifact/

${emoji} Statut Final: ${status}
"""
                sh '''
                    find . -type f -name "*.tmp" -delete || true
                    find . -type d -name "node_modules" -exec rm -rf {} + || true
                '''
            }
        }

        success {
            echo '✅ Pipeline terminé avec succès!'
        }

        failure {
            echo '❌ Pipeline terminé en échec!'
        }

        cleanup {
            cleanWs()
        }
    }
}