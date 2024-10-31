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

                        env.PLATFORM_NAME = props.platformName ?: params.PLATFORM_NAME ?: 'Web'
                        env.BROWSER = env.PLATFORM_NAME == 'Web' ? (props.browser ?: params.BROWSER ?: 'chrome') : ''
                    } else {
                        env.PLATFORM_NAME = params.PLATFORM_NAME ?: 'Web'
                        env.BROWSER = env.PLATFORM_NAME == 'Web' ? params.BROWSER ?: 'chrome' : ''
                    }

                    writeFile file: 'target/allure-results/environment.properties', text: """
                        Platform=${env.PLATFORM_NAME}
                        Browser=${env.BROWSER}
                        Test Framework=Cucumber
                        Language=FR
                    """.stripIndent()

                    echo """Configuration:
                    • Plateforme: ${env.PLATFORM_NAME}
                    • Navigateur: ${env.PLATFORM_NAME == 'Web' ? env.BROWSER : 'N/A'}"""

                    sh """
                        mkdir -p ${EXCEL_REPORTS} ${ALLURE_RESULTS} target/screenshots
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
                            includeProperties: true,
                            reportBuildPolicy: 'ALWAYS',
                            results: [[path: "${ALLURE_RESULTS}"]]
                        ])

                        // Excel raporu oluşturma işlemi
                        createExcelReport()

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

// Excel raporunu oluşturmak için gerekli fonksiyon
def createExcelReport() {
    script {
        // Apache POI veya benzeri bir kütüphane kullanarak Excel dosyasını oluşturun
        def workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()
        def sheet = workbook.createSheet('Rapports de Test')

        // Başlık satırını oluştur
        def header = sheet.createRow(0)
        header.createCell(0).setCellValue('Test Case')
        header.createCell(1).setCellValue('Dure (süre)')
        header.createCell(2).setCellValue('Etape (adım)')

        // Test sonuçlarını oku ve Excel'e yaz
        def testOutput = readFile('test_output.log')
        def testResults = parseTestResults(testOutput) // Parse fonksiyonu

        int rowNum = 1
        testResults.each { result ->
            def row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(result.testCase)
            row.createCell(1).setCellValue(result.duration) // Test süresi
            row.createCell(2).setCellValue(result.step) // Test adımı
        }

        // Excel dosyasını kaydet
        def fileOut = new FileOutputStream("${EXCEL_REPORTS}/Rapport_de_Test_${env.TIMESTAMP}.xlsx")
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}

// Test sonuçlarını parse eden fonksiyon
def parseTestResults(testOutput) {
    def results = []

    // Test çıktılarını satır satır inceleyin
    testOutput.eachLine { line ->
        // Burada satırları kontrol ederek gerekli bilgileri alın
        if (line.contains('Finished:')) {
            def parts = line.split(' ')
            def testCase = parts[1]  // Test case adını alın
            def duration = parts[2]   // Süreyi alın (örneğin, 10s)
            def step = 'Adım 1'  // Adım ismini burada doldurun (gerektiği şekilde özelleştirin)

            results << [testCase: testCase, duration: duration, step: step]
        }
    }

    return results
}
