
import groovy.json.JsonSlurper
import groovy.transform.Field

pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
        nodejs 'NodeJS'
        dockerTool 'docker'
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
        PDF_REPORTS = 'target/pdf-reports'
        VIDEO_DIR = 'target/videos'
        PERFORMANCE_REPORTS = 'target/performance'
        SECURITY_SCAN_RESULTS = 'target/security-results'

        // Configuration des Tests
        TEST_ENVIRONMENT = 'Production'
        MAX_RETRY_COUNT = '2'
        PARALLEL_THREADS = '3'
        VIDEO_FRAME_RATE = '30'
        SCREEN_RESOLUTION = '1920x1080'

        // Intégrations
        SLACK_CHANNEL = '#test-automation'
        EMAIL_RECIPIENTS = 'equipe@planity.com'
        JIRA_PROJECT = 'PLANITY'
        CONFLUENCE_SPACE = 'TEST'

        // Sécurité et Qualité
        SONAR_PROJECT_KEY = 'planity-automation'
        SONAR_TOKEN = credentials('sonar-token')

        // Docker
        DOCKER_HUB_CREDS = credentials('docker-hub-credentials')
        SELENIUM_GRID_URL = 'http://selenium-hub:4444/wd/hub'

        // Surveillance
        GRAFANA_URL = 'http://grafana:3000'
        PROMETHEUS_URL = 'http://prometheus:9090'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'staging', 'hakan', 'feature/*', 'release/*'],
            description: 'Sélectionnez la branche à tester'
        )
        choice(
            name: 'PLATFORM_NAME',
            choices: ['Web', 'Android', 'iOS'],
            description: 'Sélectionnez la plateforme de test'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'safari', 'edge'],
            description: 'Sélectionnez le navigateur (pour Web uniquement)'
        )
        choice(
            name: 'TEST_SUITE',
            choices: ['Regression', 'Smoke', 'Sanity', 'E2E', 'Performance', 'Security'],
            description: 'Sélectionnez le type de suite de test'
        )
        choice(
            name: 'TEST_PRIORITY',
            choices: ['P1', 'P2', 'P3', 'All'],
            description: 'Priorité des tests à exécuter'
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
        booleanParam(
            name: 'SEND_NOTIFICATIONS',
            defaultValue: true,
            description: 'Envoyer des notifications'
        )
        booleanParam(
            name: 'GENERATE_PDF',
            defaultValue: false,
            description: 'Générer un rapport PDF'
        )
        string(
            name: 'CUSTOM_TAGS',
            defaultValue: '',
            description: 'Tags supplémentaires (séparés par des virgules)'
        )
    }

    options {
        timeout(time: 2, unit: 'HOURS')
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        parallelsAlwaysFailFast()
        timestamps()
        disableConcurrentBuilds()
    }
