package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests


object IntellijBazelAggregator : BuildType({
    id("intellij-bsp results".toExtId())

    name = "intellij-bazel results"

    vcs {
        root(BaseConfiguration.IntellijBazelVcs)
        showDependenciesChanges = false
    }

    allowExternalStatus = true

    features {
        pullRequests {
            vcsRootExtId = "${BaseConfiguration.IntellijBazelVcs.id}"
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
