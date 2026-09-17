load("@rules_jvm//:jvm.bzl", "jps_test", "jvm_library", "resourcegroup")
load("@rules_java//java:java_library.bzl", "java_library")

# Hand-maintained. It mirrors pluginTests/BUILD.bazel, but it keeps only the dependencies that survive (resolvable in
# open-source checkout). Everything from @community//, @lib// and //platform/ comes from the SDK zip via commons.bzl.
# The SDK jars themselves are generated: see gen_sdk_test_deps.py and sdk_test_deps.bzl.

jps_test(
    name = "pluginTests_test",
    shard_count = 4,
    runtime_deps = [
        ":pluginTests_test_lib",
    ],
)

resourcegroup(
    name = "pluginTests_test_resources",
    srcs = glob(
        ["testData/**/*"],
    ),
    strip_prefix = "testData",
)

java_library(
    name = "pluginTests_compat_resources",
    testonly = True,
    resource_strip_prefix = "pluginTests/compat/hirschgarten/resources",
    resources = glob(["compat/hirschgarten/resources/**/*"]),
)

jvm_library(
    name = "pluginTests_test_lib",
    testonly = True,
    srcs = glob(
        [
            "test/**/*.kt",
            "test/**/*.java",
            "test/**/*.form",
            "testFixtures/**/*.kt",
            "testFixtures/**/*.java",
            "testFixtures/**/*.form",
            "compat/hirschgarten/**/*.kt",
            "compat/hirschgarten/**/*.java",
            "compat/hirschgarten/**/*.form",
        ],

        allow_empty = True,
    ),
    module_name = "intellij.bazel.pluginTests",
    resource_jars = [":pluginTests_test_resources"],
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":pluginTests_compat_resources",
    ],
    deps = [
        # do not sort,
        "//intellij.bazel.bazelisk:bazelisk",
        "//misc/intellij.bazel.bytecodeViewer:bazel-bytecodeViewer",
        "//intellij.bazel.commons:commons",
        "//intellij.bazel.backend:backend",
        "//intellij.bazel.core:core",
        "//intellij.bazel.core.performancePlugin:core-performancePlugin",
        "//misc/intellij.bazel.coverage:bazel-coverage",
        "//misc/intellij.bazel.coverage.performancePlugin:bazel-coverage-performancePlugin",
        "//misc/intellij.bazel.devkit:bazel-devkit",
        "//golang/intellij.bazel.golang.common:bazel-golang-common",
        "//java/intellij.bazel.java.core:bazel-java-core",
        "//java/intellij.bazel.java.common.performancePlugin:bazel-java-common-performancePlugin",
        "//java/intellij.bazel.java.coverage:bazel-java-coverage",
        "//kotlin/intellij.bazel.kotlin.common:bazel-kotlin-common",
        "//kotlin/intellij.bazel.kotlin.common.performancePlugin:bazel-kotlin-common-performancePlugin",
        "//kotlin/intellij.bazel.kotlin.coverage:bazel-kotlin-coverage",
        "//kotlin/intellij.bazel.kotlin.k2:bazel-kotlin-k2",
        "//intellij.bazel.plugin:plugin",
        "//intellij.bazel.projectview:projectview",
        "//protobuf",
        "//misc/intellij.bazel.protoedit:bazel-protoedit",
        "//python/intellij.bazel.python.common:bazel-python-common",
        "//python/intellij.bazel.python.common.performancePlugin:bazel-python-common-performancePlugin",
        "//misc/intellij.bazel.remoteDevelopment:bazel-remoteDevelopment",
        "//kotlin/intellij.bazel.kotlin.sync:bazel-kotlin-sync",
        "//java/intellij.bazel.java.sync:bazel-java-sync",
        "//python/backend",
        "//aspect-sdk:libraries-bazel-aspect-sdk",
        "//java/intellij.bazel.java.common:bazel-java-common",
    ],
)

exports_files(
    [
        "intellij.bazel.pluginTests.iml",
    ],
    visibility = ["//visibility:public"],
)