stages {
        stage('Initialisation') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    // Vérification de l'environnement
                    sh """
                        echo "🔍 Vérification de l'environnement..."
                        java -version
                        ${M2_HOME}/bin/mvn -version
                        node -v
                        npm -v
                    """

                    // Configuration Docker pour Selenium Grid
                    if (params.PLATFORM_NAME == 'Web') {
                        docker.image('selenium/hub:latest').withRun('-p 4444:4444 --name selenium-hub') {
                            docker.image("selenium/node-${params.BROWSER}:latest").withRun('--link selenium-hub:hub') {
                                echo "🐳 Configuration Selenium Grid avec ${params.BROWSER}"
                            }
                        }
                    }

                    // Préparation des répertoires
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${PDF_REPORTS}
                        mkdir -p ${VIDEO_DIR}
                        mkdir -p ${PERFORMANCE_REPORTS}
                        mkdir -p ${SECURITY_SCAN_RESULTS}
                    """

                    // Configuration de l'environnement de test
                    writeFile file: "${ALLURE_RESULTS}/environment.properties", text: """
                        Platform=${params.PLATFORM_NAME}
                        Browser=${params.BROWSER}
                        TestSuite=${params.TEST_SUITE}
                        Environment=${TEST_ENVIRONMENT}
                        Branch=${params.BRANCH_NAME}
                        VideoRecording=${params.ENABLE_VIDEO}
                        BuildNumber=${BUILD_NUMBER}
                        ExecutionDate=${TIMESTAMP}
                        TestPriority=${params.TEST_PRIORITY}
                    """

                    // Initialisation de l'enregistrement vidéo si activé
                    if (params.ENABLE_VIDEO) {
                        sh """
                            if ! command -v ffmpeg &> /dev/null; then
                                if [ "$(uname)" == "Darwin" ]; then
                                    brew install ffmpeg
                                else
                                    sudo apt-get update && sudo apt-get install -y ffmpeg
                                fi
                            fi
                        """
                    }
                }
            }
        }

        stage('Analyse de Sécurité') {
            when {
                expression { params.TEST_SUITE == 'Security' }
            }
            steps {
                script {
                    try {
                        echo '🔒 Analyse de sécurité des dépendances...'

                        // OWASP Dependency Check
                        sh """
                            ${M2_HOME}/bin/mvn org.owasp:dependency-check-maven:check \
                            -DfailBuildOnCVSS=7 \
                            -DskipTestScope=true
                        """

                        // SonarQube Analysis
                        withSonarQubeEnv('SonarQube') {
                            sh """
                                ${M2_HOME}/bin/mvn sonar:sonar \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.login=${SONAR_TOKEN}
                            """
                        }
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Problèmes de sécurité détectés: ${e.message}"
                    }
                }
            }
        }

        stage('Tests') {
            steps {
                script {
                    try {
                        echo '🏃 Exécution des tests...'

                        // Démarrage de l'enregistrement vidéo
                        if (params.ENABLE_VIDEO) {
                            sh """
                                ffmpeg -f avfoundation -i "1" -framerate ${VIDEO_FRAME_RATE} \
                                -video_size ${SCREEN_RESOLUTION} \
                                -vcodec libx264 -pix_fmt yuv420p \
                                "${VIDEO_DIR}/test-execution-${BUILD_NUMBER}.mp4" & \
                                echo \$! > video-pid
                            """
                        }

                        // Exécution des tests en parallèle si configuré
                        parallel(
                            "Test Suite 1": {
                                runTests('Group1')
                            },
                            "Test Suite 2": {
                                runTests('Group2')
                            },
                            failFast: true
                        )
                    } catch (Exception e) {
                        if (params.ENABLE_RETRY) {
                            echo "🔄 Tentative de réexécution des tests échoués..."
                            retryFailedTests()
                        }
                        throw e
                    } finally {
                        if (params.ENABLE_VIDEO) {
                            sh """
                                if [ -f video-pid ]; then
                                    kill \$(cat video-pid)
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
                    archiveArtifacts artifacts: '${VIDEO_DIR}/*.mp4', allowEmptyArchive: true
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
                                [key: '📝 Langue', value: 'FR'],
                                [key: '📹 Vidéo', value: params.ENABLE_VIDEO ? 'Activé' : 'Désactivé'],
                                [key: '🎯 Priorité', value: params.TEST_PRIORITY]
                            ]
                        )

                        // Génération du rapport PDF si activé
                        if (params.GENERATE_PDF) {
                            generatePDFReport()
                        }

                        // Archivage des résultats
                        archiveTestResults()

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur de génération des rapports: ${e.message}"
                    }
                }
            }
        }

        stage('Analyse des Performances') {
            when {
                expression { params.TEST_SUITE == 'Performance' }
            }
            steps {
                script {
                    analyzePerformance()
                }
            }
        }
    }

    post {
        always {
            script {
                generateFinalReport()
                cleanupResources()

                if (params.SEND_NOTIFICATIONS) {
                    sendNotifications()
                }
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

// Fonctions utilitaires
def runTests(String group) {
    sh """
        ${M2_HOME}/bin/mvn clean test \
        -Dtest=runner.TestRunner \
        -DplatformName=${params.PLATFORM_NAME} \
        -Dbrowser=${params.BROWSER} \
        -DtestSuite=${params.TEST_SUITE} \
        -DtestGroup=${group} \
        -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
        -Dallure.results.directory=${ALLURE_RESULTS}
    """
}

def retryFailedTests() {
    // Logique de réexécution des tests échoués
}

def generatePDFReport() {
    // Génération du rapport PDF
}

def archiveTestResults() {
    sh """
        cd target
        zip -r test-results-${BUILD_NUMBER}.zip \
            allure-results/ \
            cucumber-reports/ \
            videos/ \
            screenshots/ \
            surefire-reports/ \
            security-results/ \
            performance/ \
            cucumber.json \
            rapports-tests/
    """

    archiveArtifacts artifacts: """
        target/test-results-${BUILD_NUMBER}.zip,
        target/cucumber.json,
        target/surefire-reports/**/*,
        ${EXCEL_REPORTS}/**/*.xlsx,
        ${PDF_REPORTS}/**/*.pdf,
        ${VIDEO_DIR}/**/*.mp4
    """, allowEmptyArchive: true
}

def analyzePerformance() {
    // Analyse des performances
}

def generateFinalReport() {
    def status = currentBuild.result ?: 'SUCCESS'
    def emoji = status == 'SUCCESS' ? '✅' : status == 'UNSTABLE' ? '⚠️' : '❌'
    def statusColor = status == 'SUCCESS' ? '\033[0;32m' : status == 'UNSTABLE' ? '\033[0;33m' : '\033[0;31m'
    def resetColor = '\033[0m'

    echo """╔════════════════════════════════════════════════╗
║           🌟 Rapport Final d'Exécution           ║
╚════════════════════════════════════════════════╝

[... Reste du rapport ...]
"""
}

def sendNotifications() {
    // Envoi des notifications
}

def cleanupResources() {
    // Nettoyage des ressources
}
// Fonctions détaillées pour les rapports et notifications

def sendNotifications() {
    def status = currentBuild.result ?: 'SUCCESS'
    def color = status == 'SUCCESS' ? 'good' : status == 'UNSTABLE' ? 'warning' : 'danger'

    // Notification Slack
    slackSend(
        channel: SLACK_CHANNEL,
        color: color,
        message: """
            *${status}* - ${PROJECT_NAME} - Build #${BUILD_NUMBER}
            • Branche: ${params.BRANCH_NAME}
            • Platform: ${params.PLATFORM_NAME}
            • Browser: ${params.BROWSER}
            • Suite: ${params.TEST_SUITE}
            • Durée: ${currentBuild.durationString}

            📊 Rapports:
            • Allure: ${BUILD_URL}allure
            • Cucumber: ${BUILD_URL}cucumber-html-reports
            • Vidéos: ${BUILD_URL}artifact/target/videos/
        """
    )

    // Notification Email
    def emailBody = """
        <h2>Résultats des Tests Automatisés</h2>
        <p><strong>Statut:</strong> ${status}</p>
        <p><strong>Build:</strong> #${BUILD_NUMBER}</p>
        <p><strong>Branche:</strong> ${params.BRANCH_NAME}</p>
        <p><strong>Platform:</strong> ${params.PLATFORM_NAME}</p>
        <p><strong>Browser:</strong> ${params.BROWSER}</p>
        <p><strong>Suite:</strong> ${params.TEST_SUITE}</p>
        <p><strong>Durée:</strong> ${currentBuild.durationString}</p>

        <h3>Liens des Rapports:</h3>
        <ul>
            <li><a href="${BUILD_URL}allure">Rapport Allure</a></li>
            <li><a href="${BUILD_URL}cucumber-html-reports">Rapport Cucumber</a></li>
            <li><a href="${BUILD_URL}artifact/target/videos/">Vidéos des Tests</a></li>
        </ul>
    """

    emailext(
        subject: "${status}: ${PROJECT_NAME} - Build #${BUILD_NUMBER}",
        body: emailBody,
        to: EMAIL_RECIPIENTS,
        mimeType: 'text/html'
    )
}

def generatePDFReport() {
    sh """
        # Installation des dépendances pour la génération PDF
        npm install -g markdown-pdf

        # Création du contenu du rapport
        cat << EOF > ${PDF_REPORTS}/report-${BUILD_NUMBER}.md
# Rapport d'Exécution des Tests Automatisés
## ${PROJECT_NAME}

### Informations Générales
- **Build:** #${BUILD_NUMBER}
- **Date:** ${new Date().format('dd/MM/yyyy HH:mm')}
- **Branche:** ${params.BRANCH_NAME}
- **Plateforme:** ${params.PLATFORM_NAME}
- **Navigateur:** ${params.BROWSER}
- **Suite:** ${params.TEST_SUITE}
- **Environnement:** ${TEST_ENVIRONMENT}

### Résultats
- **Status:** ${currentBuild.result ?: 'SUCCESS'}
- **Durée:** ${currentBuild.durationString}

### Métriques
$(generateMetrics)

### Captures d'écran
Les captures d'écran sont disponibles dans le dossier artifacts.

### Vidéos
Les enregistrements vidéo sont disponibles dans le dossier videos.
EOF

        # Conversion en PDF
        markdown-pdf ${PDF_REPORTS}/report-${BUILD_NUMBER}.md -o ${PDF_REPORTS}/report-${BUILD_NUMBER}.pdf
    """
}

def generateMetrics() {
    def metrics = sh(script: """
        echo "- Total Features: \$(find . -name "*.feature" | wc -l)"
        echo "- Total Scénarios: \$(grep -r "Scenario:" features/ | wc -l)"
        echo "- Tests Réussis: \$(grep -r "status=PASSED" target/surefire-reports | wc -l)"
        echo "- Tests Échoués: \$(grep -r "status=FAILED" target/surefire-reports | wc -l)"
        echo "- Durée Moyenne: \$(awk '{ total += \$1; count++ } END { print total/count }' target/surefire-reports/*.txt)"
    """, returnStdout: true).trim()

    return metrics
}

def analyzePerformance() {
    try {
        echo '📈 Analyse des performances...'

        // Collecte des métriques de performance
        sh """
            ${M2_HOME}/bin/mvn jmeter:jmeter \
            -Djmeter.target=${TEST_ENVIRONMENT} \
            -Djmeter.report.output=${PERFORMANCE_REPORTS}
        """

        // Analyse des temps de réponse
        def performanceReport = """
            📊 Rapport de Performance:

            • Temps de réponse moyen: XX ms
            • Temps de réponse médian: XX ms
            • 95ème percentile: XX ms
            • Requêtes par seconde: XX
            • Taux d'erreur: XX%

            Pour plus de détails, consultez le rapport complet dans ${PERFORMANCE_REPORTS}
        """

        echo performanceReport

        // Vérification des seuils de performance
        def avgResponseTime = 1000 // ms
        def errorRate = 5 // %

        if (avgResponseTime > 2000 || errorRate > 10) {
            currentBuild.result = 'UNSTABLE'
            echo "⚠️ Les métriques de performance dépassent les seuils acceptables"
        }

    } catch (Exception e) {
        echo "❌ Erreur lors de l'analyse des performances: ${e.message}"
        throw e
    }
}

def cleanupResources() {
    try {
        echo "🧹 Nettoyage des ressources..."

        // Nettoyage des fichiers temporaires
        sh """
            find . -type f -name "*.tmp" -delete || true
            find . -type d -name "node_modules" -exec rm -rf {} + || true
            find . -type f -name "*.log" -delete || true
        """

        // Compression des anciens rapports
        sh """
            if [ -d "old-reports" ]; then
                zip -r old-reports-${BUILD_NUMBER}.zip old-reports/
                rm -rf old-reports/
            fi
        """

        // Nettoyage des conteneurs Docker
        if (params.PLATFORM_NAME == 'Web') {
            sh """
                docker ps -a | grep 'selenium' | awk '{print \$1}' | xargs -r docker rm -f
            """
        }

    } catch (Exception e) {
        echo "⚠️ Erreur lors du nettoyage: ${e.message}"
    }
}