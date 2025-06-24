load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "FormatTest",
    size = "small",
    src = "FormatTest.kt",
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
    ],
)

kt_junit5_test(
    name = "ExecutionRootPathTest",
    size = "small",
    src = "ExecutionRootPathTest.java",
    deps = [
        "//commons/src:testFixtures",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "@maven//:com_google_truth_truth",
    ],
)

kt_junit5_test(
    name = "WorkspacePathTest",
    size = "small",
    src = "WorkspacePathTest.java",
    deps = [
        "//commons/src:testFixtures",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "@maven//:com_google_truth_truth",
    ],
)

kt_junit5_test(
    name = "PackagePathValidatorTest",
    size = "small",
    src = "PackagePathValidatorTest.java",
    deps = [
        "//commons/src:testFixtures",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "@maven//:com_google_truth_truth",
    ],
)
