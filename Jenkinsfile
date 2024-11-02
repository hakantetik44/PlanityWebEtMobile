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
        SCREENSHOT_DIR = 'target/screenshots'
        VIDEO_DIR = 'target/videos'

        // Configuration des Tests
        TEST_ENVIRONMENT = 'Production'
        RECORD_VIDEO = 'true'
        VIDEO_FRAME_RATE = '24'
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

    stages {
        stage('Initialisation') {
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

                    // Création des répertoires
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${SCREENSHOT_DIR}
                        mkdir -p ${VIDEO_DIR}

                        echo "🔧 Configuration de l'environnement..."
                        cat << EOF > ${ALLURE_RESULTS}/environment.properties
Platform=${params.PLATFORM_NAME}
Browser=${params.BROWSER}
Branch=${params.BRANCH_NAME}
TestSuite=${params.TEST_SUITE}
Environment=${TEST_ENVIRONMENT}
VideoRecording=${params.RECORD_VIDEO}
DateExecution=${TIMESTAMP}
EOF
                    """
                }
            }
        }
        stage('Build & Test') {
                    steps {
                        script {
                            try {
                                echo '🏗️ Compilation et exécution des tests...'

                                // Démarrage de l'enregistrement vidéo si activé
                                if (params.RECORD_VIDEO) {
                                    sh """
                                        ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                                        -video_size ${SCREEN_RESOLUTION} \
                                        -vcodec libx264 -pix_fmt yuv420p \
                                        "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                        echo \$! > video-pid
                                    """
                                }

                                // Exécution des tests
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

                stage('Rapports') {
                    steps {
                        script {
                            try {
                                echo '📊 Génération des rapports...'

                                // Analyse et formatage des étapes de test
                                def stepsResults = sh(
                                    script: '''
                                        if [ -f "target/cucumber.json" ]; then
                                            jq -r '.[] | .elements[] | .steps[] | "\\(.keyword) \\(.name) - \\(.result.status)"' target/cucumber.json | while read -r line; do
                                                status=$(echo $line | awk -F' - ' '{print $2}')
                                                step=$(echo $line | awk -F' - ' '{print $1}')
                                                case $status in
                                                    "passed")   echo "✅ $step";;
                                                    "failed")   echo "❌ $step";;
                                                    "skipped")  echo "⏭️ $step";;
                                                    "pending")  echo "⏳ $step";;
                                                    *)         echo "ℹ️ $step";;
                                                esac
                                            done
                                        fi
                                    ''',
                                    returnStdout: true
                                ).trim()

                                // Création du rapport HTML des étapes
                                def stepsHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta charset="UTF-8">
                                        <title>Rapport des Étapes de Test</title>
                                        <style>
                                            body {
                                                font-family: 'Arial', sans-serif;
                                                padding: 20px;
                                                background-color: #f5f5f5;
                                            }
                                            .header {
                                                background: #4CAF50;
                                                color: white;
                                                padding: 20px;
                                                border-radius: 5px;
                                                margin-bottom: 20px;
                                            }
                                            .step {
                                                margin: 10px 0;
                                                padding: 15px;
                                                border-radius: 5px;
                                                background: white;
                                                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                                                transition: all 0.3s ease;
                                            }
                                            .step:hover {
                                                transform: translateX(5px);
                                            }
                                            .passed { border-left: 4px solid #4CAF50; }
                                            .failed { border-left: 4px solid #f44336; }
                                            .skipped { border-left: 4px solid #FFC107; }
                                            .pending { border-left: 4px solid #2196F3; }
                                            .timestamp {
                                                font-size: 0.8em;
                                                color: #666;
                                                float: right;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="header">
                                            <h1>🚀 Rapport d'Exécution des Tests</h1>
                                            <p>Date: ${new Date().format('dd/MM/yyyy HH:mm')}</p>
                                        </div>
                                        ${stepsResults.split('\n').collect { step ->
                                            def className =
                                                step.contains('✅') ? 'passed' :
                                                step.contains('❌') ? 'failed' :
                                                step.contains('⏭️') ? 'skipped' :
                                                'pending'
                                            "<div class='step ${className}'>${step}</div>"
                                        }.join('\n')}
                                    </body>
                                    </html>
                                """

                                writeFile file: "${ALLURE_RESULTS}/steps-report.html", text: stepsHtml

                                // Copie des vidéos vers Allure si enregistrement activé
                                if (params.RECORD_VIDEO) {
                                    sh """
                                        if [ -d "${VIDEO_DIR}" ]; then
                                            mkdir -p ${ALLURE_RESULTS}/videos
                                            cp ${VIDEO_DIR}/*.mp4 ${ALLURE_RESULTS}/videos/ || true
                                        fi
                                    """
                                }

                            // Configuration du rapport Allure
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
                                                            [key: '🏢 Projet', value: PROJECT_NAME],
                                                            [key: '📌 Version', value: PROJECT_VERSION],
                                                            [key: '👥 Équipe', value: TEAM_NAME],
                                                            [key: '🌿 Branche', value: params.BRANCH_NAME],
                                                            [key: '📱 Plateforme', value: params.PLATFORM_NAME],
                                                            [key: '🌐 Navigateur', value: params.BROWSER],
                                                            [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                                            [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                                            [key: '⏱️ Durée', value: currentBuild.durationString],
                                                            [key: '🌡️ Environnement', value: TEST_ENVIRONMENT],
                                                            [key: '📝 Langue', value: 'FR'],
                                                            [key: '☕ Version Java', value: sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()],
                                                            [key: '📹 Vidéo', value: params.RECORD_VIDEO ? 'Activé' : 'Désactivé'],
                                                            [key: '📊 Rapport des Étapes', value: "${BUILD_URL}artifact/target/allure-results/steps-report.html"]
                                                        ]
                                                    )

                                                    // Archivage des résultats
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
                                                            ${ALLURE_RESULTS}/steps-report.html,
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
                            ▪️ 🌿 Branche: ${params.BRANCH_NAME}
                            ▪️ 📱 Plateforme: ${params.PLATFORM_NAME}
                            ▪️ 🌐 Navigateur: ${params.BROWSER}
                            ▪️ 🎯 Suite: ${params.TEST_SUITE}
                            ▪️ 🌡️ Env: ${TEST_ENVIRONMENT}
                            ▪️ 📹 Vidéo: ${params.RECORD_VIDEO ? 'Activé' : 'Désactivé'}

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
                            ▪️ 📹 Vidéos:    ${BUILD_URL}artifact/${VIDEO_DIR}/
                            ▪️ 📝 Étapes:    ${BUILD_URL}artifact/target/allure-results/steps-report.html
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

                                            // Nettoyage
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