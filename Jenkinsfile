
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

                   // Vérification de l'OS et ffmpeg
                   def isMac = sh(script: 'uname', returnStdout: true).trim() == 'Darwin'

                   if (params.RECORD_VIDEO) {
                       def hasFfmpeg = sh(
                           script: 'command -v ffmpeg || true',
                           returnStdout: true
                       ).trim()

                       if (!hasFfmpeg) {
                           echo "⚠️ ffmpeg n'est pas installé. La capture vidéo sera désactivée."
                           env.ENABLE_VIDEO = 'false'
                       } else {
                           env.ENABLE_VIDEO = 'true'
                           echo "✅ ffmpeg est disponible, la capture vidéo sera activée."
                       }
                   } else {
                       env.ENABLE_VIDEO = 'false'
                   }

                   // Reste du code d'initialisation...
               }
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

stage('Build & Test') {
    steps {
        script {
            try {
                // Démarrage de la capture vidéo uniquement si disponible
                if (env.ENABLE_VIDEO == 'true') {
                    def isMac = sh(script: 'uname', returnStdout: true).trim() == 'Darwin'
                    if (isMac) {
                        sh """
                            ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                            -video_size ${SCREEN_RESOLUTION} \
                            -vcodec libx264 -pix_fmt yuv420p \
                            "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                            echo \$! > video-pid || true
                        """
                    } else {
                        sh """
                            ffmpeg -f x11grab -video_size ${SCREEN_RESOLUTION} \
                            -framerate ${VIDEO_FRAME_RATE} -i :0.0 \
                            -vcodec libx264 -pix_fmt yuv420p \
                            "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                            echo \$! > video-pid || true
                        """
                    }
                }

            } catch (Exception e) {
                currentBuild.result = 'FAILURE'
                throw e
            } finally {

                if (env.ENABLE_VIDEO == 'true') {
                    sh '''
                        if [ -f video-pid ]; then
                            kill $(cat video-pid) 2>/dev/null || true
                            rm -f video-pid
                            sleep 2
                        fi
                    '''
                }
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