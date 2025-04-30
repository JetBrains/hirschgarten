load("//:versions.bzl", "BENCHMARK_BUILD_NUMBER", "PY_BENCHMARK_BUILD_NUMBER")
load("//rules/testing:junit5.bzl", "kt_junit5_test")

INTELLIJ_BAZEL_PLUGIN_ZIP = "plugin-bazel/plugin-bazel.zip"

IDE_STARTER_DEPS = [
    "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/resourceUtil",
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector",
    "@maven//:com_jetbrains_intellij_tools_ide_metrics_collector_starter",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_driver",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_junit5",
    "@maven//:com_jetbrains_intellij_tools_ide_starter_squashed",
    "@maven//:org_apache_httpcomponents_httpclient",
    "@maven//:org_kodein_di_kodein_di",
]

IDE_STARTER_RESOURCES = [
    "//plugin-bazel:plugin-bazel_zip",
]

_IDE_STARTER_TEST_TAGS = ["manual", "ide-starter-test"]

IDE_ID = struct(
    IC = "IC",  # IntelliJ Community
    PY = "PY",  # PyCharm
)

_SUPPORTED_IDES = (IDE_ID.IC, IDE_ID.PY)

_IDE_ID_TO_BUILD_NUMBER = {
    IDE_ID.IC: BENCHMARK_BUILD_NUMBER,
    IDE_ID.PY: PY_BENCHMARK_BUILD_NUMBER,
}

def ide_starter_test(name, ide_ids, deps = [], jvm_flags = [], resources = [], **kwargs):
    if type(ide_ids) != "list":
        fail("ide_ids: wrong type. It must be a list.")

    if len(ide_ids) == 0:
        fail("ide_ids cannot be empty list.")

    for ide_id in ide_ids:
        if ide_id not in _SUPPORTED_IDES:
            fail("Invalid IDE id: {}, supported IDEs: {}".format(ide_id, _SUPPORTED_IDES))

        ide_build_number = _IDE_ID_TO_BUILD_NUMBER[ide_id]

        kt_junit5_test(
            name = name + ide_id,
            deps = deps + IDE_STARTER_DEPS,
            size = "large",
            jvm_flags = jvm_flags + [
                "-Dbazel.ide.starter.test.ide.id=%s" % ide_id,
                "-Dbazel.ide.starter.test.ide.build.number=%s" % ide_build_number,
                "-Dbazel.ide.starter.test.bazel.plugin.zip=%s" % INTELLIJ_BAZEL_PLUGIN_ZIP,
            ],
            resources = resources + IDE_STARTER_RESOURCES,
            tags = _IDE_STARTER_TEST_TAGS,
            **kwargs
        )

    native.test_suite(
        name = name,
        tags = _IDE_STARTER_TEST_TAGS,
        tests = [name + ide_id for ide_id in ide_ids],
    )
