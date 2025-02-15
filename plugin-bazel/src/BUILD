load("@rules_intellij//testing:test_defs_kt.bzl", "intellij_integration_test_suite")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

exports_files([
    "main/resources/META-INF/base.xml",
    "test/resources/META-INF/bazel-ide-starter-test.xml",
] + glob(["main/xml/*.xml"]))


kt_jvm_library(
    name = "intellij-bsp",
    visibility = ["//visibility:public"],
    exports = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/action/registered",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/android",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/android/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/annotations",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/assets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/buildTask",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/extensionPoints",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/flow/close",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/flow/open",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/flow/open/wizard",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/golang/debug",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/golang/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/golang/ui/gutters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/intellij",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/java",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/jvm/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/jvm/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/jvm/ui/gutters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/kotlin",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/magicmetamodel/impl/workspacemodel/impl/updaters/transformers",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/performance",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/performanceImpl",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/projectAware",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/python/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/run",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/runnerAction",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/scala",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/chunking",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/client",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/connection",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/tasks",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/server/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/services",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/startup",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/defaults",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/projectStructure",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/projectStructure/workspaceModel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/scope",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/status",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/sync/task",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/target",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/taskEvents",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/filter",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/console/ids",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/gutters",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/notifications",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/projectTree",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/projectTree/action",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/ui/widgets/fileTargets",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/utils",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/workspace",
    ],
)

kt_jvm_library(
    name = "intellij-bsp-resources",
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "test_fixtures",
    testonly = 1,
    srcs = glob([
        "testFixtures/kotlin/**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-bazel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/plugins/bsp/coroutines",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@rules_intellij//testing:lib",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
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
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
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
    ],
    deps = [
        "//plugin-bazel/src:test_fixtures",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
