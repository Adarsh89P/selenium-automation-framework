// Declarative pipeline mirroring .github/workflows/selenium.yml.
//
// The two exist because interviews and workplaces split roughly evenly between them, and because
// they answer different questions: the GitHub workflow is the always-on gate, this is the
// parameterised run someone triggers by hand when they want a specific slice - one browser, one
// tag, one environment - without editing a file or pushing a commit.
//
// Everything that differs between runs is a parameter. Nothing below hardcodes a browser, a tag
// expression, an environment or a grid URL.

pipeline {

    // TODO: 'any' assumes the agent has a JDK, Maven and a browser. Pin this to a label that
    // actually has them - agent { label 'selenium' } - once the agent inventory is known. Left
    // broad on purpose rather than guessing at a label that may not exist.
    agent any

    tools {
        // TODO: these are Jenkins *tool installation* names configured under Manage Jenkins >
        // Tools, not version strings. They will not resolve until an admin has defined them with
        // exactly these names.
        jdk 'jdk-25'
        maven 'maven-3.9'
    }

    parameters {
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge', 'all'],
            description: 'Browser to run against. "all" ignores this value and uses ' +
                         'parallel-grid.xml, which runs all three concurrently - that requires ' +
                         'EXECUTION=grid.')
        choice(
            name: 'EXECUTION',
            choices: ['local', 'grid'],
            description: 'local drives a browser on the agent. grid stands up the Dockerised ' +
                         'Selenium Grid from docker-compose.yml and drives it remotely.')
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'stage'],
            description: 'Selects config/<env>.properties.')
        string(
            name: 'TAGS',
            defaultValue: '@smoke and not @wip',
            description: 'Cucumber tag expression. Examples: "@smoke and not @wip", ' +
                         '"@regression and @leave", "@advanced".')
        string(
            name: 'THREADS',
            defaultValue: '4',
            description: 'Scenarios in flight at once. Keep this at or below the number of ' +
                         'available grid nodes: over-subscribing makes scenarios queue at the ' +
                         'hub, and the resulting timeouts read as application slowness.')
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: 'Uncheck only on an agent with a display. A headed run on a headless ' +
                         'agent fails at session creation, not at an assertion.')
    }

    options {
        timestamps()
        // A hung browser session holds an executor indefinitely; this bounds it.
        timeout(time: 90, unit: 'MINUTES')
        // Two runs of this job would fight over the same host ports (4444) and the same compose
        // project name.
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        skipDefaultCheckout(false)
    }

    triggers {
        // 08:00 IST. H spreads the load across the hour so every job on the controller does not
        // fire on the same minute.
        cron('H 2 * * *')
    }

    environment {
        MAVEN_ARGS = '-B -ntp'
        // TODO: create this credential (Manage Jenkins > Credentials) as a username/password pair
        // with id 'aut-app-credentials'. Owner reads APP_USERNAME / APP_PASSWORD from the
        // environment ahead of any properties file, so nothing has to be committed.
        APP_CREDENTIALS = credentials('aut-app-credentials')
        APP_USERNAME = "${APP_CREDENTIALS_USR}"
        APP_PASSWORD = "${APP_CREDENTIALS_PSW}"
    }

    stages {

        stage('Validate parameters') {
            steps {
                script {
                    // Fail in five seconds on a combination that cannot work, rather than after
                    // the grid has been pulled and the first scenarios have timed out.
                    if (params.BROWSER == 'all' && params.EXECUTION != 'grid') {
                        error("BROWSER=all needs EXECUTION=grid: three browsers cannot run " +
                              "concurrently on one agent.")
                    }
                }
                echo "browser=${params.BROWSER} execution=${params.EXECUTION} " +
                     "env=${params.ENVIRONMENT} tags='${params.TAGS}' threads=${params.THREADS}"
            }
        }

        stage('Start Selenium Grid') {
            when { expression { params.EXECUTION == 'grid' } }
            steps {
                // --wait blocks until the hub is healthy and the nodes have registered. Without
                // it the first scenarios fail with "Could not start a new session" in the window
                // where a node answers its own health check but has not yet reached the hub.
                sh 'docker compose up -d --wait'
                sh 'curl -s http://localhost:4444/status'
            }
        }

        stage('Test') {
            steps {
                script {
                    // The suite file, not the browser, is what changes between these two paths:
                    // parallel-grid.xml carries one <test> block per browser and
                    // BrowserParameterListener pins each block to its own browser at runtime.
                    def suite = params.BROWSER == 'all'
                            ? 'src/test/resources/suites/parallel-grid.xml'
                            : 'src/test/resources/suites/testng-regression.xml'

                    def browserArg = params.BROWSER == 'all'
                            ? ''
                            : "-Dbrowser=${params.BROWSER}"

                    sh """
                        mvn ${MAVEN_ARGS} test \
                          -Dsurefire.suiteXmlFiles=${suite} \
                          ${browserArg} \
                          -Dexecution=${params.EXECUTION} \
                          -Denv=${params.ENVIRONMENT} \
                          -Dheadless=${params.HEADLESS} \
                          -Dcucumber.tags="${params.TAGS}" \
                          -Ddataproviderthreadcount=${params.THREADS}
                    """
                }
            }
        }
    }

    post {

        always {
            // Everything below runs whether the suite passed, failed or was aborted. A failed run
            // that publishes nothing is the worst outcome: the evidence exists on the agent and
            // is about to be deleted with the workspace.

            script {
                if (params.EXECUTION == 'grid') {
                    sh 'mkdir -p target/grid-logs'
                    sh 'docker compose logs --no-color > target/grid-logs/compose.log 2>&1 || true'
                    sh 'docker compose down -v || true'
                }
            }

            // allowEmptyResults: an aborted run may have produced no XML at all, and that should
            // not turn an abort into a separate reporting failure.
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            // TODO: needs the Allure Jenkins plugin, plus an Allure commandline installation
            // configured under Manage Jenkins > Tools. Jenkins keeps the history itself, so
            // unlike the GitHub workflow there is no gh-pages branch to carry forward.
            allure([
                results: [[path: 'target/allure-results']],
                reportBuildPolicy: 'ALWAYS'
            ])

            archiveArtifacts(
                artifacts: 'target/allure-results/**, target/logs/**, target/grid-logs/**, ' +
                           'target/videos/**, target/flaky-report.txt, target/cucumber-report.html',
                allowEmptyArchive: true,
                fingerprint: false)
        }

        unstable {
            // Jenkins marks a build unstable when tests failed but the build itself did not break.
            // Worth saying out loud, because a retried-then-passed scenario is recorded by TestNG
            // as skipped, and Jenkins renders that as a skip rather than as flakiness - which is
            // exactly what target/flaky-report.txt is for.
            echo 'Tests failed. Check target/flaky-report.txt before assuming they are all real.'
        }

        cleanup {
            // Last, and only after the archive above has taken what it needs.
            cleanWs()
        }
    }
}
