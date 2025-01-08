load("//:versions.bzl", "BENCHMARK_BUILD_NUMBER")
load("//rules/testing:junit5.bzl", "kt_junit5_test")

INTELLIJ_BSP_PLUGIN_ZIP = "plugin-bsp/intellij-bsp.zip"

INTELLIJ_BAZEL_PLUGIN_ZIP = "plugin-bazel/intellij-bazel.zip"

IDE_STARTER_DEPS = [
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector",
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector_starter",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_junit5",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_squashed",
    "@maven//:org_apache_httpcomponents_httpclient",
    "@maven//:org_kodein_di_kodein_di",
]

def ide_starter_test(deps = [], jvm_flags = [], resources = [], **kwargs):
    kt_junit5_test(
        deps = deps + IDE_STARTER_DEPS,
        jvm_flags = jvm_flags + [
            "-Dbazel.ide.starter.test.platform.build.number=%s" % BENCHMARK_BUILD_NUMBER,
            "-Dbazel.ide.starter.test.bsp.plugin.zip=%s" % INTELLIJ_BSP_PLUGIN_ZIP,
            "-Dbazel.ide.starter.test.bazel.plugin.zip=%s" % INTELLIJ_BAZEL_PLUGIN_ZIP,
        ],
        resources = resources + [
            "//plugin-bazel:intellij-bazel_zip",
            "//plugin-bsp:intellij-bsp_zip",
        ],
        **kwargs
    )
