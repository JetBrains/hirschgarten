"""
load("//:versions.bzl", "BENCHMARK_BUILD_NUMBER", "GO_BENCHMARK_BUILD_NUMBER", "PY_BENCHMARK_BUILD_NUMBER")
load("//rules/testing:commons.bzl", "INTELLIJ_JVM_FLAGS", "kt_test_suite")

_IDE_ID_TO_BUILD_NUMBER = {
    "IU": BENCHMARK_BUILD_NUMBER,
    "PY": PY_BENCHMARK_BUILD_NUMBER,
    "GO": GO_BENCHMARK_BUILD_NUMBER,
}

INTELLIJ_BAZEL_PLUGIN_ZIP = "plugin-bazel/plugin-bazel.zip"

IDE_STARTER_DEPS = [
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector",
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector_starter",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_driver",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_junit5",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_squashed",
    "@maven//:org_apache_httpcomponents_httpclient",
    "@maven//:org_kodein_di_kodein_di",
]

IDE_STARTER_BASE_TEST_DEPS = [
    #     "//plugin-bazel/src/testFixtures/kotlin/org/jetbrains/bazel/ideStarter:baseTest",
    "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/testing",
]

IDE_STARTER_RESOURCES = [
    "//plugin-bazel:plugin-bazel_zip",
]

def kt_integration_test_suite(name, srcs, deps = [], **kwargs):
    java_props = []
    for (ide, build_number) in _IDE_ID_TO_BUILD_NUMBER.items():
        java_props.append("-Dbazel.ide.starter.test.ide.%s.build.number=%s" % (ide, build_number))
    kt_test_suite(
        name,
        srcs = srcs,
        deps = deps + IDE_STARTER_DEPS + IDE_STARTER_BASE_TEST_DEPS,
        jvm_flags = [
            "-Dbazel.ide.starter.test.bazel.plugin.zip=%s" % INTELLIJ_BAZEL_PLUGIN_ZIP,
        ] + java_props,
        **kwargs
    )
"""