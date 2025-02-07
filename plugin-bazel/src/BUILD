load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

exports_files([
    "main/resources/META-INF/base.xml",
    "test/resources/META-INF/bazel-ide-starter-test.xml",
])

kt_jvm_library(
    name = "intellij-bsp",
    neverlink = True,
    visibility = ["//plugin-bazel:__subpackages__"],
    exports = [
        "//plugin-bsp/src:intellij-bsp",
    ],
)

kt_jvm_library(
    name = "intellij-bazel",
    # Do not depend on this target because it exports the Bazel BSP server dependency!
    # If you want to share code with the server, use //commons.
    # Otherwise depend on a more specific subpackage of //plugin-bazel
    visibility = [
        "//plugin-bazel:__pkg__",
        "//plugin-bazel/plugin-inmem-server:__pkg__",
        "//plugin-bazel/src:__pkg__",
        "//plugin-bazel/src/test:__subpackages__",
    ],
    exports = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/bsp/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/debug",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/extension",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/flow",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/hotswap",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ideStarter",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/settings",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/ui",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
    ],
)

kt_jvm_library(
    name = "intellij-bazel-resources",
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
)

intellij_integration_test_suite(
    name = "plugin_tests",
    srcs = glob([
        "test/kotlin/org/jetbrains/**/*.kt",
    ]),
    associates = [":intellij-bazel"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        ":intellij-bazel",
        "//plugin-bsp/src:intellij-bsp",
    ],
    deps = [
        "//plugin-bsp/src:test_fixtures",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
