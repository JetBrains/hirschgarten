load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "BuildTargetCapabilitiesConverterTest",
    size = "small",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/target"],
    test_package_root = "org.jetbrains",
    deps = [
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
