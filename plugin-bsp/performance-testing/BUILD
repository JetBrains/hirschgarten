load("//plugin-bsp/rules/kotlin:junit5.bzl", "kt_test")

INTELLIJ_BSP_PLUGIN_ZIP = "plugin-bsp/intellij-bsp.zip"

INTELLIJ_BAZEL_PLUGIN_ZIP = "plugin-bazel/intellij-bazel.zip"

PLATFORM_VERSION = "242.19890.14"

kt_test(
    name = "performance-testing",
    size = "large",
    srcs = glob(["src/**/*.kt"]),
    classname = "org.jetbrains.bsp.performance.testing.BazelTest",
    jvm_flags = [
        "-Dbsp.benchmark.platform.version=%s" % PLATFORM_VERSION,
        "-Dbsp.benchmark.bsp.plugin.zip=%s" % INTELLIJ_BSP_PLUGIN_ZIP,
        "-Dbsp.benchmark.bazel.plugin.zip=%s" % INTELLIJ_BAZEL_PLUGIN_ZIP,
    ],
    resources = [
        "//plugin-bazel:intellij-bazel-with-inmemory-server_zip",
        "//plugin-bsp:intellij-bsp_zip",
    ],
    tags = ["manual"],
    deps = [
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector",
        "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector_starter",
        "@maven//:com_jetbrains_intellij_tools_ide_starter_junit5",
        "@maven//:com_jetbrains_intellij_tools_ide_starter_squashed",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_kodein_di_kodein_di",
    ],
)
