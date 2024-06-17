package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests


open class BaseBuildType(
    name: String,
    vcsRoot: GitVcsRoot,
    steps: BuildSteps.() -> Unit,
    failureConditions: FailureConditions.() -> Unit = {},
    artifactRules: String = "",
) : BuildType({
    this.name = name
    this.artifactRules = artifactRules

    failureConditions {
        executionTimeoutMin = 60
    }

    if (vcsRoot.name == "intellij-bsp-github" ) {
        id("GitHub$name".toExtId())
        requirements {
            endsWith("cloud.amazon.agent-name-prefix", "Medium")
            equals("container.engine.osType", "linux")
        }
    } else {
        id("Space$name".toExtId())
        requirements {
            endsWith("cloud.amazon.agent-name-prefix", "-XLarge")
            equals("container.engine.osType", "linux")
        }
    }

    this.failureConditions(failureConditions)


    vcs {
        root(vcsRoot)
    }


    features {
        perfmon {
        }
        if (vcsRoot.name == "intellij-bsp-github" ) {
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
                vcsRootExtId = "${vcsRoot.id}"
                provider = github {
                    authType = token {
                        token = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
                    }
                    filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
                }
            }
        } else {
            commitStatusPublisher {
                vcsRootExtId = "${vcsRoot.id}"
                publisher = space {
                    authType = connection {
                        connectionId = "PROJECT_EXT_12"
                    }
                    displayName = "BazelTeamCityCloud"
                }
            }
        }
    }

    this.steps(steps)
})


object GitHubVcs : GitVcsRoot({
    name = "intellij-bsp-github"
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

object SpaceVcs : GitVcsRoot({
    name = "intellij-bsp-space"
    url = "https://git.jetbrains.team/bazel/intellij-bsp.git"
    branch = "main"
    branchSpec = "+:refs/heads/*"
    authMethod = password {
        userName = "x-oauth-basic"
        password = "credentialsJSON:4efcb75d-2f9b-47fd-a63b-fc2969a334f5"
    }
    param("oauthProviderId", "PROJECT_EXT_15")
    param("tokenType", "permanent")
})