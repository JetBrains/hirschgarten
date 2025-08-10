package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/**
 * VCS roots used in the project.
 */
object BazelQodana : GitVcsRoot({
    name = "Bazel for Qodana"
    url = "https://github.com/JetBrainsBazelBot/bazel"
    branch = "master"
    authMethod = password {
        userName = "hb-man"
        password = Utils.CredentialsStore.GitHubPassword
    }
  param("oauthProviderId", "tc-cloud-github-connection")
  param("tokenType", "permanent")
})

object BuildBuddyQodana : GitVcsRoot({
    name = "BuildBuddy for Qodana"
    url = "https://github.com/JetBrainsBazelBot/buildbuddy"
    branch = "master"
    authMethod = password {
        userName = "hb-man"
        password = Utils.CredentialsStore.GitHubPassword
    }
    param("oauthProviderId", "tc-cloud-github-connection")
    param("tokenType", "permanent")
})

object GitHubVcs : GitVcsRoot({
    name = "hirschgarten-github"
    url = "https://github.com/JetBrains/hirschgarten.git"
    branch = "main"
    branchSpec = "+:refs/heads/*"
    authMethod =
      password {
        userName = "hb-man"
        password = Utils.CredentialsStore.GitHubPassword
      }
    param("oauthProviderId", "tc-cloud-github-connection")
    param("tokenType", "permanent")
})
