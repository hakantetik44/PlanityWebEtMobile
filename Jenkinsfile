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
        PDF_REPORTS = 'target/pdf-reports'
        CUCUMBER_REPORTS = 'target/cucumber-reports'
        VIDEO_DIR = "${PDF_REPORTS}/videos"
    }

    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'dev', 'feature/*', 'bugfix/*'],
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
            name: 'RECORD_VIDEO',
            defaultValue: true,
            description: 'Activer l\'enregistrement vidéo'
        )
    }

    stages {
        stage('Branch Selection') {
            steps {
                script {
                    sh "git fetch --all"
                    def branches = sh(
                        script: 'git branch -r | grep -v HEAD | sed "s/origin\\///"',
                        returnStdout: true
                    ).trim().split('\n')

                    echo "🌿 Available branches: ${branches.join(', ')}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        extensions: [],
                        userRemoteConfigs: [[url: 'https://github.com/hakantetik44/PlanityWebEtMobile.git']]
                    ])
                }
            }
        }

        stage('Initialisation') {
            steps {
                script {
                    echo """╔═══════════════════════════════╗
      ║ Démarrage de l'Automatisation ║
      ╚═══════════════════════════════╝"""

                    // Create directories and set permissions
                    sh """
                        mkdir -p ${PDF_REPORTS}/videos
                        mkdir -p ${ALLURE_RESULTS}
                        mkdir -p ${CUCUMBER_REPORTS}
                        mkdir -p target/screenshots
                        touch ${PDF_REPORTS}/ffmpeg.log
                        chmod -R 777 ${PDF_REPORTS}
                        chmod 777 ${PDF_REPORTS}/ffmpeg.log
                    """

                    // Check ffmpeg installation
                    sh """
                        if ! command -v ffmpeg &> /dev/null; then
                            brew install ffmpeg || apt-get install -y ffmpeg || yum install -y ffmpeg
                        fi
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

                        if (params.RECORD_VIDEO) {
                            echo "🎥 Démarrage de l'enregistrement vidéo..."
                            sh """
                                mkdir -p ${PDF_REPORTS}/videos
                                touch ${PDF_REPORTS}/ffmpeg.log

                                DISPLAY=:0 ffmpeg -y -f x11grab -video_size 1920x1080 -i :0.0 \
                                -codec:v libx264 -r 30 -pix_fmt yuv420p \
                                ${PDF_REPORTS}/videos/test_execution_${TIMESTAMP}.mp4 \
                                2>${PDF_REPORTS}/ffmpeg.log & \
                                echo \$! > ${PDF_REPORTS}/videos/recording.pid

                                sleep 2
                            """
                        }

                        sh """
                            ${M2_HOME}/bin/mvn test \
                            -Dtest=runner.TestRunner \
                            -DplatformName=${params.PLATFORM_NAME} \
                            -Dbrowser=${params.BROWSER} \
                            -DvideoDir=${PDF_REPORTS}/videos \
                            -DrecordVideo=${params.RECORD_VIDEO} \
                            -DscreenshotsDir=target/screenshots \
                            -Dcucumber.plugin="pretty,json:target/cucumber.json,html:${CUCUMBER_REPORTS},io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm" \
                            -Dallure.results.directory=${ALLURE_RESULTS}
                        """

                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    } finally {
                        if (params.RECORD_VIDEO) {
                            echo "🎥 Arrêt de l'enregistrement vidéo..."
                            sh """
                                if [ -f "${PDF_REPORTS}/videos/recording.pid" ]; then
                                    PID=\$(cat ${PDF_REPORTS}/videos/recording.pid)
                                    kill \$PID || true
                                    rm ${PDF_REPORTS}/videos/recording.pid
                                fi

                                sleep 2
                                if [ -f "${PDF_REPORTS}/videos/test_execution_${TIMESTAMP}.mp4" ]; then
                                    echo "✅ Vidéo enregistrée avec succès"
                                    ls -lh ${PDF_REPORTS}/videos/test_execution_${TIMESTAMP}.mp4
                                else
                                    echo "❌ Échec de l'enregistrement vidéo"
                                    cat ${PDF_REPORTS}/ffmpeg.log
                                fi
                            """
                        }
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

                        cucumber buildStatus: 'UNSTABLE',
                            reportTitle: '🌟 Planity Test Automation Report',
                            fileIncludePattern: '**/cucumber.json',
                            trendsLimit: 10,
                            classifications: [
                                ['key': '🌿 Branch', 'value': params.BRANCH_NAME],
                                ['key': '🚀 Platform', 'value': params.PLATFORM_NAME],
                                ['key': '🌐 Browser', 'value': params.BROWSER],
                                ['key': '🎥 Video', 'value': params.RECORD_VIDEO ? 'Enabled' : 'Disabled']
                            ]

                        sh """
                            cd target
                            zip -r test-results.zip allure-results cucumber-reports
                            mv test-results.zip ${PDF_REPORTS}
                        """
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        throw e
                    }
                }
            }
        }
    }
}
