load("@//server/rules/kotlin:junit5.bzl", "kt_test")

kt_test(
    name = "JVMLanguagePluginParserTest",
    size = "small",
    src = "JVMLanguagePluginParserTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)

kt_test(
    name = "LanguagePluginServiceTest",
    size = "small",
    src = "LanguagePluginServiceTest.kt",
    deps = [
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)
