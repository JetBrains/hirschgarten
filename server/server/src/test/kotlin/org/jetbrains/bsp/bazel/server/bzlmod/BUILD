load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "RepoMappingTest",
    size = "small",
    src = "RepoMappingTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bzlmod",
    ],
)
