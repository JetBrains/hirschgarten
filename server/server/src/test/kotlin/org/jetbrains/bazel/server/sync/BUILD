load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "TargetTagsResolverTest",
    size = "small",
    src = "TargetTagsResolverTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
