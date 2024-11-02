
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
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        EXCEL_REPORTS = 'target/rapports-tests'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        TEST_LOGS = 'target/test-logs'
    }

    parameters {
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
        booleanParam(
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo'
        )
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    echo "╔═══════════════════════════════╗\n║ Démarrage de l'Automatisation ║\n╚═══════════════════════════════╝"
                    cleanWs()
                    checkout scm

                    if (fileExists('src/test/resources/configuration.properties')) {
                        def configContent = sh(
                            script: 'cat src/test/resources/configuration.properties',
                            returnStdout: true
                        ).trim()

                        def props = configContent.split('\n').collectEntries { line ->
                            def parts = line.split('=')
                            if (parts.size() == 2) {
                                [(parts[0].trim()): parts[1].trim()]
                            } else {
                                [:]
                            }
                        }

                        env.PLATFORM_NAME = props.platformName ?: params.PLATFORM_NAME ?: 'Web'
                        env.BROWSER = env.PLATFORM_NAME == 'Web' ? (props.browser ?: params.BROWSER ?: 'chrome') : ''

                        writeFile file: 'target/allure-results/environment.properties', text: """
                            Platform=${env.PLATFORM_NAME}
                            Browser=${env.BROWSER}
                            Test Framework=Cucumber
                            Language=FR
                        """.stripIndent()
                    }

                    echo """Configuration:
                    • Plateforme: ${env.PLATFORM_NAME}
                    • Navigateur: ${env.PLATFORM_NAME == 'Web' ? env.BROWSER : 'N/A'}"""

                    sh """
                        mkdir -p ${EXCEL_REPORTS}/videos ${ALLURE_RESULTS} ${CUCUMBER_REPORTS} ${TEST_LOGS}
                        mkdir -p target/screenshots
                        export JAVA_HOME=${JAVA_HOME}
                        java -version
                        ${M2_HOME}/bin/mvn -version
                    """
                }
            }
        }

        stage('Construction') {
            steps {
                script {
                    try {
                        echo "📦 Installation des dépendances..."
                        sh "${M2_HOME}/bin/mvn clean install -DskipTests -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }

        stage('Exécution des Tests') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        if (params.RECORD_VIDEO) {
                            sh """
                                # Démarrer l'enregistrement vidéo
                                ffmpeg -f x11grab -video_size 1920x1080 -i :0.0 \
                                -codec:v libx264 -r 30 \
                                ${EXCEL_REPORTS}/videos/test-execution-${TIMESTAMP}.mp4 \
                                2>${TEST_LOGS}/video.log &
                                echo \$! > .recording.pid
                            """
                        }

                        sh """
                            ${M2_HOME}/bin/mvn test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DvideoFolder=${EXCEL_REPORTS}/videos \
                            -DscreenshotFolder=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
                        """

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur pendant l'exécution des tests: ${e.message}"
                    } finally {
                        if (params.RECORD_VIDEO) {
                            sh "if [ -f .recording.pid ]; then kill \$(cat .recording.pid); rm .recording.pid; fi"
                        }
                    }
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        // Create archives
                        sh """
                            cd target

                            # Créer l'archive des résultats de test avec les vidéos
                            zip -r test-results-${TIMESTAMP}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                test-logs/

                            # Créer une archive séparée pour les vidéos
                            cd ${EXCEL_REPORTS}
                            zip -r test-videos-${TIMESTAMP}.zip videos/
                        """

                        // Allure Report
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Report
                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: 'Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': 'Platform', 'value': params.PLATFORM_NAME],
                                ['key': 'Browser', 'value': params.BROWSER],
                                ['key': 'Video Recording', 'value': params.RECORD_VIDEO ? 'Enabled' : 'Disabled']
                            ]

                        // Archive all artifacts
                        archiveArtifacts artifacts: """
                            ${EXCEL_REPORTS}/**/*.xlsx,
                            ${EXCEL_REPORTS}/videos/**/*.mp4,
                            ${EXCEL_REPORTS}/test-videos-${TIMESTAMP}.zip,
                            target/allure-report.zip,
                            target/cucumber-reports.zip,
                            target/cucumber.json,
                            target/test-results-${TIMESTAMP}.zip
                        """, allowEmptyArchive: true

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur lors de la génération des rapports: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def duration = currentBuild.durationString

                echo """╔════════════════════════════╗
║    Résumé de l'Exécution    ║
╚════════════════════════════╝

🏗️ Build: #${BUILD_NUMBER}
⏱️ Durée: ${duration}
📱 Plateforme: ${params.PLATFORM_NAME}
🌐 Navigateur: ${params.BROWSER}
🎥 Enregistrement Vidéo: ${params.RECORD_VIDEO ? 'Activé' : 'Désactivé'}

📊 Rapports:
• Allure: ${BUILD_URL}allure/
• Cucumber: ${BUILD_URL}cucumber-html-reports/overview-features.html
• Vidéos: ${BUILD_URL}artifact/${EXCEL_REPORTS}/videos/
• Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}/

${status == 'SUCCESS' ? '✅ SUCCÈS' : status == 'UNSTABLE' ? '⚠️ INSTABLE' : '❌ ÉCHEC'}"""

                // Cleanup workspace but keep reports
                sh """
                    if [ -d "target" ]; then
                        find target -type f ! -name '*.zip' ! -name '*.xlsx' ! -name '*.json' ! -name '*.mp4' ! -name '*.png' -delete
                    fi
                """
            }
        }

        failure {
            script {
                echo "❌ Des échecs ont été détectés. Consultez les rapports pour plus de détails."
            }
        }

        cleanup {
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/classes/', type: 'INCLUDE'],
                    [pattern: 'target/test-classes/', type: 'INCLUDE'],
                    [pattern: '**/.git/', type: 'INCLUDE']
                ]
            )
        }
    }
}
