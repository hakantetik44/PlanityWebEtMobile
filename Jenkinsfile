pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        // Core Environment Settings
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'

        // Project Information
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        GIT_REPO_URL = 'https://github.com/hakantetik44/PlanityWebEtMobile.git'

        // Report and Output Directories
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_RECORDS = 'target/video-records'
        SCREENSHOTS_DIR = 'target/screenshots'

        // Output Files
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        TEST_OUTPUT_LOG = 'test_output.log'
        VIDEO_PID_FILE = 'video-pid.txt'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'hakan'],
            description: '🌿 Sélectionnez la branche à tester'
        )
        choice(
            name: 'PLATFORM_NAME',
            choices: ['Web', 'Android', 'iOS'],
            description: '📱 Sélectionnez la plateforme de test'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'safari'],
            description: '🌐 Sélectionnez le navigateur (pour Web uniquement)'
        )
        booleanParam(
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: '🎥 Activer l\'enregistrement vidéo des tests'
        )
        choice(
            name: 'TEST_ENVIRONMENT',
            choices: ['production', 'staging', 'development'],
            description: '🌍 Sélectionnez l\'environnement de test'
        )
    }

    stages {
        stage('Validate Environment') {
            steps {
                script {
                    echo "🔍 Validation de l'environnement..."

                    // Check Java version
                    sh '''
                        echo "Java Version:"
                        java -version
                        echo "\nMaven Version:"
                        mvn -version
                    '''

                    // Check operating system
                    def osType = sh(script: 'uname', returnStdout: true).trim()
                    if (osType != 'Darwin') {
                        error "❌ Ce pipeline est configuré pour MacOS. OS détecté: ${osType}"
                    }
                }
            }
        }

        stage('Environment Setup') {
            steps {
                script {
                    echo "🚀 Initialisation de l'environnement de test"

                    // Cleanup workspace
                    cleanWs()

                    // Git checkout with error handling
                    try {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.BRANCH_NAME}"]],
                            extensions: [[$class: 'CleanBeforeCheckout']],
                            userRemoteConfigs: [[url: env.GIT_REPO_URL]]
                        ])
                    } catch (Exception e) {
                        error "❌ Échec du checkout Git: ${e.message}"
                    }

                    // Create directory structure
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${VIDEO_RECORDS}
                        mkdir -p ${SCREENSHOTS_DIR}
                        touch ${CUCUMBER_JSON_PATH}
                    """

                    // Install dependencies for MacOS
                    if (params.RECORD_VIDEO) {
                        sh '''
                            # Install Homebrew if not present
                            if ! command -v brew &> /dev/null; then
                                echo "🍺 Installation de Homebrew..."
                                /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                            fi

                            # Install FFmpeg if not present
                            if ! command -v ffmpeg &> /dev/null; then
                                echo "🎥 Installation de FFmpeg..."
                                brew install ffmpeg
                            fi

                            echo "✅ Vérification des versions:"
                            ffmpeg -version | head -n 1
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

                        // Start screen recording for MacOS
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -f avfoundation -i "1" -framerate 30 \
                                -video_size 1920x1080 -c:v libx264 -preset ultrafast \
                                ${VIDEO_RECORDS}/test-recording-${BUILD_NUMBER}.mp4 & \
                                echo \$! > ${VIDEO_PID_FILE}
                            """
                            echo "🎥 Enregistrement vidéo démarré"
                        }

                        // Execute Maven tests
                        sh """
                            export MAVEN_OPTS="${MAVEN_OPTS}"
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -Denvironment=${params.TEST_ENVIRONMENT} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS}" \
                            -Dallure.results.directory=${ALLURE_RESULTS} | tee ${TEST_OUTPUT_LOG}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Échec des tests: ${e.message}"
                    } finally {
                        // Stop video recording
                        if (params.RECORD_VIDEO) {
                            sh '''
                                if [ -f video-pid.txt ]; then
                                    kill $(cat video-pid.txt) || true
                                    rm video-pid.txt
                                fi
                            '''
                            echo "🎥 Enregistrement vidéo arrêté"
                        }
                    }
                }
            }
            post {
                always {
                    junit(
                        testResults: '**/target/surefire-reports/*.xml',
                        allowEmptyResults: true,
                        skipMarkingBuildUnstable: false,
                        skipPublishingChecks: false
                    )
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

                        // Cucumber Report with enhanced classifications
                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🌍 Environment', value: params.TEST_ENVIRONMENT],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                [key: '👤 Exécuté par', value: currentBuild.getBuildCauses()[0].userId ?: 'System'],
                                [key: '⏱️ Durée', value: currentBuild.durationString],
                                [key: '🎥 Vidéo', value: params.RECORD_VIDEO ? 'Activé' : 'Désactivé'],
                                [key: '🔧 Framework', value: 'Cucumber + Selenium'],
                                [key: '📝 Langue', value: 'Français'],
                                [key: '⚙️ OS', value: sh(script: 'sw_vers -productName', returnStdout: true).trim()],
                                [key: '🛠️ Maven', value: sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()],
                                [key: '☕ Java', value: sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()]
                            ]
                        )

                        // Archive test artifacts
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                surefire-reports/ \
                                cucumber.json \
                                rapports-tests/ \
                                video-records/ \
                                ${TEST_OUTPUT_LOG}
                        """

                        // Archive artifacts
                        archiveArtifacts(
                            artifacts: """
                                target/test-results-${BUILD_NUMBER}.zip,
                                target/cucumber.json,
                                target/surefire-reports/**/*,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${VIDEO_RECORDS}/*.mp4,
                                ${TEST_OUTPUT_LOG}
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
                   def statusColor = status == 'SUCCESS' ? '\033[0;32m' : status == 'UNSTABLE' ? '\033[0;33m' : '\033[0;31m'
                   def resetColor = '\033[0m'

                   // Collect test statistics
                   def testStats = [
                       features: sh(script: 'find . -name "*.feature" | wc -l', returnStdout: true).trim(),
                       scenarios: sh(script: 'grep -r "Scenario:" features/ 2>/dev/null | wc -l || echo "0"', returnStdout: true).trim(),
                       totalTests: sh(script: 'ls -1 target/surefire-reports/*.xml 2>/dev/null | wc -l || echo "0"', returnStdout: true).trim()
                   ]

                   echo """╔═══════════════════════════════════════════════╗
   ║           🌟 Rapport d'Exécution Final          ║
   ╚═══════════════════════════════════════════════╝

   📌 Informations Générales:
   ▪️ 🔄 Build: #${BUILD_NUMBER}
   ▪️ 📅 Date: ${new Date().format('dd/MM/yyyy HH:mm')}
   ▪️ ⏱️ Durée: ${currentBuild.durationString}

   🌍 Configuration:
   ▪️ 🌿 Branch: ${params.BRANCH_NAME}
   ▪️ 🌍 Environment: ${params.TEST_ENVIRONMENT}
   ▪️ 📱 Platform: ${params.PLATFORM_NAME}
   ▪️ 🌐 Browser: ${params.BROWSER}
   ▪️ 🎥 Enregistrement Vidéo: ${params.RECORD_VIDEO ? 'Activé' : 'Désactivé'}

   🔧 Environnement Technique:
   ▪️ ⚙️ OS: ${sh(script: 'sw_vers -productName', returnStdout: true).trim()}
   ▪️ 🛠️ Maven: ${sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()}
   ▪️ ☕ Java: ${sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()}

   📊 Rapports Disponibles:
   ▪️ 📈 Allure:    ${BUILD_URL}allure/
   ▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
   ▪️ 📑 Excel:     ${BUILD_URL}artifact/${EXCEL_REPORTS}/
   ▪️ 🎥 Vidéos:    ${BUILD_URL}artifact/${VIDEO_RECORDS}/
   ▪️ 📦 Artifacts: ${BUILD_URL}artifact/

   📈 Métriques:
   ▪️ Features: ${testStats.features}
   ▪️ Scénarios: ${testStats.scenarios}
   ▪️ Tests Total: ${testStats.totalTests}
   ▪️ Durée d'Exécution: ${currentBuild.durationString}

   ${emoji} Résultat Final: ${statusColor}${status}${resetColor}

   🔍 Tags Principaux:
   ▪️ @${params.TEST_ENVIRONMENT}
   ▪️ @${params.PLATFORM_NAME.toLowerCase()}
   ▪️ @regression
   ▪️ @smoke
   ▪️ @critical

   📱 Liens Utiles:
   ▪️ 📚 Documentation: https://confluence.planity.com/tests
   ▪️ 🐞 Jira: https://jira.planity.com
   ▪️ 📊 Dashboard: ${BUILD_URL}
   """

                   // Cleanup workspace
                   cleanWs(
                       cleanWhenSuccess: true,
                       cleanWhenUnstable: true,
                       cleanWhenFailure: false,
                       cleanWhenNotBuilt: true,
                       cleanWhenAborted: true,
                       deleteDirs: true,
                       disableDeferredWipeout: true,
                       notFailBuild: true
                   )
               }
           }

           success {
               echo '✅ Pipeline terminé avec succès!'
               script {
                   if (params.RECORD_VIDEO) {
                       echo "🎥 L'enregistrement vidéo est disponible dans les artifacts"
                   }
                   // Send success notification if needed
                   echo "🚀 Tous les tests ont été exécutés avec succès!"
                   // Additional success notifications can be added here
               }
           }

           failure {
               echo '❌ Pipeline échoué!'
               script {
                   // Collect failure information
                   def failureInfo = """
                       🔍 Détails de l'échec:
                       • Build: #${BUILD_NUMBER}
                       • Branch: ${params.BRANCH_NAME}
                       • Platform: ${params.PLATFORM_NAME}
                       • Environment: ${params.TEST_ENVIRONMENT}
                   """
                   echo failureInfo
                   // Additional failure notifications can be added here
               }
           }

           unstable {
               echo '⚠️ Pipeline instable!'
               script {
                   echo """
                       ⚠️ Attention: Build instable
                       • Certains tests ont peut-être échoué
                       • Vérifiez les rapports pour plus de détails
                   """
                   // Additional unstable notifications can be added here
               }
           }

           aborted {
               echo '⛔ Pipeline interrompu!'
               script {
                   echo """
                       ⛔ Build interrompu
                       • L'exécution a été arrêtée manuellement ou en raison d'une erreur système
                       • Durée avant interruption: ${currentBuild.durationString}
                   """
               }
           }

           cleanup {
               script {
                   echo "🧹 Nettoyage post-build..."

                   // Remove temporary files
                   sh """
                       find . -type f -name "*.tmp" -delete || true
                       find . -type d -name "node_modules" -exec rm -rf {} + || true
                       find . -name "*.log" -type f -delete || true
                   """

                   // Archive test outputs if they exist
                   if (fileExists('target')) {
                       echo "📦 Archivage des résultats de test..."
                       archiveArtifacts artifacts: 'target/**/*', allowEmptyArchive: true
                   }
               }
           }
       }
   }