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

        // Configuration Vidéo
        RECORD_VIDEO = 'true'
        VIDEO_NAME = "test-recording-${BUILD_NUMBER}.mp4"
        FFMPEG_PATH = '/usr/local/bin/ffmpeg'
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

                       // Création des répertoires
                       sh """
                           mkdir -p ${ALLURE_RESULTS}
                           mkdir -p ${CUCUMBER_REPORTS}
                           mkdir -p ${EXCEL_REPORTS}
                           mkdir -p ${VIDEO_DIR}
                           mkdir -p config
                           chmod -R 777 ${VIDEO_DIR}

                           # Création du fichier de configuration
                           cat << EOF > config/configuration.properties
           platformName=${params.PLATFORM_NAME}
           browser=${params.BROWSER}
           environment=Production
           baseUrl=https://www.planity.com
           EOF

                           echo "🔧 Configuration de l'environnement..."
                           cat << EOF > ${ALLURE_RESULTS}/environment.properties
           Platform=${params.PLATFORM_NAME}
           Browser=${params.BROWSER}
           Branch=${params.BRANCH_NAME}
           Environment=Production
           Video=Enabled
           EOF
                       """

                       // Vérification de ffmpeg
                       sh '''
                           FFMPEG_PATHS=("/usr/local/bin/ffmpeg" "/opt/homebrew/bin/ffmpeg" "/usr/bin/ffmpeg")
                           FFMPEG_FOUND=false

                           for path in "${FFMPEG_PATHS[@]}"; do
                               if [ -x "$path" ]; then
                                   echo "📹 ffmpeg trouvé: $path"
                                   FFMPEG_FOUND=true
                                   ln -sf "$path" ./ffmpeg
                                   break
                               fi
                           done

                           if [ "$FFMPEG_FOUND" = false ]; then
                               echo "⚠️ ffmpeg non trouvé, installation..."
                               brew install ffmpeg || true
                           fi

                           if [ -x "./ffmpeg" ]; then
                               ./ffmpeg -f avfoundation -i "1:none" \
                                   -framerate ${VIDEO_FRAME_RATE} \
                                   -vcodec libx264 \
                                   -preset ultrafast \
                                   -pix_fmt yuv420p \
                                   "${VIDEO_DIR}/${VIDEO_NAME}" & \
                               echo $! > video.pid
                               echo "📹 Enregistrement vidéo démarré"
                           else
                               echo "⚠️ Impossible de démarrer l'enregistrement vidéo"
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
                            // Arrêt de l'enregistrement vidéo
                            if (env.RECORD_VIDEO == 'true') {
                                sh '''#!/bin/bash
                                    PID_FILE="${WORKSPACE}/${VIDEO_DIR}/video.pid"
                                    if [ -f "$PID_FILE" ]; then
                                        PID=$(cat "$PID_FILE")
                                        echo "📹 Arrêt de l'enregistrement: $PID"
                                        kill -2 $PID || true
                                        sleep 3
                                        rm "$PID_FILE"
                                    fi

                                    VIDEO_FILE="${WORKSPACE}/${VIDEO_DIR}/${VIDEO_NAME}"
                                    if [ -f "$VIDEO_FILE" ]; then
                                        echo "📹 Vidéo sauvegardée: $(ls -lh $VIDEO_FILE)"
                                    else
                                        echo "⚠️ Fichier vidéo non trouvé!"
                                        cat "${WORKSPACE}/${VIDEO_DIR}/ffmpeg.log"
                                    fi
                                '''
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

                                    // Rapport Allure
                                    allure([
                                        includeProperties: true,
                                        jdk: '',
                                        properties: [],
                                        reportBuildPolicy: 'ALWAYS',
                                        results: [[path: "${ALLURE_RESULTS}"]]
                                    ])

                                    // Rapport Cucumber amélioré
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
                                            [key: '📹 Video', value: env.RECORD_VIDEO == 'true' ? 'Activé' : 'Désactivé']
                                        ]
                                    )

                                    // Vérification et copie de la vidéo
                                    sh """
                                        if [ -f "${VIDEO_DIR}/${VIDEO_NAME}" ]; then
                                            echo "📹 Vidéo trouvée: \$(ls -lh ${VIDEO_DIR}/${VIDEO_NAME})"
                                            cp ${VIDEO_DIR}/${VIDEO_NAME} ${ALLURE_RESULTS}/
                                        fi
                                    """

                                    // Archivage des artefacts essentiels
                                    archiveArtifacts(
                                        artifacts: """
                                            ${VIDEO_DIR}/**/*.mp4,
                                            ${VIDEO_DIR}/**/*.log,
                                            ${EXCEL_REPORTS}/**/*.xlsx
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