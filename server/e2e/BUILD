load("@//rules/bazel_integration_test:defs.bzl", "bazel_integration_test_all_versions")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "bazel_integration_test", "integration_test_utils")

package(default_visibility = ["//visibility:public"])

####################
# SUPER IMPORTANT! #
####################
# Wheneven you add a new test, make sure to run `bazel run @rules_bazel_integration_test//tools:update_deleted_packages`

bazel_integration_test_all_versions(
    name = "sample_repo_test",
    bzlmod_project_path = "test-projects/bzlmod/sample-repo",
    env = {
        "foo1": "invalid_val1",
        "foo2": "invalid_val2",
        "foo3": "val3",
        "foo4": "val4",
    },
    project_path = "test-projects/workspace/sample-repo",
)

bazel_integration_test_all_versions(
    name = "local_jdk_test",
    bzlmod_project_path = "test-projects/bzlmod/local-jdk-project",
    inherited_env_names = ["JAVA_HOME"],
    project_path = "test-projects/workspace/local-jdk-project",
)

bazel_integration_test_all_versions(
    name = "remote_jdk_test",
    bzlmod_project_path = "test-projects/bzlmod/remote-jdk-project",
    project_path = "test-projects/workspace/remote-jdk-project",
)

bazel_integration_test_all_versions(
    name = "python_project_test",
    bzlmod_project_path = "test-projects/bzlmod/python-project",
    project_path = "test-projects/workspace/python-project",
)

bazel_integration_test_all_versions(
    name = "kotlin_project_test",
    bzlmod_project_path = "test-projects/bzlmod/kotlin-project",
    project_path = "test-projects/workspace/kotlin-project",
)

bazel_integration_test_all_versions(
    name = "enabled_rules_test",
    project_path = "test-projects/workspace/enabled-rules-project",
)

bazel_integration_test_all_versions(
    name = "go_project_test",
    bzlmod_project_path = "test-projects/bzlmod/go-project",
)

bazel_integration_test_all_versions(
    name = "java_diagnostics_test",
    bzlmod_project_path = "test-projects/bzlmod/java-diagnostics-project",
)

bazel_integration_test_all_versions(
    name = "allow_manual_targets_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/allow-manual-targets-sync-project",
    project_path = "test-projects/workspace/allow-manual-targets-sync-project",
)

bazel_integration_test_all_versions(
    name = "build_and_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/build-and-sync-test",
)

bazel_integration_test_all_versions(
    name = "first_phase_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/first-phase-sync-project",
)

# bazel_integration_test_all_versions(
#     name = "nested_modules_test",
#     bzlmod_project_path = "test-projects/bzlmod/nested-modules",
# )

bazel_integration_test_all_versions(
    name = "external_autoloads_test",
    bzlmod_project_path = "test-projects/bzlmod/external-autoloads-project",
    exclude_bazel_7 = True,
)

test_suite(
    name = "all_tests",
    tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
    # we need to include all of them manually because of the "manual" tag
    tests = [
        ":allow_manual_targets_sync_test",
        ":build_and_sync_test",
        ":enabled_rules_test",
        ":external_autoloads_test",
        ":first_phase_sync_test",
        ":go_project_test",
        ":java_diagnostics_test",
        ":kotlin_project_test",
        ":local_jdk_test",
        ":python_project_test",
        ":remote_jdk_test",
        ":sample_repo_test",
    ],
)

# Just a reminder - have you read the note at the top of this file?
