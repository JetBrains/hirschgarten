load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "SourceRootGuesserTest",
    size = "small",
    src = "SourceRootGuesserTest.kt",
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/languages",
    ],
)
