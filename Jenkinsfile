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
        choice(name: 'PLATFORM_NAME', choices: ['Web', 'Android', 'iOS'], description: 'Test platformunu seçin')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'safari'], description: 'Tarayıcı seçin (sadece Web için)')
        booleanParam(name: 'RECORD_VIDEO', defaultValue: true, description: 'Video kaydını etkinleştir')
        booleanParam(name: 'PERFORMANCE_TEST', defaultValue: false, description: 'Performans testlerini çalıştır')
        choice(name: 'TEST_ENVIRONMENT', choices: ['DEV', 'QA', 'STAGING', 'PROD'], description: 'Test ortamı seçin')
        string(name: 'SLACK_CHANNEL', defaultValue: '#test-automation', description: 'Bildirimler için Slack kanalı')
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        ansiColor('xterm')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Başlatma') {
            steps {
                script {
                    // Klasörleri oluştur
                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} ${VIDEO_FOLDER} target/screenshots
                        mkdir -p ${PERFORMANCE_REPORTS} ${CODE_COVERAGE} ${TEST_LOGS}
                        export JAVA_HOME=${JAVA_HOME}
                        java -version
                        ${M2_HOME}/bin/mvn -version
                    """

                    // Test konfigürasyon dosyasını oluştur
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

        stage('Test Hazırlığı') {
            steps {
                script {
                    try {
                        echo "🔍 Ön koşulların kontrolü..."
                        sh """
                            df -h > ${TEST_LOGS}/disk-space.log || echo "Disk alanı kontrolü başarısız"
                            free -m > ${TEST_LOGS}/memory.log || echo "Bellek kontrolü başarısız"
                            find target -name "*.log" -mtime +7 -delete || echo "Eski loglar temizlenemedi"
                            find target -name "*.mp4" -mtime +7 -delete || echo "Eski videolar temizlenemedi"
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error "Hazırlık aşamasında hata: ${e.message}. Daha fazla bilgi için loglara bakın."
                    }
                }
            }
        }

        stage('Testlerin Çalıştırılması') {
            steps {
                script {
                    try {
                        echo "🧪 Testler başlatılıyor..."

                        // Video kaydını başlat
                        if (params.RECORD_VIDEO) {
                            sh """
                                ffmpeg -f x11grab -video_size 1920x1080 -i :0.0 -codec:v libx264 -r 30 \
                                ${VIDEO_FOLDER}/test-execution-${TIMESTAMP}.mp4 2>${TEST_LOGS}/video.log &
                                echo \$! > .recording.pid
                            """
                        }

                        def mvnCommand = """
                            ${M2_HOME}/bin/mvn verify \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -Denvironment=${params.TEST_ENVIRONMENT} \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DvideoFolder=${VIDEO_FOLDER} \
                            -DscreenshotFolder=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm,timeline:${CUCUMBER_REPORTS}/timeline" \
                            -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
                            -Dmaven.test.failure.ignore=true
                        """

                        // Performans testleri aktifse
                        if (params.PERFORMANCE_TEST) {
                            mvnCommand += " -Dgatling.enabled=true -Dgatling.resultsDirectory=${PERFORMANCE_REPORTS}"
                        }

                        sh mvnCommand

                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Test yürütme sırasında hata oluştu: ${e.message}"
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

        stage('Sonuçların Analizi') {
            steps {
                script {
                    try {
                        echo "📊 Sonuçlar analiz ediliyor..."

                        // Raporu oluştur
                        sh """
                            echo "Test Özeti Raporu - ${TIMESTAMP}" > ${TEST_LOGS}/analysis.md
                            echo "===========================" >> ${TEST_LOGS}/analysis.md
                            grep -r "FAILED" target/surefire-reports/*.txt | tee ${TEST_LOGS}/failures.log
                            echo "Test Süresi: \$(grep 'Tests run' target/surefire-reports/*.txt | tail -1)" >> ${TEST_LOGS}/analysis.md
                            echo "Başarısızlık Oranı: \$(grep -c 'FAILED' target/surefire-reports/*.txt)%" >> ${TEST_LOGS}/analysis.md
                            cd target
                            zip -q -r test-results-${TIMESTAMP}.zip allure-results/ cucumber-reports/ screenshots/ videos/ test-logs/ performance/
                        """

                    } catch (Exception e) {
                        echo "⚠️ Analiz sırasında hata oluştu: ${e.message}"
                    }
                }
            }
        }

        stage('Raporlar') {
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
                            reportTitle: 'Planity Test Otomasyon Raporu',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': 'Platform', 'value': params.PLATFORM_NAME],
                                ['key': 'Tarayıcı', 'value': params.BROWSER],
                                ['key': 'Ortam', 'value': params.TEST_ENVIRONMENT],
                                ['key': 'Build', 'value': BUILD_NUMBER],
                                ['key': 'Test Tarihi', 'value': TIMESTAMP]
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
                        echo "⚠️ Rapor oluşturma sırasında hata: ${e.message}"
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

                // Test detayları
                def testDetails = """
                    🏗️ Build: #${BUILD_NUMBER}
                    ⏱️ Süre: ${duration}
                    🌍 Ortam: ${params.TEST_ENVIRONMENT}
                    📱 Platform: ${params.PLATFORM_NAME}
                    🌐 Tarayıcı: ${params.BROWSER}
                """

                echo """╔════════════════════════════╗
║    Çalışma Sonucu: ${status}     ║
╚════════════════════════════╝
                """

                // Slack bildirimini gönder
                slackSend(channel: params.SLACK_CHANNEL, message: testDetails)
            }
        }
    }
}
