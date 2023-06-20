package configurations

import configurations.intellijBsp.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests


object IntellijBspAggregator : BuildType({
    id("intellij-bsp results".toExtId())

    name = "intellij-bsp results"

    vcs {
        root(BaseConfiguration.IntellijBspVcs)
        showDependenciesChanges = false
    }

    features {
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

    type = Type.COMPOSITE
})
