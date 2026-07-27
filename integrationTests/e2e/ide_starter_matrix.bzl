"""Rules for declaring IDE-Starter tests across explicit OS and Bazel-version variants."""

load("@community//build:tests-options.bzl", "jps_test")
load("@jps_dynamic_deps_ultimate//:targets.bzl", "ALL_ULTIMATE_TARGETS", "BAZEL_TARGETS_JSON_ULTIMATE")

_OS_CONSTRAINTS = {
    "linux": "@platforms//os:linux",
    "macos": "@platforms//os:osx",
    "windows": "@platforms//os:windows",
}

def _version_suffix(version):
    if not version:
        fail("Bazel version must not be empty")
    for forbidden in ["/", "\\", ":", " "]:
        if forbidden in version:
            fail("Bazel version %r contains unsupported character %r" % (version, forbidden))
    return version.replace(".", "_").replace("-", "_")

def ide_starter_e2e_matrix(
        test_class,
        bazel_versions,
        default_bazel_version,
        os_list = ["linux", "macos", "windows"],
        visibility = ["//plugins/bazel/integrationTests/e2e:__pkg__"],
        **kwargs):
    """Declares one real jps_test per supported (OS, Bazel version) pair.

    Each OS-only test_suite points to that OS's committed/default Bazel version.
    visibility applies to the generated tests and suites; remaining keyword
    arguments are forwarded to every generated jps_test.
    """
    if not test_class:
        fail("test_class must not be empty")
    if not bazel_versions:
        fail("bazel_versions must not be empty")
    if default_bazel_version not in bazel_versions:
        fail("default_bazel_version %r is not declared in bazel_versions" % default_bazel_version)
    if len(bazel_versions) != len(depset(bazel_versions).to_list()):
        fail("bazel_versions contains duplicates: %r" % bazel_versions)
    if len(os_list) != len(depset(os_list).to_list()):
        fail("os_list contains duplicates: %r" % os_list)

    suffixes = {}
    for version in bazel_versions:
        suffix = _version_suffix(version)
        if suffix in suffixes:
            fail("Bazel versions %r and %r normalize to the same target suffix" % (suffixes[suffix], version))
        suffixes[suffix] = version

    for os in os_list:
        if os not in _OS_CONSTRAINTS:
            fail("Unsupported OS %r; expected one of %r" % (os, sorted(_OS_CONSTRAINTS.keys())))

        default_test = None
        for version in bazel_versions:
            name = "%s_bazel_%s" % (os, _version_suffix(version))
            jps_test(
                name = name,
                data = ALL_ULTIMATE_TARGETS + [BAZEL_TARGETS_JSON_ULTIMATE],
                env = {
                    # Read natively by bazelisk inside the launched IDE; wins over .bazelversion.
                    "USE_BAZEL_VERSION": version,
                    "JB_TEST_FILTER": test_class,
                },
                jvm_flags = [
                    "-Dintellij.build.bazel.targets.json.file=$(rlocationpath %s)" % BAZEL_TARGETS_JSON_ULTIMATE,
                ],
                runtime_deps = ["//plugins/bazel/integrationTests:integrationTests_test_lib"],
                tags = ["exclusive", "ide-starter-e2e"],
                target_compatible_with = [_OS_CONSTRAINTS[os]],
                visibility = visibility,
                **kwargs
            )
            if version == default_bazel_version:
                default_test = ":" + name

        native.test_suite(
            name = os,
            tests = [default_test],
            visibility = visibility,
        )

def ide_starter_e2e_host_test(
        test_class,
        os_list = ["linux", "macos", "windows"],
        visibility = ["//plugins/bazel/integrationTests/e2e:__pkg__"],
        **kwargs):
    """Declares OS-only tests for a project supplied dynamically at runtime.

    Unlike ide_starter_e2e_matrix, this rule does not select a Bazel version.
    The project is supplied dynamically through IDE-Starter system properties.
    Remaining keyword arguments are forwarded to every generated jps_test.
    """
    if not test_class:
        fail("test_class must not be empty")
    if len(os_list) != len(depset(os_list).to_list()):
        fail("os_list contains duplicates: %r" % os_list)

    for os in os_list:
        if os not in _OS_CONSTRAINTS:
            fail("Unsupported OS %r; expected one of %r" % (os, sorted(_OS_CONSTRAINTS.keys())))
        jps_test(
            name = os,
            data = ALL_ULTIMATE_TARGETS + [BAZEL_TARGETS_JSON_ULTIMATE],
            env = {
                "JB_TEST_FILTER": test_class,
            },
            jvm_flags = [
                "-Dintellij.build.bazel.targets.json.file=$(rlocationpath %s)" % BAZEL_TARGETS_JSON_ULTIMATE,
            ],
            runtime_deps = ["//plugins/bazel/integrationTests:integrationTests_test_lib"],
            tags = ["exclusive", "ide-starter-e2e", "ide-starter-dynamic-project"],
            target_compatible_with = [_OS_CONSTRAINTS[os]],
            visibility = visibility,
            **kwargs
        )
