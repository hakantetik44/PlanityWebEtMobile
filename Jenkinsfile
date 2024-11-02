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
        SCREENSHOT_FOLDER = 'target/screenshots'
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

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    echo "╔═══════════════════════════════╗\n║ Démarrage de l'Automatisation ║\n╚═══════════════════════════════╝"
                    cleanWs()
                    checkout scm

                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} ${VIDEO_FOLDER} ${SCREENSHOT_FOLDER}
                        export JAVA_HOME=${JAVA_HOME}
                        java -version
                        ${M2_HOME}/bin/mvn -version
                    """

                    writeFile file: 'src/test/resources/config.properties', text: """
                        platformName=${params.PLATFORM_NAME}
                        browser=${params.BROWSER}
                        recordVideo=${params.RECORD_VIDEO}
                        videoFolder=${VIDEO_FOLDER}
                        screenshotFolder=${SCREENSHOT_FOLDER}
                        allureResultsDir=${ALLURE_RESULTS}
                        testEnvironment=Jenkins
                        buildNumber=${BUILD_NUMBER}
                        rerunFailedTests=true
                        maxRetryCount=2
                    """
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

                        def mvnCommand = """
                            ${M2_HOME}/bin/mvn test
                            -Dtest=runner.TestRunner
                            -DplatformName=${params.PLATFORM_NAME}
                            -Dbrowser=${params.BROWSER}
                            -DrecordVideo=${params.RECORD_VIDEO}
                            -DvideoFolder=${VIDEO_FOLDER}
                            -DscreenshotFolder=${SCREENSHOT_FOLDER}
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
                            -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
                        """

                        sh "${mvnCommand}"
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        archiveArtifacts artifacts: "${SCREENSHOT_FOLDER}/**/*.png", allowEmptyArchive: true
                        throw e
                    }
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        // Zip video files
                        sh """
                            if [ -d "${VIDEO_FOLDER}" ]; then
                                cd target && zip -r test-execution-videos.zip videos/
                            fi
                        """

                        // Allure Reports
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Reports
                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: 'Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': 'Platform', 'value': params.PLATFORM_NAME],
                                ['key': 'Browser', 'value': params.BROWSER],
                                ['key': 'Environment', 'value': 'Jenkins'],
                                ['key': 'Build', 'value': BUILD_NUMBER]
                            ]

                        // Create report archives
                        sh """
                            cd target
                            zip -r allure-report.zip allure-results/
                            zip -r cucumber-reports.zip cucumber-reports/
                            zip -r screenshots.zip screenshots/
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: """
                        ${EXCEL_REPORTS}/**/*.xlsx,
                        target/allure-report.zip,
                        target/cucumber-reports.zip,
                        target/cucumber.json,
                        target/test-execution-videos.zip,
                        target/screenshots.zip,
                        ${VIDEO_FOLDER}/**/*.mp4,
                        ${SCREENSHOT_FOLDER}/**/*.png
                    """, allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def testResults = fileExists('target/cucumber.json') ?
                    groovy.json.JsonSlurper().parse(new File('target/cucumber.json')) : []

                def summary = """╔═══════════════════════════╗
║   Résumé de l'Exécution   ║
╚═══════════════════════════╝

📝 Rapports:
• Allure: ${BUILD_URL}allure/
• Cucumber: ${BUILD_URL}cucumber-html-reports/overview-features.html
• Vidéos: ${BUILD_URL}artifact/target/test-execution-videos.zip
• Screenshots: ${BUILD_URL}artifact/target/screenshots.zip
• Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}/

🔍 Configuration:
• Plateforme: ${params.PLATFORM_NAME}
• Navigateur: ${params.BROWSER}
• Enregistrement Vidéo: ${params.RECORD_VIDEO}
• Build: #${BUILD_NUMBER}

${status == 'SUCCESS' ? '✅ SUCCÈS' : '❌ ÉCHEC'}"""

                echo summary

                // Clean workspace but keep reports
                sh """
                    if [ -d "target" ]; then
                        find target -type f ! -name '*.zip' ! -name '*.xlsx' ! -name '*.json' ! -name '*.mp4' ! -name '*.png' -delete
                    fi
                """
            }
        }
        failure {
            echo "❌ Des échecs ont été détectés. Consultez les rapports pour plus de détails."
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
