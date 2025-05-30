load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "JVMLanguagePluginParserTest",
    size = "small",
    src = "JVMLanguagePluginParserTest.kt",
    tags = ["exclusive"],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
    ],
)

kt_junit5_test(
    name = "LanguagePluginServiceTest",
    size = "small",
    src = "LanguagePluginServiceTest.kt",
    tags = ["exclusive"],
    deps = [
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
    ],
)
