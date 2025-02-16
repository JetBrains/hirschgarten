load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")
load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "console",
    srcs = glob(["*.kt"]),
    associates = ["//plugin-bazel/src:intellij-bsp"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bazel:intellij-bazel",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//plugin-bazel/src:test_fixtures",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

kt_intellij_junit4_test(
    name = "BazelBuildTargetConsoleFilterTest",
    src = "BazelBuildTargetConsoleFilterTest.kt",
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui/console/bazel",
    ],
)
