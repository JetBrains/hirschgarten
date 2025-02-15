load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "BspProjectMapperTest",
    size = "small",
    src = "BspProjectMapperTest.kt",
    deps = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)

kt_junit5_test(
    name = "DependencyMapperTest",
    size = "small",
    src = "DependencyMapperTest.kt",
    deps = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)

kt_junit5_test(
    name = "TargetTagsResolverTest",
    size = "small",
    src = "TargetTagsResolverTest.kt",
    deps = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
