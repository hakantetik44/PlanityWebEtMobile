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
           CUCUMBER_REPORTS = 'target/cucumber-reports'
           CUCUMBER_JSON = "${WORKSPACE_DIR}/target/cucumber.json"
           CUCUMBER_PUBLISH_ENABLED = 'true'
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
    }

   stages {
           stage('Initialization') {
               steps {
                   script {
                       cleanWs()
                       checkout scm

                       sh """
                           mkdir -p ${ALLURE_RESULTS}
                           mkdir -p ${CUCUMBER_REPORTS}
                           mkdir -p target/screenshots
                           echo '[]' > target/cucumber.json
                           chmod 777 target/cucumber.json
                       """
                   }
               }
           }

           stage('Build & Test') {
               steps {
                   script {
                       try {
                           sh """
                               ${M2_HOME}/bin/mvn clean test \
                               -Dtest=runner.TestRunner \
                               -DplatformName=${params.PLATFORM_NAME} \
                               -Dbrowser=${params.BROWSER} \
                               -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS}" \
                               -Dallure.results.directory=${ALLURE_RESULTS}
                           """

                           // JSON dosyasını kontrol et
                           if (!fileExists('target/cucumber.json')) {
                               error "Cucumber JSON dosyası oluşturulamadı"
                           }

                           // JSON içeriğini kontrol et
                           def jsonContent = readFile('target/cucumber.json').trim()
                           if (jsonContent.isEmpty()) {
                               error "Cucumber JSON dosyası boş"
                           }
                       } catch (Exception e) {
                           currentBuild.result = 'FAILURE'
                           error "Test execution failed: ${e.message}"
                       }
                   }
               }
               post {
                   always {
                       junit '**/target/surefire-reports/*.xml'
                   }
               }
           }

           stage('Reports') {
               steps {
                   script {
                       try {
                           // Allure Report
                           allure([
                               includeProperties: true,
                               reportBuildPolicy: 'ALWAYS',
                               results: [[path: "${ALLURE_RESULTS}"]]
                           ])

                           // JSON dosyası kontrolü
                           def jsonFile = new File("${WORKSPACE}/target/cucumber.json")
                           if (jsonFile.exists() && jsonFile.length() > 0) {
                               // Cucumber Report
                               cucumber([
                                   fileIncludePattern: "target/cucumber.json",
                                   jsonReportDirectory: "${WORKSPACE}/target",
                                   reportTitle: '🌟 Planity Test Report',
                                   buildStatus: 'UNSTABLE',
                                   classifications: [
                                       [key: '🌿 Branch', value: params.BRANCH_NAME],
                                       [key: '📱 Platform', value: params.PLATFORM_NAME],
                                       [key: '🌐 Browser', value: params.BROWSER],
                                       [key: '📅 Date', value: new Date().format('dd/MM/yyyy HH:mm')]
                                   ]
                               ])
                           } else {
                               error "Valid cucumber.json file not found"
                           }

                           // Archive
                           archiveArtifacts(
                               artifacts: """
                                   target/cucumber.json,
                                   target/surefire-reports/**/*,
                                   target/screenshots/**/*
                               """,
                               allowEmptyArchive: true
                           )
                       } catch (Exception e) {
                           echo "⚠️ Report generation warning: ${e.message}"
                           currentBuild.result = 'UNSTABLE'
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

                   echo """╔═══════════════════════════════════════════╗
   ║             Résumé d'Exécution              ║
   ╚═══════════════════════════════════════════╝

   🎯 Build: #${BUILD_NUMBER}
   🌿 Branch: ${params.BRANCH_NAME}
   🕒 Durée: ${currentBuild.durationString}
   📱 Platform: ${params.PLATFORM_NAME}
   🌐 Browser: ${params.BROWSER}

   📊 Rapports:
   🔹 Allure:    ${BUILD_URL}allure/
   🔹 Cucumber:  ${BUILD_URL}cucumber-html-reports/
   🔹 Artifacts: ${BUILD_URL}artifact/

   ${emoji} Status Final: ${status}"""
               }
               cleanWs()
           }
       }
   }}