load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "JVMLanguagePluginParserTest",
    size = "small",
    src = "JVMLanguagePluginParserTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)

kt_junit5_test(
    name = "LanguagePluginServiceTest",
    size = "small",
    src = "LanguagePluginServiceTest.kt",
    deps = [
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)
