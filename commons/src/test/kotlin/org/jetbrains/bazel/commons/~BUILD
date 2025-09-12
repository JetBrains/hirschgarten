load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "RepoMappingTest",
    size = "small",
    src = "RepoMappingTest.kt",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "@maven//:com_jetbrains_intellij_platform_util",
    ],
)
