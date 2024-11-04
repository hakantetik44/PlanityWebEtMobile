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
        PROJECT_VERSION = '1.0.0'
        TEAM_NAME = 'Quality Assurance'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_DIR = 'target/videos'

        // Video Configuration
        VIDEO_NAME = "test-recording-${BUILD_NUMBER}.mp4"
        SCREEN_RESOLUTION = '1920x1080'
        VIDEO_FRAME_RATE = '30'
        FFMPEG_CMD = '/usr/local/bin/ffmpeg'
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
                       chmod 777 ${VIDEO_DIR}

                       echo "🔧 Configuration de l'environnement..."
                       echo "Platform=${params.PLATFORM_NAME}" > ${ALLURE_RESULTS}/environment.properties
                       echo "Browser=${params.BROWSER}" >> ${ALLURE_RESULTS}/environment.properties
                       echo "Branch=${params.BRANCH_NAME}" >> ${ALLURE_RESULTS}/environment.properties
                       echo "Environment=Production" >> ${ALLURE_RESULTS}/environment.properties
                   """

                   // Vérification et démarrage de la vidéo
                   sh '''
                       # Check if ffmpeg exists in different locations
                       FFMPEG_PATH="/usr/local/bin/ffmpeg"
                       if [ ! -f "$FFMPEG_PATH" ]; then
                           FFMPEG_PATH="/opt/homebrew/bin/ffmpeg"
                       fi
                       if [ ! -f "$FFMPEG_PATH" ]; then
                           FFMPEG_PATH="/usr/bin/ffmpeg"
                       fi

                       if [ -f "$FFMPEG_PATH" ]; then
                           echo "📹 Using ffmpeg from: $FFMPEG_PATH"
                           $FFMPEG_PATH -f avfoundation -list_devices true -i "" || true
                           $FFMPEG_PATH -f avfoundation -i "1:none" \
                               -r 30 \
                               -s 1920x1080 \
                               -vcodec libx264 \
                               -preset ultrafast \
                               -pix_fmt yuv420p \
                               "${VIDEO_DIR}/test-recording-${BUILD_NUMBER}.mp4" & \
                           echo $! > video.pid
                           echo "📹 Video recording started"
                       else
                           echo "⚠️ ffmpeg not found, video recording disabled"
                       fi
                   '''
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
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } finally {
                        sh '''
                            if [ -f video.pid ]; then
                                PID=$(cat video.pid)
                                kill -INT $PID || true
                                wait $PID 2>/dev/null || true
                                rm video.pid
                                echo "📹 Enregistrement vidéo terminé"
                            fi
                        '''
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

                        // Allure Report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Report
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
                                [key: '📹 Video', value: 'Enabled']
                            ]
                        )

                        // Verification et archivage des artifacts
                        sh """
                            if [ -f "${VIDEO_DIR}/${VIDEO_NAME}" ]; then
                                echo "📹 Vidéo trouvée, taille: \$(ls -lh ${VIDEO_DIR}/${VIDEO_NAME} | awk '{print \$5}')"
                                cp ${VIDEO_DIR}/${VIDEO_NAME} ${ALLURE_RESULTS}/
                            else
                                echo "⚠️ Fichier vidéo non trouvé!"
                            fi
                        """

                        archiveArtifacts(
                            artifacts: """
                                ${VIDEO_DIR}/*.mp4,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${CUCUMBER_REPORTS}/**/*
                            """,
                            allowEmptyArchive: true,
                            fingerprint: true
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

📈 Rapports Disponibles:
▪️ 📊 Allure: ${BUILD_URL}allure/
▪️ 🥒 Cucumber: ${BUILD_URL}cucumber-html-reports/
▪️ 📹 Vidéos: ${BUILD_URL}artifact/${VIDEO_DIR}/
▪️ 📑 Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}/

${emoji} Statut Final: ${status}
"""
            }
            cleanWs()
        }

        success {
            echo '✅ Pipeline terminé avec succès!'
        }

        failure {
            echo '❌ Pipeline terminé en échec!'
        }
    }
}