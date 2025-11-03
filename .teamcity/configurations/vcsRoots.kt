package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/**
 * VCS roots used in the project.
 */
object VcsRootBazelQodana : GitVcsRoot({
    name = "Bazel for Qodana"
    url = "https://github.com/JetBrainsBazelBot/bazel"
    branch = "master"
    authMethod = password {
        userName = "hb-man"
        password = CredentialsStore.GitHubPassword
    }
  param("oauthProviderId", "tc-cloud-github-connection")
  param("tokenType", "permanent")
})

object VcsRootBuildBuddyQodana : GitVcsRoot({
    name = "BuildBuddy for Qodana"
    url = "https://github.com/JetBrainsBazelBot/buildbuddy"
    branch = "master"
    authMethod = password {
        userName = "hb-man"
        password = CredentialsStore.GitHubPassword
    }
    param("oauthProviderId", "tc-cloud-github-connection")
    param("tokenType", "permanent")
})

object VcsRootHirschgarten : GitVcsRoot({
    name = "hirschgarten-github"
    url = "https://github.com/JetBrains/hirschgarten.git"
    branch = "main"
    branchSpec = "+:refs/heads/*"
    authMethod =
      password {
        userName = "hb-man"
        password = CredentialsStore.GitHubPassword
      }
    param("oauthProviderId", "tc-cloud-github-connection")
    param("tokenType", "permanent")
})

object VcsRootHirschgartenSpace : GitVcsRoot({
    name = "hirschgarten-space"
    url = "https://git.jetbrains.team/bazel/hirschgarten.git"
    branch = "252"
    branchSpec = """
      +:refs/heads/*
      +:(refs/merge/*)
    """.trimIndent()
    authMethod =
      password {
        userName = "x-oauth-basic"
        password = CredentialsStore.SpaceToken
      }
    param("oauthProviderId", "PROJECT_EXT_15")
    param("tokenType", "permanent")
})
