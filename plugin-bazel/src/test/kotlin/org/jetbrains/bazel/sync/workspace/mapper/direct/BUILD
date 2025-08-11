load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "DirectTargetMapperTest",
    srcs = ["DirectTargetMapperTest.kt"],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains.bazel.sync.workspace.mapper.direct",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@maven//:com_google_truth_truth",
    ],
)

intellij_integration_test_suite(
    name = "JvmDirectMapperTest",
    srcs = ["JvmDirectMapperTest.kt"],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains.bazel.sync.workspace.mapper.direct",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/workspace",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@maven//:com_google_truth_truth",
    ],
)
