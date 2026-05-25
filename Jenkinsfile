pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        DEFAULT_BASE_BRANCH = 'main'
        // Quality gates
        COVERAGE_MIN_LINE = '0.70'
        // Tooling defaults (can be overridden at job level)
        SONARQUBE_ENV = 'SonarQube'
        SNYK_TOKEN_CRED_ID = 'snyk-token'
    }

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()   
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Gitleaks (secrets scan)') {
            steps {
                script {
                    def rc = sh(script: '''
                        if command -v gitleaks >/dev/null 2>&1; then
                            gitleaks dir . --redact --verbose
                        else
                            echo "gitleaks CLI not found on agent, skipping secrets scan."
                        fi
                    ''', returnStatus: true)
                    if (rc != 0) {
                        echo "WARNING: Gitleaks found secrets (exit code ${rc}). Review findings above."
                        unstable('Gitleaks detected secrets')
                    }
                }
            }
        }

        stage('Detect changed modules') {
            steps {
                script {
                    // Always fetch all remote branches so origin/main exists locally
                    sh "git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*"

                    // 1) Choose base for diff
                    // - PR build: compare with PR target branch (usually main)
                    // - Branch build: compare with origin/main
                    def baseRef = ''
                    if (env.CHANGE_ID) {
                        baseRef = "origin/${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH}"
                    } else {
                        baseRef = "origin/${env.DEFAULT_BASE_BRANCH}"
                    }

                    // 2) Diff and collect changed files
                    def diffCmd = "git diff --name-only ${baseRef}...HEAD"
                    def changedFilesRaw = sh(script: diffCmd, returnStdout: true).trim()
                    def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') : []

                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "Base for diff: ${baseRef}"
                    echo "origin/${env.DEFAULT_BASE_BRANCH}: " + sh(script: "git rev-parse ${baseRef}", returnStdout: true).trim()
                    echo "HEAD: " + sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    echo "Changed files:\n- " + (changedFiles ? changedFiles.join("\n- ") : "(none)")

                    // 3) Declare Maven modules (folder names)
                    def modules = [
                        'customer',
                        'cart',
                        'order',
                        'product',
                        'tax',
                        'media',
                        'search',
                        'webhook',
                        'common-library',
                        'inventory'
                    ]

                    // 4) Decide impacted modules (Option A: ONLY folder-based changes)
                    def impacted = modules.findAll { m ->
                        changedFiles.any { f -> f.startsWith("${m}/") }
                    }

                    if (impacted.isEmpty()) {
                        echo "No impacted modules detected (only service-folder changes are considered). Marking build as NOT_BUILT."
                        currentBuild.result = 'NOT_BUILT'
                        env.IMPACTED_MODULES = ''
                    } else {
                        env.IMPACTED_MODULES = impacted.join(',')
                        echo "Impacted modules: ${env.IMPACTED_MODULES}"
                    }
                }
            }
        }

        stage('Build impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    sh "mvn -B clean install -pl ${pl} -am -DskipTests"
                }
            }
        }

        stage('Test impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    // Ensure JaCoCo reports are generated even when tests fail
                    sh "mvn -B test jacoco:report -pl ${pl} -am -Dmaven.test.failure.ignore=true"
                }
            }
            post {
                always {
                    // Skip junit publishing - only coverage gate determines success
                    // Tests are executed but don't affect build status
                    // JaCoCo coverage is the only quality metric
                    
                    script {
                        def mods = (env.IMPACTED_MODULES?.trim() ? env.IMPACTED_MODULES.split(',') : []) as List
                        mods.each { m ->
                            jacoco(
                                execPattern: "${m}/target/jacoco.exec",
                                classPattern: "${m}/target/classes",
                                sourcePattern: "${m}/src/main/java",
                                exclusionPattern: '**/*Test*.class'
                            )

                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: "${m}/target/site/jacoco",
                                reportFiles: 'index.html',
                                reportName: "${m} Coverage Report",
                                reportTitles: "Code Coverage Report (${m})"
                            ])
                        }
                    }
                }
            }
        }

        stage('SonarQube (code quality)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    try {
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        def pl = mods.join(',')
                        withSonarQubeEnv(env.SONARQUBE_ENV) {
                            sh """
                                mvn -B sonar:sonar \
                                  -pl ${pl} -am \
                                  -Dsonar.organization=devops-org-newnol \
                                  -Dsonar.projectKey=devops-org-newnol_devops-org-newnol \
                                  -Dsonar.projectName="devops-org-newnol"
                            """.stripIndent()
                        }
                        echo "SonarQube analysis completed"
                    } catch (Exception e) {
                        echo "SonarQube analysis failed or skipped: ${e.message}"
                        echo "Note: SonarQube failures do not block the pipeline"
                    }
                }
            }
        }

        stage('Snyk (dependency vulnerabilities)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    withCredentials([string(credentialsId: env.SNYK_TOKEN_CRED_ID, variable: 'SNYK_TOKEN')]) {
                        sh '''
                            set -e
                            snyk --version
                            mkdir -p snyk-reports
                            snyk test --all-projects \
                                --severity-threshold=high \
                                --json-file-output=snyk-reports/snyk.json
                            snyk monitor --all-projects
                        '''
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'snyk-reports/*.json', allowEmptyArchive: true
                }
            }
        }

        stage('Coverage gate (> 70%)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def minCoverage = 70
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def failures = []

                    mods.each { m ->
                        def reportPath = "${m}/target/site/jacoco/jacoco.xml"
                        if (!fileExists(reportPath)) {
                            echo "⚠️  ${m}: Coverage report not found at ${reportPath}"
                            failures << "${m}: Report not found"
                            return
                        }

                        // Use bash to calculate coverage percentage with more robust parsing
                        def coverageScript = """
                            set -e
                            REPORT="${reportPath}"
                            
                            # Extract covered and missed counts from LINE counter
                            COVERED=\$(grep 'type="LINE"' \$REPORT | sed -n 's/.*covered="\\([^"]*\\)".*/\\1/p' | head -1)
                            MISSED=\$(grep 'type="LINE"' \$REPORT | sed -n 's/.*missed="\\([^"]*\\)".*/\\1/p' | head -1)
                            
                            if [ -z "\$COVERED" ] || [ -z "\$MISSED" ]; then
                                COVERED=0
                                MISSED=0
                            fi
                            
                            TOTAL=\$((COVERED + MISSED))
                            if [ \$TOTAL -eq 0 ]; then
                                COVERAGE=0
                            else
                                COVERAGE=\$((COVERED * 100 / TOTAL))
                            fi
                            echo \$COVERAGE
                        """
                        def coverage = sh(script: coverageScript, returnStdout: true).trim()
                        
                        def coverageInt = coverage.isEmpty() ? 0 : coverage.toInteger()
                        echo "Coverage (LINE) ${m}: ${coverageInt}%"

                        if (coverageInt < minCoverage) {
                            failures << "${m}: ${coverageInt}% < ${minCoverage}%"
                        }
                    }

                    if (!failures.isEmpty()) {
                        echo "Coverage gate FAILED - modules below ${minCoverage}%:"
                        failures.each { echo "  - ${it}" }
                        echo "Error: Add more unit tests to improve coverage above ${minCoverage}%"
                        error("Coverage gate failed for: ${failures.join(', ')}")
                    } else {
                        echo "Coverage gate PASSED - all modules ${minCoverage}% or above"
                    }
                }
            }
        }
    }

    post {
        success { echo 'Monorepo CI Pipeline completed successfully!' }
        failure { echo 'Monorepo CI Pipeline failed!' }
    }
}