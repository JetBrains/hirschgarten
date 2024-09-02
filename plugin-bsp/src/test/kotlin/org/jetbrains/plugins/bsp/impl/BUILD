load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")

# TODO: split when the src target is split
intellij_integration_test_suite(
    name = "impl",
    srcs = glob([
        "**/*.kt",
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
