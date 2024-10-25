load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("//rules/testing:junit5.bzl", "kt_junit5_test")

intellij_integration_test_suite(
    name = "utils",
    srcs = glob([
        "*.kt",
    ]),
    associates = ["//plugin-bsp/src:intellij-bsp"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bsp/src:test_fixtures",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

kt_junit5_test(
    name = "PathExtensionTest",
    src = "PathExtensionTest.kt",
    deps = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
    ],
)
