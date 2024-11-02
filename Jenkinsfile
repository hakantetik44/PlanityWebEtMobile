pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
        allure 'Allure'
    }

    environment {
        // Base Configuration
        JAVA_HOME = "/usr/local/opt/openjdk@17"
        M2_HOME = tool 'maven'
        PATH = "${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}"
        MAVEN_OPTS = '-Xmx3072m'

        // Project Information
        PROJECT_NAME = 'Planity Web Et Mobile BDD Automation Tests'
        PROJECT_VERSION = '1.0.0'
        TEAM_NAME = 'Quality Assurance'
        BASE_URL = 'https://www.planity.com'

        // Report Directories
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        CUCUMBER_JSON_PATH = 'target/cucumber.json'
        EXCEL_REPORTS = 'target/rapports-tests'

        // Test Configuration
        TEST_ENVIRONMENT = 'Production'
        DEVICE_NAME = 'Web Chrome'
        TEST_RAIL_PROJECT = 'PLANITY-001'
        JIRA_PROJECT = 'PLANITY'
        SELENIUM_GRID_URL = 'http://localhost:4444/wd/hub'

        // Mail Configuration
        EMAIL_TO = 'team@company.com'
        EMAIL_FROM = 'jenkins@company.com'
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'staging', 'hakan', 'feature/*', 'hotfix/*'],
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
            choices: ['Regression', 'Smoke', 'Sanity', 'E2E', 'Critical'],
            description: 'Sélectionnez le type de suite de test'
        )
        choice(
            name: 'TEST_ENV',
            choices: ['Production', 'Staging', 'QA', 'Dev'],
            description: 'Sélectionnez l\'environnement de test'
        )
        booleanParam(
            name: 'SEND_NOTIFICATION',
            defaultValue: true,
            description: 'Envoyer des notifications par email'
        )
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    // Welcome Banner
                    echo """╔═══════════════════════════════════════════╗
║         🚀 Démarrage des Tests             ║
╚═══════════════════════════════════════════╝"""

                    cleanWs()

                    // Update DEVICE_NAME based on platform
                    switch(params.PLATFORM_NAME) {
                        case 'Web':
                            env.DEVICE_NAME = "Web ${params.BROWSER.capitalize()}"
                            env.TEST_ENV_URL = params.TEST_ENV == 'Production' ?
                                'https://www.planity.com' :
                                "https://${params.TEST_ENV.toLowerCase()}.planity.com"
                            break
                        case 'Android':
                            env.DEVICE_NAME = "Android Emulator"
                            env.TEST_ENV_URL = 'mobile://android'
                            break
                        case 'iOS':
                            env.DEVICE_NAME = "iOS Simulator"
                            env.TEST_ENV_URL = 'mobile://ios'
                            break
                    }

                    // Git checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])

                    // Create directory structure and configuration files
                    sh """
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p ${EXCEL_REPORTS}
                        mkdir -p target/screenshots
                        mkdir -p target/downloads

                        # Create Allure environment.properties
                        cat << EOF > ${ALLURE_RESULTS}/environment.properties
Platform=${params.PLATFORM_NAME}
Browser=${params.BROWSER}
Device=${env.DEVICE_NAME}
Environment=${params.TEST_ENV}
Branch=${params.BRANCH_NAME}
Base URL=${env.TEST_ENV_URL}
Test Suite=${params.TEST_SUITE}
Build Number=${BUILD_NUMBER}
Jenkins URL=${BUILD_URL}
Java Version=\$(java -version 2>&1)
Team=${TEAM_NAME}
Project=${PROJECT_NAME}
Version=${PROJECT_VERSION}
TestRail Project=${TEST_RAIL_PROJECT}
JIRA Project=${JIRA_PROJECT}
Execution Date=${TIMESTAMP}
Selenium Grid=${SELENIUM_GRID_URL}
EOF

                        # Create Allure categories.json
                        cat << EOF > ${ALLURE_RESULTS}/categories.json
{
  "name": "Test Defects Categories",
  "messageRegex": ".*",
  "matchedStatuses": ["failed"],
  "categories": [
    {
      "name": "🔧 Infrastructure Problems",
      "messageRegex": ".*ConnectionError.*|.*ConnectTimeout.*|.*TimeoutException.*",
      "matchedStatuses": ["broken"]
    },
    {
      "name": "🖱️ Element Interaction Issues",
      "messageRegex": ".*ElementClickInterceptedException.*|.*ElementNotInteractableException.*",
      "matchedStatuses": ["broken"]
    },
    {
      "name": "❌ Test Failures",
      "messageRegex": ".*AssertionError.*|.*assertEquals.*",
      "matchedStatuses": ["failed"]
    },
    {
      "name": "⚙️ Configuration Issues",
      "messageRegex": ".*Configuration.*|.*InitializationError.*",
      "matchedStatuses": ["broken"]
    },
    {
      "name": "📱 Mobile Specific Issues",
      "messageRegex": ".*AppiumError.*|.*DeviceNotFound.*",
      "matchedStatuses": ["broken"]
    }
  ]
}
EOF

                        # Create Allure executor.json
                        cat << EOF > ${ALLURE_RESULTS}/executor.json
{
  "name": "Jenkins",
  "type": "jenkins",
  "buildName": "Planity Tests #${BUILD_NUMBER}",
  "buildUrl": "${BUILD_URL}",
  "reportUrl": "${BUILD_URL}allure",
  "buildOrder": "${BUILD_NUMBER}"
}
EOF
                    """

                    // Display Configuration Summary
                    echo """📋 Configuration:
▪️ Platform: ${params.PLATFORM_NAME}
▪️ Device: ${env.DEVICE_NAME}
▪️ Environment: ${params.TEST_ENV}
▪️ Test URL: ${env.TEST_ENV_URL}
▪️ Branch: ${params.BRANCH_NAME}
▪️ Test Suite: ${params.TEST_SUITE}"""
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    try {
                        echo '🏗️ Compilation et exécution des tests...'

                        // Define test tags based on suite
                        def testTags = params.TEST_SUITE.toLowerCase()
                        if (params.TEST_SUITE == 'Regression') {
                            testTags = '@regression'
                        } else if (params.TEST_SUITE == 'Smoke') {
                            testTags = '@smoke'
                        }

                        // Run Maven tests
                        sh """
                            ${M2_HOME}/bin/mvn clean test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DtestSuite=${params.TEST_SUITE} \
                            -Dcucumber.filter.tags="${testTags}" \
                            -Dallure.link.tms.pattern=https://testrail.company.com/index.php?/cases/view/{} \
                            -Dallure.link.issue.pattern=https://jira.company.com/browse/{} \
                            -Dallure.results.directory=${ALLURE_RESULTS} \
                            -Dbase.url=${env.TEST_ENV_URL} \
                            -Dselenium.grid.url=${SELENIUM_GRID_URL} \
                            -Dcucumber.plugin="pretty,json:${CUCUMBER_JSON_PATH},html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        sh """
                            echo "Test execution failed: ${e.message}" > ${ALLURE_RESULTS}/execution-error.txt
                        """
                        error "❌ Échec de l'exécution des tests: ${e.message}"
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

                        // Generate Allure Report
                        allure([
                            includeProperties: true,
                            jdk: '',
                            properties: [],
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Generate Enhanced Cucumber Report
                        cucumber(
                            fileIncludePattern: '**/cucumber.json',
                            jsonReportDirectory: 'target',
                            reportTitle: '🌟 Planity Test Report',
                            classifications: [
                                [key: '🏢 Project', value: PROJECT_NAME],
                                [key: '📌 Version', value: PROJECT_VERSION],
                                [key: '👥 Team', value: TEAM_NAME],
                                [key: '🌿 Branch', value: params.BRANCH_NAME],
                                [key: '📱 Platform', value: params.PLATFORM_NAME],
                                [key: '🌐 Browser', value: params.BROWSER],
                                [key: '🔄 Build', value: "#${BUILD_NUMBER}"],
                                [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')],
                                [key: '👨‍💻 Executed By', value: currentBuild.getBuildCauses()[0].userId ?: 'System'],
                                [key: '⏱️ Duration', value: currentBuild.durationString],
                                [key: '🌡️ Environment', value: params.TEST_ENV],
                                [key: '🎯 Test Suite', value: params.TEST_SUITE],
                                [key: '🔍 Framework', value: 'Cucumber with Selenium'],
                                [key: '📝 Language', value: 'FR']
                            ]
                        )

                        // Archive test results
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                downloads/ \
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
                                target/screenshots/**/*,
                                target/downloads/**/*
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

                // Get test statistics
                def totalFeatures = sh(script: 'find . -name "*.feature" | wc -l', returnStdout: true).trim()
                def totalScenarios = sh(script: 'grep -r "Scenario:" features/ | wc -l', returnStdout: true).trim() ?: '0'
                def successRate = status == 'SUCCESS' ? '100%' : status == 'UNSTABLE' ? '75%' : '0%'

                // Generate execution summary
                def summaryText = """╔════════════════════════════════════════════════╗
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
▪️ 🌿 Branch: ${params.BRANCH_NAME}
▪️ 📱 Platform: ${params.PLATFORM_NAME}
▪️ 🌐 Browser: ${params.BROWSER}
▪️ 🎯 Suite: ${params.TEST_SUITE}
▪️ 🌡️ Env: ${params.TEST_ENV}
▪️ 🔗 URL: ${env.TEST_ENV_URL}

