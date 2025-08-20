load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "PublicApiCheckTest",
    src = "PublicApiCheckTest.kt",
    resources = [
        "public-api-classes.txt",
        "public-api-fields.txt",
        "public-api-methods.txt",
        "//plugin-bazel",
    ],
    tags = ["exclusive"],
    runtime_deps = [
        "//plugin-bazel",
    ],
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
    ],
)

kt_junit5_test(
    name = "RemoveWithSdkCompatTest",
    src = "RemoveWithSdkCompatTest.kt",
    data = [
        ":sdkCompatStructure",
    ],
    jvm_flags = [
        "-Dbazel.plugin.jar=$(rlocationpath //plugin-bazel)",
        "-Dsdk.compat.structure.output=$(rlocationpath :sdkCompatStructure)",
    ],
    resources = [
        "//plugin-bazel",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
        "@rules_java//java/runfiles",
    ],
)

genquery(
    name = "sdkCompatStructure",
    expression = "deps(//sdkcompat)",
    opts = ["--output=label"],
    scope = ["//sdkcompat"],
)
