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
        PROJECT_NAME = 'Radio BDD Automation Tests'
        TIMESTAMP = new Date().format('yyyy-MM-dd_HH-mm-ss')
        ALLURE_RESULTS = 'target/allure-results'
        EXCEL_REPORTS = 'target/rapports-tests'
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
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    echo "╔═══════════════════════════════╗\n║ Démarrage de l'Automatisation ║\n╚═══════════════════════════════╝"
                    cleanWs()
                    checkout scm

                    // Varsayılan değerler
                    env.PLATFORM_NAME = params.PLATFORM_NAME ?: 'Web'
                    env.BROWSER = params.PLATFORM_NAME == 'Web' ? (params.BROWSER ?: 'chrome') : ''

                    // Properties dosyasını kontrol et ve oku
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

                        if (props.platformName) env.PLATFORM_NAME = props.platformName
                        if (props.browser && env.PLATFORM_NAME == 'Web') env.BROWSER = props.browser
                    }

                    echo """Configuration actuelle:
                    • Plateforme: ${env.PLATFORM_NAME}
                    • Navigateur: ${env.PLATFORM_NAME == 'Web' ? env.BROWSER : 'N/A'}"""

                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} target/screenshots

                        echo "=== Vérification de l'environnement ==="
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

                        def mvnCommand = "${M2_HOME}/bin/mvn test -Dtest=runner.TestRunner -DplatformName=${env.PLATFORM_NAME}"

                        if (env.PLATFORM_NAME == 'Web') {
                            mvnCommand += " -Dbrowser=${env.BROWSER}"
                        }

                        mvnCommand += """ \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn > test_output.log
                        """

                        sh mvnCommand
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        allure([
                            includeProperties: false,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        sh """
                            if [ -d "${ALLURE_RESULTS}" ]; then
                                cd target && zip -q -r allure-report.zip allure-results/
                            fi
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "${EXCEL_REPORTS}/**/*.xlsx, target/allure-report.zip", allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            script {
                def testResults = fileExists('test_output.log') ? readFile('test_output.log').trim() : "Aucun résultat disponible"

                echo """╔═══════════════════════════╗
║   Résumé de l'Exécution   ║
╚═══════════════════════════╝

📝 Rapports:
• Allure: ${BUILD_URL}allure/
• Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}/

Plateforme: ${env.PLATFORM_NAME}
${env.PLATFORM_NAME == 'Web' ? "Navigateur: ${env.BROWSER}" : ''}
${currentBuild.result == 'SUCCESS' ? '✅ SUCCÈS' : '❌ ÉCHEC'}"""
            }
            cleanWs notFailBuild: true
        }
    }
}