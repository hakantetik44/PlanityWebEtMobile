pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        // Existing environment variables remain the same
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')

        // Existing report directories
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'

        // New video recording directory
        VIDEO_REPORTS = 'target/videos'
        SCREENSHOT_DIR = 'target/screenshots'

        TEST_ENVIRONMENT = 'Production'
        TEAM_NAME = 'Quality Assurance'
        PROJECT_VERSION = '1.0.0'

        // Recording configuration
        RECORD_VIDEO = 'true'
        VIDEO_FOLDER = "${WORKSPACE}/${VIDEO_REPORTS}"
        FFMPEG_FRAMERATE = '30'
        SCREEN_RESOLUTION = '1920x1080'
    }

    // Existing parameters remain the same...

    stages {
        stage('Initialization') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    // Existing checkout remains the same...

                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p ${SCREENSHOT_DIR}
                        mkdir -p ${VIDEO_REPORTS}

                        # Install ffmpeg if not present
                        if ! command -v ffmpeg &> /dev/null; then
                            echo "Installing ffmpeg..."
                            apt-get update && apt-get install -y ffmpeg
                        fi

                        echo "🔧 Configuration de l'environnement..."
                        echo "Platform=${params.PLATFORM_NAME}" > ${ALLURE_RESULTS}/environment.properties
                        echo "Browser=${params.BROWSER}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Branch=${params.BRANCH_NAME}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "TestSuite=${params.TEST_SUITE}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Environment=${TEST_ENVIRONMENT}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "VideoRecording=Enabled" >> ${ALLURE_RESULTS}/environment.properties
                    """

                    // Create video attachment template for Allure
                    sh """
                        cat << EOF > ${ALLURE_RESULTS}/video-attachment-template.ftl
                        <#if data.video??>
                            <div class="attachment-row">
                                <div class="attachment-title">Test Recording</div>
                                <video width="100%" height="auto" controls>
                                    <source src="videos/\${data.video}" type="video/mp4">
                                    Your browser does not support the video tag.
                                </video>
                            </div>
                        </#if>
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

                        // Start video recording
                        if (params.PLATFORM_NAME == 'Web') {
                            sh """
                                ffmpeg -y -f x11grab -video_size ${SCREEN_RESOLUTION} -i :0.0 \
                                -framerate ${FFMPEG_FRAMERATE} -pix_fmt yuv420p \
                                "${VIDEO_REPORTS}/test-recording-${BUILD_NUMBER}.mp4" & echo \$! > recording.pid
                            """
                        }

                        // Run tests with video recording enabled
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DtestSuite=${params.TEST_SUITE} \
                            -DrecordVideo=${RECORD_VIDEO} \
                            -DvideoFolder=${VIDEO_FOLDER} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Échec de l'exécution des tests: ${e.message}"
                    } finally {
                        // Stop video recording
                        if (params.PLATFORM_NAME == 'Web') {
                            sh """
                                if [ -f recording.pid ]; then
                                    kill \$(cat recording.pid)
                                    rm recording.pid
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

        stage('Reports') {
            steps {
                script {
                    try {
                        echo '📊 Génération des rapports...'

                        // Copy videos to Allure results
                        sh """
                            if [ -d "${VIDEO_REPORTS}" ]; then
                                cp -r ${VIDEO_REPORTS}/* ${ALLURE_RESULTS}/videos/ || true
                            fi
                        """

                        // Existing Allure configuration...
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Enhanced Cucumber Report with video info
                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                // Existing classifications remain...
                                [key: '📹 Video Recording', value: 'Enabled'],
                                [key: '🎥 Video Quality', value: "${SCREEN_RESOLUTION} @ ${FFMPEG_FRAMERATE}fps"]
                            ]
                        )

                        // Archive artifacts including videos
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
                                ${VIDEO_REPORTS}/**/*.mp4
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

    // Existing post section remains the same but add video info to the final report...
    post {
        always {
            script {
                // Existing report generation code...
                echo """
// ... (previous report content) ...

📊 Artifacts Générés:
▪️ 📹 Vidéos:     ${BUILD_URL}artifact/${VIDEO_REPORTS}/
▪️ 📸 Screenshots: ${BUILD_URL}artifact/${SCREENSHOT_DIR}/
▪️ 📊 Allure:      ${BUILD_URL}allure/
▪️ 🥒 Cucumber:    ${BUILD_URL}cucumber-html-reports/
▪️ 📑 Excel:       ${BUILD_URL}artifact/${EXCEL_REPORTS}/
"""
            }
        }
    }
}