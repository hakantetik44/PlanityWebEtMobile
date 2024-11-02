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
        VIDEO_DIR = "${EXCEL_REPORTS}/videos"
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

                    sh """
                        mkdir -p ${VIDEO_DIR} ${ALLURE_RESULTS} ${CUCUMBER_REPORTS}
                        mkdir -p target/screenshots
                        chmod -R 777 ${VIDEO_DIR}
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

        stage('Exécution des Tests') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        // Démarre l'enregistrement vidéo
                        sh """
                            ffmpeg -f x11grab -video_size 1920x1080 -i :0.0 \
                            -codec:v libx264 -r 30 -pix_fmt yuv420p \
                            ${VIDEO_DIR}/test_execution_${BUILD_NUMBER}.mp4 \
                            2>${EXCEL_REPORTS}/ffmpeg.log & echo \$! > ${VIDEO_DIR}/recording.pid
                        """

                        // Exécute les tests
                        sh """
                            ${M2_HOME}/bin/mvn test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DvideoDir=${VIDEO_DIR} \
                            -DscreenshotsDir=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """

                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    } finally {
                        sh """
                            if [ -f "${VIDEO_DIR}/recording.pid" ]; then
                                kill \$(cat ${VIDEO_DIR}/recording.pid) || true
                                rm ${VIDEO_DIR}/recording.pid
                            fi
                        """
                    }
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        // Crée les archives
                        sh """
                            cd target
                            zip -r test-results-${BUILD_NUMBER}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                ${EXCEL_REPORTS}/videos/
                        """

                        // Génère les rapports
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: '🌟 Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': '🚀 Platform', 'value': params.PLATFORM_NAME],
                                ['key': '🌐 Browser', 'value': params.BROWSER],
                                ['key': '🎥 Video', 'value': 'Available']
                            ]

                        // Archive les artifacts
                        archiveArtifacts artifacts: """
                            ${EXCEL_REPORTS}/**/*.xlsx,
                            ${VIDEO_DIR}/**/*.mp4,
                            target/test-results-${BUILD_NUMBER}.zip,
                            target/cucumber.json
                        """, allowEmptyArchive: true

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Erreur rapports: ${e.message}"
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

                echo """╔═══════════════════════════════════════════╗
║             Résumé d'Exécution              ║
╚═══════════════════════════════════════════╝

🎯 Build: #${BUILD_NUMBER}
🕒 Durée: ${currentBuild.durationString}
📱 Plateforme: ${params.PLATFORM_NAME}
🌐 Navigateur: ${params.BROWSER}

📊 Rapports:
🔹 Allure:    ${BUILD_URL}allure/
🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/
🔹 Video:     ${BUILD_URL}artifact/${VIDEO_DIR}/
🔹 Excel:     ${BUILD_URL}artifact/${EXCEL_REPORTS}/

${statusEmoji} Statut Final: ${status}
"""

                // Nettoie l'espace de travail
                sh """
                    find . -type f -name '*.tmp' -delete
                    find . -type f -name '*.log' -mtime +7 -delete
                """
            }
        }
    }
}