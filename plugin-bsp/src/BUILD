load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

exports_files(glob(["main/xml/*.xml"]))

kt_jvm_library(
    name = "intellij-bsp",
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
    exports = [
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/android",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/assets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/building",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/flow",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/intellij",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/jvm",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/kotlin",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/performance",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/python",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/scala",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/chunking",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/client",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/tasks",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/services",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/actions",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/server/timeout",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/gutters",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/project",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/runconfig",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/workspace",
    ],
)

kt_jvm_library(
    name = "test_fixtures",
    testonly = 1,
    srcs = glob([
        "testFixtures/kotlin/**/*.kt",
    ]),
    deps = [
        ":intellij-bsp",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@rules_intellij//testing:lib",
    ],
)

intellij_integration_test_suite(
    name = "plugin_tests",
    srcs = glob([
        "test/kotlin/org/jetbrains/plugins/bsp/**/*.kt",
    ]),
    associates = [":intellij-bsp"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        ":test_fixtures",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

intellij_integration_test_suite(
    name = "utils_tests",
    srcs = glob([
        "test/kotlin/utils/**/*.kt",
    ]),
    associates = [":intellij-bsp"],
    test_package_root = "utils",
    deps = [
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
