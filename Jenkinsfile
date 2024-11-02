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
        CODE_COVERAGE = 'target/coverage'
        TEST_LOGS = 'target/test-logs'
    }

    parameters {
        choice(name: 'PLATFORM_NAME', choices: ['Web', 'Android', 'iOS'], description: 'Sélectionnez la plateforme de test')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'safari'], description: 'Sélectionnez le navigateur (pour Web uniquement)')
        booleanParam(name: 'RECORD_VIDEO', defaultValue: true, description: 'Activer l\'enregistrement vidéo')
        booleanParam(name: 'PERFORMANCE_TEST', defaultValue: false, description: 'Exécuter les tests de performance')
        choice(name: 'TEST_ENVIRONMENT', choices: ['DEV', 'QA', 'STAGING', 'PROD'], description: 'Environnement de test')
        string(name: 'SLACK_CHANNEL', defaultValue: '#test-automation', description: 'Canal Slack pour les notifications')
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        ansiColor('xterm')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Initialisation') {
            steps {
                script {
                    // Test metrikleri için klasörler oluştur
                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} ${VIDEO_FOLDER} target/screenshots
                        mkdir -p ${PERFORMANCE_REPORTS} ${CODE_COVERAGE} ${TEST_LOGS}
                        export JAVA_HOME=${JAVA_HOME}
                        java -version
                        ${M2_HOME}/bin/mvn -version
                    """

                    // Test konfigürasyon dosyası oluştur
                    writeFile file: 'target/test-config.json', text: """
                        {
                            "platform": "${params.PLATFORM_NAME}",
                            "browser": "${params.BROWSER}",
                            "environment": "${params.TEST_ENVIRONMENT}",
                            "buildNumber": "${BUILD_NUMBER}",
                            "timestamp": "${TIMESTAMP}",
                            "recordVideo": ${params.RECORD_VIDEO},
                            "performanceTest": ${params.PERFORMANCE_TEST}
                        }
                    """
                }
            }
        }

        stage('Tests Préparation') {
            steps {
                script {
                    try {
                        echo "🔍 Vérification des prérequis..."
                        sh """
                            # Vérifier l'espace disque
                            df -h > ${TEST_LOGS}/disk-space.log

                            # Vérifier la mémoire
                            free -m > ${TEST_LOGS}/memory.log

                            # Nettoyer les anciens rapports
                            find target -name "*.log" -mtime +7 -delete
                            find target -name "*.mp4" -mtime +7 -delete
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "Erreur lors de la préparation: ${e.message}"
                    }
                }
            }
        }

        stage('Exécution des Tests') {
            steps {
                script {
                    try {
                        echo "🧪 Lancement des tests..."

                        // Démarrer la capture vidéo
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -f x11grab -video_size 1920x1080 -i :0.0 -codec:v libx264 -r 30 \
                                ${VIDEO_FOLDER}/test-execution-${TIMESTAMP}.mp4 2>${TEST_LOGS}/video.log &
                                echo \$! > .recording.pid
                            """
                        }

                        // Maven komutunu tanımla
                        def mvnCommand = """
                            ${M2_HOME}/bin/mvn verify -e -X \  # Hata ayıklama için -e ve -X parametreleri eklendi
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -Denvironment=${params.TEST_ENVIRONMENT} \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DvideoFolder=${VIDEO_FOLDER} \
                            -DscreenshotFolder=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm,timeline:${CUCUMBER_REPORTS}/timeline" \
                            -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
                            -Dmaven.test.failure.ignore=true \
                        """

                        // Eğer performans testleri aktifse
                        if (params.PERFORMANCE_TEST) {
                            mvnCommand += " -Dgatling.enabled=true -Dgatling.resultsDirectory=${PERFORMANCE_REPORTS}"
                        }

                        // Maven komutunu çalıştır
                        sh mvnCommand

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Des erreurs sont survenues pendant l'exécution: ${e.message}"
                    } finally {
                        if (params.RECORD_VIDEO) {
                            sh """
                                if [ -f .recording.pid ]; then
                                    kill \$(cat .recording.pid)
                                    rm .recording.pid
                                fi
                            """
                        }
                    }
                }
            }
        }

        stage('Analyse des Résultats') {
            steps {
                script {
                    try {
                        echo "📊 Analyse des résultats..."

                        // Detaylı raporları oluştur
                        sh """
                            echo "Test Summary Report - ${TIMESTAMP}" > ${TEST_LOGS}/analysis.md
                            echo "===========================" >> ${TEST_LOGS}/analysis.md

                            # Test loglarını analiz et
                            grep -r "FAILED" target/surefire-reports/*.txt | tee ${TEST_LOGS}/failures.log

                            # Metrikleri hesapla
                            echo "Test Duration: \$(grep 'Tests run' target/surefire-reports/*.txt | tail -1)" >> ${TEST_LOGS}/analysis.md
                            echo "Failure Rate: \$(grep -c 'FAILED' target/surefire-reports/*.txt)%" >> ${TEST_LOGS}/analysis.md

                            # Arşiv oluştur
                            cd target
                            zip -q -r test-results-${TIMESTAMP}.zip \
                                allure-results/ \
                                cucumber-reports/ \
                                screenshots/ \
                                videos/ \
                                test-logs/ \
                                performance/
                        """

                    } catch (Exception e) {
                        echo "⚠️ Erreur lors de l'analyse: ${e.message}"
                    }
                }
            }
        }

        stage('Rapports') {
            steps {
                script {
                    try {
                        // Allure Raporları
                        allure([
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Cucumber Raporları
                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: 'Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': 'Platform', 'value': params.PLATFORM_NAME],
                                ['key': 'Browser', 'value': params.BROWSER],
                                ['key': 'Environment', 'value': params.TEST_ENVIRONMENT],
                                ['key': 'Build', 'value': BUILD_NUMBER],
                                ['key': 'Test Date', 'value': TIMESTAMP]
                            ]

                        // Tüm raporları arşivle
                        archiveArtifacts artifacts: """
                            ${EXCEL_REPORTS}/**/*.xlsx,
                            target/allure-report.zip,
                            target/cucumber-reports.zip,
                            target/cucumber.json,
                            ${VIDEO_FOLDER}/**/*.mp4,
                            target/screenshots/**/*.png,
                            ${TEST_LOGS}/**/*,
                            ${PERFORMANCE_REPORTS}/**/*,
                            target/test-results-${TIMESTAMP}.zip
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

                // Test detayları için rapor
                def testDetails = """
                    🏗️ Build: #${BUILD_NUMBER}
                    ⏱️ Durée: ${duration}
                    🌍 Environnement: ${params.TEST_ENVIRONMENT}
                    🖥️ Plateforme: ${params.PLATFORM_NAME}
                    🧪 Navigateur: ${params.BROWSER}
                    📈 État: ${status}
                """

                // Slack'e bildirim gönder
                slackSend(channel: params.SLACK_CHANNEL, message: testDetails)
            }
        }
    }
}
