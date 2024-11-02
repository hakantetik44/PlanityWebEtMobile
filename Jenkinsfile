
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
        VIDEO_FOLDER = 'target/videos'
        PERFORMANCE_REPORTS = 'target/performance'
        TEST_LOGS = 'target/test-logs'
        AI_WORKSPACE = 'target/ai-workspace'
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
            name: 'GENERATE_AI_TESTS',
            defaultValue: false,
            description: 'Activer la génération de tests par IA'
        )
        booleanParam(
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo'
        )
        string(
            name: 'TEST_DESCRIPTION',
            defaultValue: '',
            description: 'Description pour la génération de tests IA'
        )
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    echo "╔═══════════════════════════════╗\n║ Démarrage de l'Automatisation ║\n╚═══════════════════════════════╝"
                    cleanWs()
                    checkout scm

                    // Create required directories
                    sh """
                        mkdir -p ${ALLURE_RESULTS} ${EXCEL_REPORTS} ${VIDEO_FOLDER}
                        mkdir -p ${TEST_LOGS} ${AI_WORKSPACE}
                        mkdir -p target/screenshots
                    """

                    // Write environment properties
                    writeFile file: "${ALLURE_RESULTS}/environment.properties", text: """
                        Platform=${params.PLATFORM_NAME}
                        Browser=${params.BROWSER}
                        Test Framework=Cucumber
                        Language=FR
                        AI_Enabled=${params.GENERATE_AI_TESTS}
                    """
                }
            }
        }

        stage('AI Test Generation') {
            when {
                expression { params.GENERATE_AI_TESTS == true }
            }
            steps {
                script {
                    try {
                        echo "🤖 Génération de tests avec l'IA..."

                        // Load OpenAI credentials
                        withCredentials([string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY')]) {
                            // Initialize TestGenerator
                            def testGenerator = load "src/test/java/utils/TestGenerator.groovy"
                            testGenerator.init(OPENAI_API_KEY)

                            // Generate test scenarios
                            if (params.TEST_DESCRIPTION) {
                                testGenerator.generateFeatureFileWithAI(params.TEST_DESCRIPTION)
                            }

                            // Analyze and report
                            testGenerator.analyzeTestHistory()

                            // Archive AI workspace
                            dir(AI_WORKSPACE) {
                                writeFile file: 'ai-metrics.json', text: readFile('target/test-metrics.json')
                                writeFile file: 'ai-history.json', text: readFile('target/test-history.json')
                            }
                            archiveArtifacts artifacts: "${AI_WORKSPACE}/**/*"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Erreur lors de la génération IA: ${e.message}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Construction') {
            steps {
                script {
                    try {
                        echo "📦 Installation des dépendances..."
                        sh "${M2_HOME}/bin/mvn clean install -DskipTests"
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "Échec de la construction: ${e.message}"
                    }
                }
            }
        }

        stage('Exécution des Tests') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        // Start video recording if enabled
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -f x11grab -video_size 1920x1080 -i :0.0 \
                                -codec:v libx264 -r 30 ${VIDEO_FOLDER}/test-execution-${TIMESTAMP}.mp4 \
                                2>${TEST_LOGS}/video.log &
                                echo \$! > .recording.pid
                            """
                        }

                        // Execute tests
                        sh """
                            ${M2_HOME}/bin/mvn test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DvideoFolder=${VIDEO_FOLDER} \
                            -DscreenshotFolder=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
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
                                ['key': 'AI Tests', 'value': params.GENERATE_AI_TESTS ? 'Yes' : 'No']
                            ]

                        // Archive test artifacts
                        sh """
                            cd target
                            zip -r test-results-${TIMESTAMP}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                videos/ \
                                test-logs/ \
                                ${params.GENERATE_AI_TESTS ? 'ai-workspace/' : ''}
                        """

                        archiveArtifacts artifacts: """
                            target/test-results-${TIMESTAMP}.zip,
                            ${VIDEO_FOLDER}/**/*.mp4,
                            target/screenshots/**/*.png,
                            ${EXCEL_REPORTS}/**/*.xlsx,
                            ${TEST_LOGS}/**/*
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
🤖 Tests IA: ${params.GENERATE_AI_TESTS ? 'Activé' : 'Désactivé'}

📊 Rapports:
• Allure: ${BUILD_URL}allure/
• Cucumber: ${BUILD_URL}cucumber-html-reports/overview-features.html
• Vidéos: ${BUILD_URL}artifact/${VIDEO_FOLDER}/
• Logs: ${BUILD_URL}artifact/${TEST_LOGS}/

${status == 'SUCCESS' ? '✅ SUCCÈS' : status == 'UNSTABLE' ? '⚠️ INSTABLE' : '❌ ÉCHEC'}"""

                // Cleanup workspace
                cleanWs(
                    deleteDirs: true,
                    patterns: [
                        [pattern: 'target/classes/', type: 'INCLUDE'],
                        [pattern: 'target/test-classes/', type: 'INCLUDE']
                    ]
                )
            }
        }
    }
}
