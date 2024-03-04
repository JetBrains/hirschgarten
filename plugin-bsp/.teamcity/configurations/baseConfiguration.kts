package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script


open class BaseBuildType(
    name: String,
    vcsRoot: GitVcsRoot,
    steps: BuildSteps.() -> Unit,
    failureConditions: FailureConditions.() -> Unit = {},
    artifactRules: String = "",
    setupSteps: Boolean = false,
    requirements: Requirements.() -> Unit = {}
) : BuildType({
    id(name.toExtId())
    this.name = name
    this.artifactRules = artifactRules

    failureConditions {
        executionTimeoutMin = 60
    }

    this.requirements(requirements)

    this.failureConditions(failureConditions)


    vcs {
        root(vcsRoot)
    }


    features {
        perfmon {
        }

        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
                }
            }
            param("github_oauth_user", "hb-man")
        }

        pullRequests {
            vcsRootExtId = "${BaseConfiguration.IntellijBspVcs.id}"
            provider = github {
                authType = token {
                    token = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
                }
                filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
            }
        }
    }

    if (setupSteps) {
        steps {
            script {
                this.name = "Coursier"

                scriptContent = """
                    #!/bin/bash
                    set -euxo pipefail
                                        
                    #install coursier
                    curl -fL "https://github.com/coursier/coursier/releases/download/v2.1.5/cs-x86_64-pc-linux.gz" | gzip -d > cs 
                    sudo mv cs /usr/bin/cs
                    
                    sudo chmod +x "/usr/bin/cs"
            """.trimIndent()
            }
        }
    }
    this.steps(steps)
})


object IntellijBspVcs : GitVcsRoot({
    name = "intellij-bsp-github-repo"
    url = "https://github.com/JetBrains/intellij-bsp.git"
    branch = "main"
    branchSpec = """
        +:refs/heads/*
    """.trimIndent()

    authMethod = password {
        userName = "hb-man"
        password = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
    }
    checkoutPolicy = AgentCheckoutPolicy.USE_MIRRORS
    param("oauthProviderId", "tc-cloud-github-connection")
    param("tokenType", "permanent")
})