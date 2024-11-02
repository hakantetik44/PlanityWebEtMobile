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
        WORKSPACE_DIR = pwd()
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_RESULTS = 'target/cucumber-reports'
        CUCUMBER_JSON = 'target/cucumber.json'
        SCREENSHOTS_DIR = 'target/screenshots'
        TEST_RESULTS = 'target/surefire-reports'
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
            name: 'RETRY_FAILED_TESTS',
            defaultValue: true,
            description: 'Réessayer les tests échoués'
        )
        string(
            name: 'MAX_RETRY_COUNT',
            defaultValue: '2',
            description: 'Nombre maximum de tentatives pour les tests échoués'
        )
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    // Temizlik ve hazırlık
                    cleanWs()

                    // Git checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [
                            [$class: 'CleanBeforeCheckout'],
                            [$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true]
                        ],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Klasör yapısı oluşturma
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_RESULTS}
                        mkdir -p ${SCREENSHOTS_DIR}
                        mkdir -p ${TEST_RESULTS}

                        echo "Build Info:" > ${WORKSPACE_DIR}/build-info.txt
                        echo "Build Number: ${BUILD_NUMBER}" >> ${WORKSPACE_DIR}/build-info.txt
                        echo "Branch: ${params.BRANCH_NAME}" >> ${WORKSPACE_DIR}/build-info.txt
                        echo "Platform: ${params.PLATFORM_NAME}" >> ${WORKSPACE_DIR}/build-info.txt
                        echo "Browser: ${params.BROWSER}" >> ${WORKSPACE_DIR}/build-info.txt
                        echo "Timestamp: ${TIMESTAMP}" >> ${WORKSPACE_DIR}/build-info.txt
                    """
                }
            }
        }

        stage('Build & Dependencies') {
            steps {
                script {
                    try {
                        echo "📦 Installation des dépendances..."
                        sh """
                            ${M2_HOME}/bin/mvn clean install -DskipTests \
                            -Dmaven.test.failure.ignore=true \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON},html:${CUCUMBER_RESULTS}" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Erreur lors de l'installation des dépendances: ${e.message}"
                    }
                }
            }
        }

        stage('Test Execution') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        def maxRetries = params.RETRY_FAILED_TESTS ? params.MAX_RETRY_COUNT.toInteger() : 0
                        def success = false
                        def attempt = 0

                        while (!success && attempt <= maxRetries) {
                            attempt++
                            echo "📋 Tentative ${attempt}/${maxRetries + 1}"

                            try {
                                sh """
                                    ${M2_HOME}/bin/mvn test \
                                    -Dtest=runner.TestRunner \
                                    -DplatformName=${params.PLATFORM_NAME} \
                                    -Dbrowser=${params.BROWSER} \
                                    -DscreenshotsDir=${SCREENSHOTS_DIR} \
                                    -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON},html:${CUCUMBER_RESULTS}" \
                                    -Dallure.results.directory=${ALLURE_RESULTS} \
                                    -Dmaven.test.failure.ignore=true
                                """

                                // Verify cucumber.json exists and is valid
                                if (fileExists(CUCUMBER_JSON)) {
                                    def jsonContent = readFile(CUCUMBER_JSON)
                                    if (jsonContent.trim()) {
                                        success = true
                                    } else {
                                        error "Cucumber JSON file is empty"
                                    }
                                } else {
                                    error "Cucumber JSON file not created"
                                }

                            } catch (Exception e) {
                                if (attempt > maxRetries) {
                                    throw e
                                }
                                echo "⚠️ Tentative ${attempt} échouée, nouvelle tentative..."
                            }
                        }

                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "❌ Erreur lors de l'exécution des tests: ${e.message}"
                    }
                }
            }
            post {
                always {
                    // Collect and save test results
                    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                }
            }
        }

        stage('Generate Reports') {
            steps {
                script {
                    try {
                        // Validate Cucumber JSON
                        if (fileExists(CUCUMBER_JSON)) {
                            def jsonContent = readFile(CUCUMBER_JSON)
                            if (!jsonContent.trim()) {
                                error "Cucumber JSON file is empty"
                            }
                        } else {
                            error "Cucumber JSON file not found"
                        }

                        // Allure Report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Report
                        cucumber buildStatus: 'UNSTABLE',
                            failedFeaturesNumber: -1,
                            failedScenariosNumber: -1,
                            failedStepsNumber: -1,
                            fileIncludePattern: '**/*.json',
                            jsonReportDirectory: 'target',
                            pendingStepsNumber: -1,
                            skippedStepsNumber: -1,
                            sortingMethod: 'ALPHABETICAL',
                            undefinedStepsNumber: -1

                        // Archive test results and reports
                        sh """
                            cd ${WORKSPACE_DIR}/target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                surefire-reports/ \
                                cucumber.json
                        """

                        archiveArtifacts artifacts: """
                            target/test-results-${BUILD_NUMBER}.zip,
                            target/cucumber.json,
                            build-info.txt
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
                def statusEmoji = status == 'SUCCESS' ? '✅' : status == 'UNSTABLE' ? '⚠️' : '❌'

                // Test Results Analysis
                def testResults = []
                if (fileExists('target/surefire-reports')) {
                    testResults = findFiles(glob: 'target/surefire-reports/*.xml')
                }

                echo """╔═══════════════════════════════════════════╗
║             Résumé d'Exécution              ║
╚═══════════════════════════════════════════╝

🎯 Build: #${BUILD_NUMBER}
🌿 Branch: ${params.BRANCH_NAME}
🕒 Durée: ${currentBuild.durationString}
📱 Plateforme: ${params.PLATFORM_NAME}
🌐 Navigateur: ${params.BROWSER}

📊 Rapports:
🔹 Allure:    ${BUILD_URL}allure/
🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/
🔹 Artifacts: ${BUILD_URL}artifact/

📝 Test Results:
- Nombre de fichiers de test: ${testResults.size()}
- Résultat final: ${status}

${statusEmoji} Statut Final: ${status}
"""

                // Cleanup workspace but keep reports
                sh """
                    find . -type f -name "*.tmp" -delete
                    find . -type d -name "node_modules" -exec rm -rf {} +
                """
            }
        }

        success {
            echo '✅ Pipeline completed successfully!'
        }

        failure {
            echo '❌ Pipeline failed!'
        }

        unstable {
            echo '⚠️ Pipeline is unstable!'
        }
    }
}