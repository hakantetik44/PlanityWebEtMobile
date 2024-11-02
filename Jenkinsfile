pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }

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
        VIDEO_DIR = "target/videos"
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
            name: 'VIDEO_RECORDING',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo des tests'
        )
    }

    triggers {
        cron('0 0 * * *')
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    def description = """
                    <h2>🤖 ${PROJECT_NAME}</h2>
                    <p><b>🔄 Build:</b> #${env.BUILD_NUMBER}</p>
                    <p><b>📱 Plateforme:</b> ${params.PLATFORM_NAME}</p>
                    <p><b>🌐 Navigateur:</b> ${params.PLATFORM_NAME == 'Web' ? params.BROWSER : 'N/A'}</p>
                    <p><b>📅 Date d'exécution:</b> ${TIMESTAMP}</p>
                    <hr/>
                    """

                    currentBuild.description = description
                    cleanWs()
                    checkout scm

                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} target/screenshots ${VIDEO_DIR}
                        echo "Test execution started at ${TIMESTAMP}" > execution.log
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
                        sh "${M2_HOME}/bin/mvn clean install -DskipTests -B"
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }

        stage('Tests') {
            steps {
                script {
                    try {
                        def mvnCommand = """
                            ${M2_HOME}/bin/mvn test
                            -Dtest=runner.TestRunner
                            -DplatformName=${params.PLATFORM_NAME}
                            ${params.PLATFORM_NAME == 'Web' ? "-Dbrowser=${params.BROWSER}" : ''}
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
                        """

                        sh mvnCommand
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
            post {
                always {
                    cucumber(
                        buildStatus: 'UNSTABLE',
                        reportTitle: "${PROJECT_NAME} - Build #${env.BUILD_NUMBER}",
                        fileIncludePattern: '**/cucumber.json',
                        trendsLimit: 10,
                        classifications: [
                            [
                                'key': 'Platform',
                                'value': params.PLATFORM_NAME
                            ],
                            [
                                'key': 'Browser',
                                'value': params.PLATFORM_NAME == 'Web' ? params.BROWSER : 'N/A'
                            ]
                        ]
                    )
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        sh """
                            if [ -d "${ALLURE_RESULTS}" ]; then
                                cd target && zip -r allure-report.zip allure-results/
                            fi
                            if [ -d "${CUCUMBER_REPORTS}" ]; then
                                cd target && zip -r cucumber-reports.zip cucumber-reports/
                            fi
                        """

                        def reportIndex = """
                        <html>
                            <head>
                                <title>Rapports de Test - Build #${env.BUILD_NUMBER}</title>
                                <style>
                                    body { font-family: Arial, sans-serif; margin: 20px; }
                                    table { border-collapse: collapse; width: 100%; }
                                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                                    th { background-color: #f2f2f2; }
                                </style>
                            </head>
                            <body>
                                <h1>🗂️ Rapports de Test - Build #${env.BUILD_NUMBER}</h1>
                                <table>
                                    <tr>
                                        <th>Type de Rapport</th>
                                        <th>Description</th>
                                        <th>Lien</th>
                                    </tr>
                                    <tr>
                                        <td>Allure Report</td>
                                        <td>Rapport détaillé avec screenshots et logs</td>
                                        <td><a href="../allure">Voir le rapport</a></td>
                                    </tr>
                                    <tr>
                                        <td>Cucumber Report</td>
                                        <td>Rapport BDD avec statistiques</td>
                                        <td><a href="../cucumber-html-reports/overview-features.html">Voir le rapport</a></td>
                                    </tr>
                                    <tr>
                                        <td>Excel Report</td>
                                        <td>Rapport détaillé au format Excel</td>
                                        <td><a href="artifact/${EXCEL_REPORTS}">Télécharger</a></td>
                                    </tr>
                                </table>
                                <p><i>Généré le ${TIMESTAMP}</i></p>
                            </body>
                        </html>
                        """
                        writeFile file: 'target/report-index.html', text: reportIndex
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: """
                        target/allure-report.zip,
                        target/cucumber-reports.zip,
                        target/cucumber.json,
                        target/report-index.html,
                        ${EXCEL_REPORTS}/**/*.xlsx
                    """, fingerprint: true
                }
            }
        }
    }

    post {
        always {
            script {
                def testSummary = """
                ╔═══════════════════════════╗
                ║   Résumé de l'Exécution   ║
                ╚═══════════════════════════╝

                🏗️ Build: #${env.BUILD_NUMBER}
                📝 Rapports:
                • Allure: ${BUILD_URL}allure
                • Cucumber: ${BUILD_URL}cucumber-html-reports/overview-features.html
                • Excel: ${BUILD_URL}artifact/${EXCEL_REPORTS}
                • Index: ${BUILD_URL}artifact/target/report-index.html

                📱 Plateforme: ${params.PLATFORM_NAME}
                ${params.PLATFORM_NAME == 'Web' ? "🌐 Navigateur: ${params.BROWSER}" : ''}

                ${currentBuild.result == 'SUCCESS' ? '✅ SUCCÈS' : '❌ ÉCHEC'}
                """

                echo testSummary

                def updatedDescription = currentBuild.description + """
                <hr/>
                <p><b>Statut:</b> ${currentBuild.result == 'SUCCESS' ? '✅ SUCCÈS' : '❌ ÉCHEC'}</p>
                <p><b>Durée:</b> ${currentBuild.durationString}</p>
                """

                currentBuild.description = updatedDescription
            }
            cleanWs notFailBuild: true
        }
    }
}