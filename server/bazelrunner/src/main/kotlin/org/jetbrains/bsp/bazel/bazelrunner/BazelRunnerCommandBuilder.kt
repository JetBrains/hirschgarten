package org.jetbrains.bsp.bazel.bazelrunner

enum class BazelCommand(val command: String) {
  CLEAN("clean"),
  INFO("info"),
  RUN("run"),
  MOD_GRAPH("mod graph"),
  MOD_PATH("mod path"),
  MOD_SHOW_REPO("mod show_repo"),
  QUERY("query"),
  CQUERY("cquery"),
  BUILD("build"),
  MOBILE_INSTALL("mobile-install"),
  TEST("test"),
  COVERAGE("coverage");

  fun useBuildFlags() = when (this) {
    // TODO: why not CQUERY? google's plugin has a flag that must be used for to work...
    RUN, BUILD, MOBILE_INSTALL, TEST, COVERAGE -> true
    else -> false
  }
}

class BazelRunnerCommandBuilder internal constructor(private val bazelRunner: BazelRunner) {

  fun clean() = BazelRunnerBuilder(bazelRunner, "clean")

  fun info() = BazelRunnerBuilder(bazelRunner, "info")

  fun run() = BazelRunnerBuilder(bazelRunner, "run").withUseBuildFlags()

  fun mod(subcommand: String) = BazelRunnerBuilder(bazelRunner, "mod $subcommand")

  fun graph() = mod("graph")

  fun path() = mod("path")

  fun showRepo() = mod("show_repo")

  fun query() = BazelRunnerBuilder(bazelRunner, ("query"))

  fun cquery() = BazelRunnerBuilder(bazelRunner, ("cquery"))

  fun build() = BazelRunnerBuildBuilder(bazelRunner, ("build")).withUseBuildFlags()

  fun mobileInstall() = BazelRunnerBuildBuilder(bazelRunner, ("mobile-install")).withUseBuildFlags()

  fun test() = BazelRunnerBuildBuilder(bazelRunner, ("test")).withUseBuildFlags()

  fun coverage() = BazelRunnerBuildBuilder(bazelRunner, ("coverage")).withUseBuildFlags()
}
