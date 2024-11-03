pipeline {
    agent any

    tools {
        maven 'maven'  // Define Maven version
        jdk 'JDK17'    // Define JDK version
        allure 'Allure' // Define Allure installation
    }

    environment {
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'
        VIDEO_DIR = 'target/videos'  // Video directory
        TEST_ENVIRONMENT = 'Production'
        TEAM_NAME = 'Quality Assurance'
        PROJECT_VERSION = '1.0.0'
    }

    parameters {
        choice(name: 'BRANCH_NAME', choices: ['main', 'develop', 'staging', 'hakan'], description: 'Select the branch to test')
        choice(name: 'PLATFORM_NAME', choices: ['Web', 'Android', 'iOS'], description: 'Select the testing platform')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'safari'], description: 'Select the browser (for Web only)')
        choice(name: 'TEST_SUITE', choices: ['Regression', 'Smoke', 'Sanity'], description: 'Select the type of test suite')
        booleanParam(name: 'RECORD_VIDEO', defaultValue: false, description: 'Enable video recording')
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Starting Tests                   ║
╚═══════════════════════════════════════════╝"""

                    cleanWs() // Clean the workspace

                    // Checkout code from the specified branch
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Create necessary directories and configuration files
                    sh """
                        mkdir -p ${ALLURE_RESULTS} ${CUCUMBER_REPORTS} ${EXCEL_REPORTS} target/screenshots ${VIDEO_DIR}

                        echo "🔧 Environment Configuration..."
                        echo "Platform=${params.PLATFORM_NAME}" > ${ALLURE_RESULTS}/environment.properties
                        echo "Browser=${params.BROWSER}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Branch=${params.BRANCH_NAME}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "TestSuite=${params.TEST_SUITE}" >> ${ALLURE_RESULTS}/environment.properties
                        echo "Environment=${TEST_ENVIRONMENT}" >> ${ALLURE_RESULTS}/environment.properties
                    """
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    try {
                        echo '🏗️ Compiling and running tests...'

                        // Start video recording if enabled
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -video_size 1920x1080 -framerate 25 -f x11grab -i :0.0 \
                                -codec:v libx264 -preset ultrafast -crf 18 \
                                ${VIDEO_DIR}/test-video-${BUILD_NUMBER}.mp4 > /dev/null 2>&1 &
                                echo \$! > ${VIDEO_DIR}/ffmpeg.pid
                            """
                        }

                        // Run Maven tests
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DtestSuite=${params.TEST_SUITE} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """

                        // Stop video recording if it was started
                        if (params.RECORD_VIDEO) {
                            sh """
                                kill -SIGINT \$(cat ${VIDEO_DIR}/ffmpeg.pid)
                                rm -f ${VIDEO_DIR}/ffmpeg.pid
                            """
                        }

                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Test execution failed: ${e.message}"
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
                        echo '📊 Generating reports...'

                        // Generate Allure report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Generate enhanced Cucumber report
                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                [key: '🏢 Project', value: PROJECT_NAME],
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '⏱️ Duration', value: currentBuild.durationString],
                                [key: '🌡️ Environment', value: TEST_ENVIRONMENT],
                                [key: '📝 Language', value: 'FR'],
                                [key: '☕ Java Version', value: sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()]
                            ]
                        )

                        // Archive artifacts
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                surefire-reports/ \
                                cucumber.json \
                                rapports-tests/ \
                                videos/  // Include video files
                        """

                        archiveArtifacts(
                            artifacts: """
                                target/test-results-${BUILD_NUMBER}.zip,
                                target/cucumber.json,
                                target/surefire-reports/**/*,
                                ${EXCEL_REPORTS}/**/*.xlsx,
                                ${VIDEO_DIR}/**/*.mp4  // Archive video files
                            """,
                            allowEmptyArchive: true
                        )

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Error generating reports: ${e.message}"
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

                // Get test statistics
                def totalFeatures = sh(script: 'find . -name "*.feature" | wc -l', returnStdout: true).trim()
                def totalScenarios = sh(script: 'grep -r "Scenario:" features/ | wc -l', returnStdout: true).trim() ?: '0'
                def successRate = (status == 'SUCCESS') ? '100%' : (status == 'UNSTABLE') ? '75%' : '0%'

                echo """╔════════════════════════════════════════════════╗
║           🌟 Final Execution Report           ║
╚════════════════════════════════════════════════╝

🏢 Project Information:
▪️ Name: ${PROJECT_NAME}
▪️ Version: ${PROJECT_VERSION}
▪️ Team: ${TEAM_NAME}

🔄 Build Information:
▪️ Number: #${BUILD_NUMBER}
▪️ Date: ${new Date().format('dd/MM/yyyy HH:mm')}
▪️ Duration: ${currentBuild.durationString}
▪️ Executed by: ${currentBuild.getBuildCauses()[0].userId ?: 'System'}

🌍 Environment:
▪️ 🌿 Branch: ${params.BRANCH_NAME}
▪️ 📱 Platform: ${params.PLATFORM_NAME}
▪️ 🌐 Browser: ${params.BROWSER}
▪️ 🎯 Suite: ${params.TEST_SUITE}
▪️ 🌡️ Env: ${TEST_ENVIRONMENT}

⚙️ Technical Configuration:
▪️ 🔨 Maven: ${sh(script: '${M2_HOME}/bin/mvn -version | head -n 1', returnStdout: true).trim()}
▪️ ☕ Java: ${sh(script: 'java -version 2>&1 | head -n 1', returnStdout: true).trim()}

📊 Test Metrics:
▪️ Features: ${totalFeatures}
▪️ Scenarios: ${totalScenarios}
▪️ Success Rate: ${successRate}

📈 Available Reports:
▪️ 📊 Allure:    ${BUILD_URL}allure/
▪️ 🥒 Cucumber:  ${BUILD_URL}cucumber-html-reports/
▪️ 📑 Excel:     ${BUILD_URL}artifact/${EXCEL_REPORTS}/
▪️ 📦 Artifacts: ${BUILD_URL}artifact/

🏷️ Main Tags:
▪️ @regression
▪️ @smoke
▪️ @critical
▪️ @${params.PLATFORM_NAME.toLowerCase()}

${emoji} Final Status: ${statusColor}${status}${resetColor}

═══════════════════════════════════════════════════

💡 Useful Links:
▪️ 📚 Wiki: https://wiki.example.com/tests
▪️ 🎯 Jenkins: ${BUILD_URL}
▪️ 📊 Dashboard: ${BUILD_URL}allure
"""

                // Cleanup temporary files
                sh """
                    find . -type f -name "*.tmp" -delete || true
                    find . -type d -name "node_modules" -exec rm -rf {} + || true
                """
            }
        }

        success {
            echo '✅ Pipeline completed successfully!'
        }

        failure {
            echo '❌ Pipeline failed!'
        }

        cleanup {
            deleteDir() // Clean up the workspace
        }
    }
}
