load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "sync",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src:plugin-bazel-lib"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bazel",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src:test_fixtures",
        "//rules_intellij/third_party/go:go_for_tests",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
