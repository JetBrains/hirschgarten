load("@//rules/bazel_integration_test:defs.bzl", "bazel_integration_test_all_versions", "bazel_integration_test_current_version")
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
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspSampleRepoTest",
)

bazel_integration_test_all_versions(
    name = "local_jdk_test",
    additional_env_inherit = ["JAVA_HOME"],
    bzlmod_project_path = "test-projects/bzlmod/local-jdk-project",
    project_path = "test-projects/workspace/local-jdk-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspLocalJdkTest",
)

bazel_integration_test_all_versions(
    name = "remote_jdk_test",
    bzlmod_project_path = "test-projects/bzlmod/remote-jdk-project",
    project_path = "test-projects/workspace/remote-jdk-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspRemoteJdkTest",
)

bazel_integration_test_all_versions(
    name = "cpp_project_test",
    project_path = "test-projects/workspace/cpp-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspCppProjectTest",
)

bazel_integration_test_all_versions(
    name = "python_project_test",
    bzlmod_project_path = "test-projects/bzlmod/python-project",
    project_path = "test-projects/workspace/python-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspPythonProjectTest",
)

bazel_integration_test_all_versions(
    name = "kotlin_project_test",
    bzlmod_project_path = "test-projects/bzlmod/kotlin-project",
    project_path = "test-projects/workspace/kotlin-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspKotlinProjectTest",
)

bazel_integration_test_all_versions(
    name = "kotlin_project_with_enabled_rules_test",
    bzlmod_project_path = "test-projects/bzlmod/kotlin-project",
    project_path = "test-projects/workspace/kotlin-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspKotlinProjectWithEnabledRulesTest",
)

bazel_integration_test_all_versions(
    name = "enabled_rules_test",
    project_path = "test-projects/workspace/enabled-rules-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspScalaProjectTest",
)

bazel_integration_test(
    name = "server_downloads_bazelisk_test",
    timeout = "eternal",
    bazel_version = "6.4.0",
    env = {
        "PATH": "",  # To ensure that the server won't find Bazel in PATH
    },
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:ServerDownloadsBazeliskTest",
    workspace_path = "test-projects/workspace/sample-repo",
)

bazel_integration_test_current_version(
    name = "android_project_test",
    project_path = "test-projects/workspace/android-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspAndroidProjectTest",
)

bazel_integration_test_current_version(
    name = "android_kotlin_project_test",
    project_path = "test-projects/workspace/android-kotlin-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspAndroidKotlinProjectTest",
)

bazel_integration_test_all_versions(
    name = "go_project_test",
    bzlmod_project_path = "test-projects/bzlmod/go-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspGoProjectTest",
)

bazel_integration_test_all_versions(
    name = "java_diagnostics_test",
    bzlmod_project_path = "test-projects/bzlmod/java-diagnostics-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:JavaDiagnosticsTest",
)

bazel_integration_test_all_versions(
    name = "allow_manual_targets_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/allow-manual-targets-sync-project",
    project_path = "test-projects/workspace/allow-manual-targets-sync-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspAllowManualTargetsSyncTest",
)

bazel_integration_test_all_versions(
    name = "partial_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/sample-repo",
    project_path = "test-projects/workspace/sample-repo",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspPartialSyncTest",
)

bazel_integration_test_all_versions(
    name = "build_and_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/build-and-sync-test",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspBuildAndSyncTest",
)

bazel_integration_test_all_versions(
    name = "first_phase_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/first-phase-sync-project",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelBspFirstPhaseSyncTest",
)

bazel_integration_test_all_versions(
    name = "nested_modules_test",
    bzlmod_project_path = "test-projects/bzlmod/nested-modules",
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:NestedModulesTest",
)

bazel_integration_test_all_versions(
    name = "external_autoloads_test",
    bzlmod_project_path = "test-projects/bzlmod/external-autoloads-project",
    exclude_bazel_7 = True,
    test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:ExternalAutoloadsTest",
)

test_suite(
    name = "all_tests",
    tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
    # we need to include all of them manually because of the "manual" tag
    tests = [
        ":allow_manual_targets_sync_test",
        ":android_kotlin_project_test",
        ":android_project_test",
        ":build_and_sync_test",
        ":enabled_rules_test",
        ":external_autoloads_test",
        ":first_phase_sync_test",
        ":go_project_test",
        ":java_diagnostics_test",
        ":kotlin_project_test",
        ":kotlin_project_with_enabled_rules_test",
        ":local_jdk_test",
        ":nested_modules_test",
        ":partial_sync_test",
        ":python_project_test",
        ":remote_jdk_test",
        ":sample_repo_test",
        ":server_downloads_bazelisk_test",
    ],
)

# Just a reminder - have you read the note at the top of this file?
