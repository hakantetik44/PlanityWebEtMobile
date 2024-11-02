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
        BUILD_DIR = "target"
        ALLURE_RESULTS = "${BUILD_DIR}/allure-results"
        CUCUMBER_REPORTS = "${BUILD_DIR}/cucumber-reports"
        PDF_REPORTS = "${BUILD_DIR}/pdf-reports"
        CUCUMBER_JSON = "${BUILD_DIR}/cucumber.json"
        TEST_LOGS = "${BUILD_DIR}/test-logs"
        RETRY_COUNT = 2
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'hakan'],
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
        booleanParam(
            name: 'GENERATE_PDF',
            defaultValue: true,
            description: 'Générer un rapport PDF'
        )
        booleanParam(
            name: 'RUN_RETRY',
            defaultValue: true,
            description: 'Réessayer les tests échoués'
        )
    }

    stages {
        stage('Prepare Test Environment') {
            steps {
                script {
                    // Temizlik
                    cleanWs()

                    // Branch checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Klasörleri oluştur
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${PDF_REPORTS}
                        mkdir -p ${TEST_LOGS}
                        mkdir -p ${BUILD_DIR}/screenshots
                    """

                    // Test konfigürasyonu
                    writeFile file: "${BUILD_DIR}/test-config.json", text: """
                        {
                            "platform": "${params.PLATFORM_NAME}",
                            "browser": "${params.BROWSER}",
                            "timestamp": "${TIMESTAMP}",
                            "buildNumber": "${BUILD_NUMBER}"
                        }
                    """
                }
            }
        }

        stage('Build & Verify') {
            steps {
                script {
                    try {
                        sh "${M2_HOME}/bin/mvn clean verify -B -DskipTests"
                    } catch (Exception e) {
                        error "Build failed: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    int retryCount = params.RUN_RETRY ? RETRY_COUNT : 0
                    boolean testSuccess = false

                    for (int i = 0; i <= retryCount && !testSuccess; i++) {
                        if (i > 0) {
                            echo "📝 Tentative de test #${i+1}"
                        }

                        try {
                            sh """
                                ${M2_HOME}/bin/mvn test \
                                -Dtest=runner.TestRunner \
                                -DplatformName=${params.PLATFORM_NAME} \
                                -Dbrowser=${params.BROWSER} \
                                -DscreenshotPath=${BUILD_DIR}/screenshots \
                                -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON},html:${CUCUMBER_REPORTS}" \
                                -Dcucumber.features=src/test/resources/features \
                                -Dallure.results.directory=${ALLURE_RESULTS} \
                                -Dtest.logs=${TEST_LOGS} \
                                -Dfailsafe.rerunFailingTestsCount=${retryCount}
                            """
                            testSuccess = true
                        } catch (Exception e) {
                            if (i == retryCount) {
                                error "Test execution failed after ${i+1} attempts: ${e.getMessage()}"
                            }
                            echo "⚠️ Test failed, retrying..."
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        // Test logs'u kaydet
                        sh "find ${TEST_LOGS} -name '*.log' -exec gzip {} \\;"
                        archiveArtifacts "${TEST_LOGS}/**/*.log.gz"

                        // Screenshots
                        archiveArtifacts "${BUILD_DIR}/screenshots/**/*"
                    }
                }
            }
        }

        stage('Generate Reports') {
            parallel {
                stage('Allure Report') {
                    steps {
                        script {
                            allure([
                                includeProperties: true,
                                jdk: '',
                                properties: [],
                                reportBuildPolicy: 'ALWAYS',
                                results: [[path: "${ALLURE_RESULTS}"]]
                            ])
                        }
                    }
                }

                stage('Cucumber Report') {
                    steps {
                        script {
                            // Verify cucumber.json exists and is valid
                            if (!fileExists(CUCUMBER_JSON)) {
                                error "Cucumber JSON file not found: ${CUCUMBER_JSON}"
                            }

                            // Check if file is not empty
                            def jsonContent = readFile(CUCUMBER_JSON)
                            if (jsonContent.trim().isEmpty()) {
                                error "Cucumber JSON file is empty"
                            }

                            // Validate JSON format
                            def jsonSlurper = new groovy.json.JsonSlurper()
                            try {
                                jsonSlurper.parseText(jsonContent)
                            } catch (Exception e) {
                                error "Invalid Cucumber JSON format: ${e.getMessage()}"
                            }

                            // Generate Cucumber Report
                            cucumber buildStatus: 'UNSTABLE',
                                failedFeaturesNumber: 1,
                                failedScenariosNumber: 1,
                                skippedStepsNumber: 1,
                                failedStepsNumber: 1,
                                reportTitle: 'Planity Test Report',
                                fileIncludePattern: '**/cucumber.json',
                                sortingMethod: 'ALPHABETICAL',
                                trendsLimit: 10,
                                classifications: [
                                    [
                                        'key': 'Browser',
                                        'value': params.BROWSER
                                    ],
                                    [
                                        'key': 'Branch',
                                        'value': params.BRANCH_NAME
                                    ],
                                    [
                                        'key': 'Platform',
                                        'value': params.PLATFORM_NAME
                                    ]
                                ]
                        }
                    }
                }

                stage('PDF Report') {
                    when { expression { params.GENERATE_PDF } }
                    steps {
                        script {
                            // Generate PDF report using test results
                            sh """
                                echo "# Test Execution Report" > report.md
                                echo "## Build #${BUILD_NUMBER}" >> report.md
                                echo "### Configuration" >> report.md
                                echo "* Branch: ${params.BRANCH_NAME}" >> report.md
                                echo "* Platform: ${params.PLATFORM_NAME}" >> report.md
                                echo "* Browser: ${params.BROWSER}" >> report.md
                                echo "### Test Results" >> report.md

                                if [ -f "${TEST_LOGS}/summary.txt" ]; then
                                    cat "${TEST_LOGS}/summary.txt" >> report.md
                                fi

                                if [ -f "${CUCUMBER_JSON}" ]; then
                                    echo "### Test Scenarios" >> report.md
                                    jq -r '.[] | .elements[] | "* " + .name' "${CUCUMBER_JSON}" >> report.md
                                fi

                                pandoc report.md -o "${PDF_REPORTS}/TestReport_${BUILD_NUMBER}.pdf"
                            """
                        }
                    }
                }
            }
        }

        stage('Archive Results') {
            steps {
                script {
                    // Create results archive
                    sh """
                        cd ${BUILD_DIR}
                        zip -r test-results-${BUILD_NUMBER}.zip \
                            allure-results/ \
                            cucumber-reports/ \
                            screenshots/ \
                            test-logs/ \
                            pdf-reports/ \
                            cucumber.json
                    """

                    // Archive artifacts
                    archiveArtifacts artifacts: """
                        ${BUILD_DIR}/test-results-${BUILD_NUMBER}.zip,
                        ${PDF_REPORTS}/*.pdf,
                        ${CUCUMBER_JSON}
                    """, allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def statusEmoji = status == 'SUCCESS' ? '✅' : status == 'UNSTABLE' ? '⚠️' : '❌'

                // Test sonuçlarını analiz et
                def testSummary = ""
                if (fileExists(CUCUMBER_JSON)) {
                    def json = readJSON file: CUCUMBER_JSON
                    def scenarios = json.collect { it.elements }.flatten()
                    def passed = scenarios.count { it.steps.every { step -> step.result.status == 'passed' } }
                    def total = scenarios.size()
                    testSummary = "\nTest Results: ${passed}/${total} scenarios passed"
                }

                echo """╔═══════════════════════════════════════════╗
║             Rapport d'Exécution            ║
╚═══════════════════════════════════════════╝

🎯 Build: #${BUILD_NUMBER}
🌿 Branch: ${params.BRANCH_NAME}
🕒 Durée: ${currentBuild.durationString}
📱 Platform: ${params.PLATFORM_NAME}
🌐 Browser: ${params.BROWSER}
${testSummary}

📊 Reports:
🔹 Allure:    ${BUILD_URL}allure/
🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/
🔹 PDF:       ${BUILD_URL}artifact/${PDF_REPORTS}/
🔹 Logs:      ${BUILD_URL}artifact/${TEST_LOGS}/

${statusEmoji} Status Final: ${status}"""

                // Clean workspace
                cleanWs(
                    deleteDirs: true,
                    patterns: [
                        [pattern: 'target/classes/', type: 'INCLUDE'],
                        [pattern: 'target/test-classes/', type: 'INCLUDE']
                    ]
                )
            }
        }

        success {
            script {
                echo "✅ Build successful! All tests passed."
            }
        }

        failure {
            script {
                echo "❌ Build failed! Check the logs for details."
                // Hata detaylarını kaydet
                sh "find ${TEST_LOGS} -name '*.log' -exec gzip {} \\;"
                archiveArtifacts "${TEST_LOGS}/**/*.log.gz"
            }
        }

        unstable {
            script {
                echo "⚠️ Build unstable. Some tests may have failed."
            }
        }
    }
}