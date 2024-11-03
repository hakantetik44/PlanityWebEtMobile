pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        // Base Configuration
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'

        // Project Information
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        PROJECT_VERSION = '1.0.0'
        TEAM_NAME = 'Quality Assurance'

        // Directories
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_DIR = 'target/videos'
        SCREENSHOT_DIR = 'target/screenshots'

        // Video Configuration
        SCREEN_RESOLUTION = '1920x1080'
        VIDEO_FRAME_RATE = '30'
        ENABLE_VIDEO = 'true'
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
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    // Git checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Create directories and set up environment
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${VIDEO_DIR}
                        mkdir -p ${SCREENSHOT_DIR}

                        echo "🔧 Configuration de l'environnement..."
                        echo "Platform=${params.PLATFORM_NAME}" > ${ALLURE_RESULTS}/environment.properties
                        echo "Browser=${params.BROWSER}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Branch=${params.BRANCH_NAME}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Environment=Production" >> ${ALLURE_RESULTS}/environment.properties
                    """

                    // Start video recording for MacOS
                    if (env.ENABLE_VIDEO == 'true') {
                        sh """
                            ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                            -video_size ${SCREEN_RESOLUTION} \
                            -vcodec libx264 -pix_fmt yuv420p \
                            "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                            echo \$! > video-pid
                        """
                    }
                }
            }
        }

        stage('Test Execution') {
            steps {
                script {
                    try {
                        echo '🏗️ Exécution des tests...'
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Échec des tests: ${e.message}"
                    } finally {
                        if (env.ENABLE_VIDEO == 'true') {
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
        }

        stage('Reports') {
            steps {
                script {
                    try {
                        echo '📊 Génération des rapports...'

                        // Allure Report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Archive artifacts
                        archiveArtifacts(
                            artifacts: """
                                ${VIDEO_DIR}/**/*.mp4,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${SCREENSHOT_DIR}/**/*.png
                            """,
                            allowEmptyArchive: true
                        )

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur de rapports: ${e.message}"
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

📈 Rapports Disponibles:
▪️ 📊 Allure: ${BUILD_URL}allure/
▪️ 📹 Vidéos: ${BUILD_URL}artifact/${VIDEO_DIR}/
▪️ 📑 Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}/

${emoji} Statut Final: ${status}
"""
            }
            cleanWs()
        }
    }
}