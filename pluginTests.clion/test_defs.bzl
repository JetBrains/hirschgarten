load("@community//build:tests-options.bzl", "jps_test")
load("@community//plugins/kotlin:kotlin_test_dependencies.bzl", "all_test_dep_targets")
load("@rules_jvm//:jvm.bzl", "jvm_library")

_TESTS_DIR = "tests/"

# The tests compile only against the CLion LanguageEngine test abstraction. These packages load Radler and the Rider
# backend at runtime, through `CC_TEST_RUNTIME_DEPS`.
_RUNTIME_ONLY_PACKAGES = ["//CIDR/clion-radler", "//rider/"]

# The compile dependencies of `:pluginTests.clion_test_lib` and of every test, in the order of the class path.
# Keep this list in sync with the `TEST` dependencies of `intellij.bazel.pluginTests.clion.iml`.
CC_TEST_DEPS = [
    # do not sort
    "//plugins/bazel/pluginTests:pluginTests_test_lib",
    "@lib//:kotlin-stdlib",
    "//CIDR/nativeDebug/plugin",
    "//CIDR/clion",
    "//CIDR/cidr-projectModel",
    "//CIDR/cidr-psi-base",
    "//CIDR/clion-testFramework-nolang/junit5/core",
    "@community//platform/analysis-api:analysis",
    "@community//platform/core-api:core",
    "@community//platform/core-impl",
    "@community//platform/lang-api:lang",
    "@community//platform/lang-impl",
    "@community//platform/buildView",
    "@community//platform/testFramework",
    "@community//platform/testFramework/junit5",
    "@community//platform/projectModel-impl",
    "@community//platform/workspace/storage",
    "@community//libraries/assertj-core",
    "//plugins/bazel/intellij.bazel.commons:commons",
    "//plugins/bazel/intellij.bazel.backend:backend",
    "//plugins/bazel/intellij.bazel.core:core",
    "//plugins/bazel/intellij.bazel.languages:languages",
    "//plugins/bazel/clion/intellij.bazel.clion.common:bazel-clion-common",
    "//plugins/bazel/clion/intellij.bazel.clion.core:bazel-clion-core",
    "//plugins/bazel/clion/intellij.bazel.clion.shared:bazel-clion-shared",
    "@community//libraries/junit6",
    "@community//libraries/junit6-params",
    "@community//platform/execution",
    "@community//platform/smRunner",
    "@community//platform/ide-core",
    "@community//platform/testRunner",
    "//CIDR/clion-testFramework-nolang/junit5/debugger",
    "//CIDR/cidr-common-testFramework/nolang",
    "//CIDR/clion-execution",
    "//CIDR/cidr-debugger/core",
    "@community//platform/xdebugger-api:debugger",
    "@community//platform/xdebugger-testFramework:debugger-testFramework",
    "//CIDR/cidr-runner",
    "//CIDR/cidr-debugger/backend-api:cidr-debugger-backend",
    "//CIDR/cidr-debugger/profiles/backend",
    "//CIDR/cidr-debugger/dap",
    "@community//platform/xdebugger-impl:debugger-impl",
    "@community//platform/projectModel-api:projectModel",
    "//plugins/bazel/clion/intellij.bazel.clion.test.google:bazel-clion-test-google",
    "//plugins/bazel/clion/intellij.bazel.clion.test.catch:bazel-clion-test-catch",
    "//CIDR/cidr-execution",
    "@community//libraries/rd/core",
    "@community//libraries/rd/framework",
    "@community//platform/execution-impl",
    "@community//platform/consoleView",
    "@community//platform/core-ui",
    "@community//platform/util:util-ui",
    "@community//platform/editor-ui-api:editor-ui",
    "//CIDR/clion-test-google/core",
    "//CIDR/clion-test-catch/core",
    "@community//platform/scratch/plugin",
    "//CIDR/cidr-lang-base",
    "@community//platform/bazel-runfiles",
]

# The runtime dependencies of `:pluginTests.clion_test_lib`. They hold Radler and the Rider backend.
# Keep this list in sync with the `RUNTIME` dependencies of `intellij.bazel.pluginTests.clion.iml`.
CC_TEST_RUNTIME_DEPS = [
    # do not sort
    "//CIDR/clion-radler",
    "//CIDR/clion-runFile/nova",
    "//CIDR/clion-radler/testFramework/junit5",
    "//rider/rdclient-dotnet",
    "//rider/model/generated",
    "//CIDR/clion-radler/testing",
    "//CIDR/clion-radler/core",
    "@community//json/plugin",
    "@community//plugins/yaml/plugin",
    "//CIDR/clion-test-google/nova",
    "//CIDR/clion-test-google",
    "//CIDR/clion-test-catch/nova",
    "//CIDR/clion-test-catch",
]

def _check_compile_deps(owner, deps):
    """Fails if a compile dependency is in a runtime-only package."""
    for dep in deps:
        for package in _RUNTIME_ONLY_PACKAGES:
            if dep.startswith(package):
                fail("%s must not compile against %s. Radler and Rider are runtime dependencies only." % (owner, dep))

def clion_plugin_test(test, deps = [], data = [], env = {}, **kwargs):
    """Declares a test target and its library for one Kotlin file in `tests/`.

    The library compiles only this file, against `CC_TEST_DEPS` and `:pluginTests.clion_test_lib`. It sees the
    `internal` declarations of `:pluginTests.clion_test_lib`. The name of the test target is the name of the file
    without `.kt`, and the library has the suffix `_lib`.

    Args:
        test: The name of the Kotlin file in `tests/`, for example `CcCoptsTest.kt`.
        deps: The additional compile dependencies of the test.
        data: The additional runtime data of the test.
        env: The environment of the test.
    """

    if not test.endswith(".kt"):
        fail("The test must be the name of a Kotlin file in %s: %s" % (_TESTS_DIR, test))

    _check_compile_deps("CC_TEST_DEPS", CC_TEST_DEPS)
    _check_compile_deps("The test " + test, deps)

    name = test.removesuffix(".kt")
    lib = name + "_lib"

    jvm_library(
        name = lib,
        testonly = True,
        srcs = [_TESTS_DIR + test],
        associates = [":pluginTests.clion_test_lib"],
        deps = CC_TEST_DEPS + deps,
    )

    jps_test(
        name = name,
        data = all_test_dep_targets + data,
        env = env,
        env_inherit = ["RESHARPER_HOST_BIN"],
        runtime_deps = [lib],
        **kwargs
    )
