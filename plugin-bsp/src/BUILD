load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")
load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("//rules/kotlin:junit5.bzl", "kt_test")

exports_files(["main/resources/META-INF/base.xml"])

kt_jvm_library(
    name = "intellij-bsp",
    srcs = glob([
        "main/kotlin/**/*.java",
        "main/kotlin/**/*.kt",
    ]),
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
        "@rules_intellij//third_party/python",
        "@rules_intellij//third_party/performance",
        "@maven//:com_google_code_gson_gson",
        "@maven//:ch_epfl_scala_bsp4j",
        "//protocol/src:protocol",
        "//workspacemodel/src:workspacemodel",
        "//jps-compilation/src:jps-compilation",
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
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@maven//:ch_epfl_scala_bsp4j",
        "@rules_intellij//testing:lib"
    ],
)

intellij_integration_test_suite(
    name = "plugin_tests",
    srcs = glob([
        "test/kotlin/org/jetbrains/plugins/bsp/**/*.kt",
    ]),
    associates = [":intellij-bsp"],
    test_package_root = "org.jetbrains",
    deps = [
        ":test_fixtures",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
    runtime_deps = [
        "//:intellij-bsp",
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
