load("//rules/kotlin:junit5.bzl", "kt_test")

PLUGIN_ZIP = "intellij-bsp.zip"
PLATFORM_VERSION = "242.18071.24"

kt_test(
    name = "performance-testing",
    srcs = glob(["src/**/*.kt"]),
    deps = [
        "@maven//:com_jetbrains_intellij_tools_ide_starter_squashed",
        "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector",
        "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector_starter",
        "@maven//:com_jetbrains_intellij_tools_ide_starter_junit5",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:org_kodein_di_kodein_di",
        "@maven//:org_apache_httpcomponents_httpclient",
    ],
    classname = "org.jetbrains.bsp.performance.testing.BazelTest",
    jvm_flags = [
        "-Dbsp.benchmark.platform.version=%s" % PLATFORM_VERSION,
        "-Dbsp.benchmark.plugin.zip=%s" % PLUGIN_ZIP,
    ],
    resources = ["//:intellij-bsp_zip"],
    tags = ["manual"],
    size = "large",
)