load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BazelBspLanguageExtensionsGeneratorTest",
    size = "small",
    src = "BazelBspLanguageExtensionsGeneratorTest.kt",
    deps = [
        "//server/commons",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/managers",
    ],
)

kt_junit5_test(
    name = "BzlModGraphTest",
    size = "small",
    src = "BzlModGraphTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/managers",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/utils:structured_files_utils",
    ],
)
