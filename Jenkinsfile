
import groovy.json.JsonSlurper
import groovy.transform.Field

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
        MAX_RETRY_COUNT = '2'
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
            name: 'ENABLE_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo des tests'
        )
        booleanParam(
            name: 'ENABLE_RETRY',
            defaultValue: true,
            description: 'Réessayer les tests échoués'
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
                    """

                    // Configuration de l'environnement
                    writeFile file: "${ALLURE_RESULTS}/environment.properties", text: """
                        Platform=${params.PLATFORM_NAME}
                        Browser=${params.BROWSER}
                        TestSuite=${params.TEST_SUITE}
                        Environment=${TEST_ENVIRONMENT}
                        Branch=${params.BRANCH_NAME}
                        VideoRecording=${params.ENABLE_VIDEO}
                    """

                    // Installation de ffmpeg pour MacOS si nécessaire

                if (params.ENABLE_VIDEO) {
                    script {
                        def isMac = sh(script: 'uname', returnStdout: true).trim() == 'Darwin'
                        if (isMac) {
                            def hasFfmpeg = sh(script: 'which ffmpeg || true', returnStdout: true).trim()
                            if (!hasFfmpeg) {
                                error "ffmpeg est requis pour l'enregistrement vidéo. Veuillez l'installer manuellement sur votre Mac."
                            }
                        }
                    }
                }

                def getUserId() {
                    return currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)?.userId ?: 'System'
                }


        stage('Test Execution') {
            steps {
                script {
                    try {
                        if (params.ENABLE_VIDEO) {
                            // Démarrage de l'enregistrement vidéo pour MacOS
                            sh '''
                                ffmpeg -f avfoundation -i "1" -framerate 30 \
                                -video_size 1920x1080 \
                                -vcodec libx264 -pix_fmt yuv420p \
                                target/videos/test-execution-${BUILD_NUMBER}.mp4 & \
                                echo $! > video-pid
                            '''
                        }

                        echo '🏃 Exécution des tests...'
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
                        if (params.ENABLE_VIDEO) {
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
                }
            }
        }
        stage('Rapports') {
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

                                // Rapport Cucumber
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
                                        [key: '📹 Vidéo', value: params.ENABLE_VIDEO ? 'Activé' : 'Désactivé']
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
                        def statusColor = status == 'SUCCESS' ? 'good' : status == 'UNSTABLE' ? 'warning' : 'danger'

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
        ▪️ Exécuté par: ${currentBuild.getCause(hudson.model.Cause$UserIdCause)?.userId ?: 'System'}

        🌍 Environnement:
        ▪️ 🌿 Branche: ${params.BRANCH_NAME}
        ▪️ 📱 Plateforme: ${params.PLATFORM_NAME}
        ▪️ 🌐 Navigateur: ${params.BROWSER}
        ▪️ 🎯 Suite: ${params.TEST_SUITE}
        ▪️ 📹 Vidéo: ${params.ENABLE_VIDEO ? 'Activé' : 'Désactivé'}

        📊 Résultats:
        ▪️ Features: \$(find . -name "*.feature" | wc -l)
        ▪️ Scénarios: \$(grep -r "Scenario:" features/ | wc -l || echo "0")
        ▪️ Status: ${status}

        📈 Rapports Disponibles:
        ▪️ 📊 Allure:    ${BUILD_URL}allure/
        ▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
        ▪️ 📹 Vidéos:    ${BUILD_URL}artifact/target/videos/
        ▪️ 📦 Artifacts: ${BUILD_URL}artifact/

        ${emoji} Statut Final: ${status}
        """

                        // Nettoyage
                        cleanWs(
                            cleanWhenSuccess: true,
                            cleanWhenFailure: false,
                            cleanWhenAborted: true
                        )
                    }
                }

                success {
                    echo '✅ Pipeline terminé avec succès!'
                }

                failure {
                    echo '❌ Pipeline terminé en échec!'
                }

                unstable {
                    echo '⚠️ Pipeline terminé avec des avertissements!'
                }
            }
        }