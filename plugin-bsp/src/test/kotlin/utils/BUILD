load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "utils",
    srcs = glob([
        "*.kt",
    ]),
    associates = ["//plugin-bsp/src:intellij-bsp"],
    test_package_root = "utils",
    deps = [
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
