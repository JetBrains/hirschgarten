load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

kt_junit5_test(
    name = "WorkspaceContextConstructorTest",
    size = "small",
    src = "WorkspaceContextConstructorTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "BazelBinarySpecMapperTest",
    size = "small",
    src = "BazelBinarySpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "BuildFlagsSpecMapperTest",
    size = "small",
    src = "BuildFlagsSpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "SyncFlagsSpecMapperTest",
    size = "small",
    src = "SyncFlagsSpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "AllowManualTargetsSyncSpecMapperTest",
    size = "small",
    src = "AllowManualTargetsSyncSpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "TargetsSpecMapperTest",
    size = "small",
    src = "TargetsSpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "ImportDepthSpecMapperTest",
    size = "small",
    src = "ImportDepthSpecMapperTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

kt_junit5_test(
    name = "DirectoriesSpecExtractorTest",
    size = "small",
    src = "DirectoriesSpecExtractorTest.kt",
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
)

intellij_integration_test_suite(
    name = "DefaultWorkspaceContextProviderTest",
    srcs = ["DefaultWorkspaceContextProviderTest.kt"],
    associates = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext/provider",
    ],
    test_package_root = "org.jetbrains.bazel.workspacecontext",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
    ],
)
