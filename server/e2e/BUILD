load("@//rules/bazel_integration_test:defs.bzl", "bazel_aspects_output_test_all_versions", "bazel_diagnostics_output_test_all_versions")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "integration_test_utils")

package(default_visibility = ["//visibility:public"])

####################
# SUPER IMPORTANT! #
####################
# Wheneven you add a new test, make sure to run `bazel run @rules_bazel_integration_test//tools:update_deleted_packages`

bazel_aspects_output_test_all_versions(
    name = "sample_repo_test",
    bzlmod_project_path = "test-projects/bzlmod/sample-repo",
    enabled_rules = [
        "io_bazel_rules_scala",
        "rules_java",
        "rules_jvm",
    ],
    env = {
        "foo1": "invalid_val1",
        "foo2": "invalid_val2",
        "foo3": "val3",
        "foo4": "val4",
    },
    project_path = "test-projects/workspace/sample-repo",
    # Rules MODULE uses transitively rules_jvm_external@6.3 which is not supported by bazel 6.4.0
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
)

bazel_diagnostics_output_test_all_versions(
    name = "java_diagnostics_test",
    bzlmod_project_path = "test-projects/bzlmod/java-diagnostics-project",
    compile_targets = [
        ["@//:deprecated_warning"],
        [
            "@//:no_such_method_error",
            "@//:warning_and_error",
        ],
    ],
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
)

bazel_aspects_output_test_all_versions(
    name = "sample_repo_partial_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/sample-repo",
    enabled_rules = [
        "io_bazel_rules_scala",
        "rules_java",
        "rules_jvm",
    ],
    env = {
        "foo1": "invalid_val1",
        "foo2": "invalid_val2",
        "foo3": "val3",
        "foo4": "val4",
    },
    # Rules MODULE uses transitively rules_jvm_external@6.3 which is not supported by bazel 6.4.0
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
    targets = ["@//java_targets:java_library"],
)

bazel_aspects_output_test_all_versions(
    name = "local_jdk_test",
    bzlmod_project_path = "test-projects/bzlmod/local-jdk-project",
    inherited_env_names = ["JAVA_HOME"],
    project_path = "test-projects/workspace/local-jdk-project",
)

bazel_aspects_output_test_all_versions(
    name = "remote_jdk_test",
    bzlmod_project_path = "test-projects/bzlmod/remote-jdk-project",
    project_path = "test-projects/workspace/remote-jdk-project",
)

bazel_aspects_output_test_all_versions(
    name = "python_project_test",
    bzlmod_project_path = "test-projects/bzlmod/python-project",
    project_path = "test-projects/workspace/python-project",
    # Rules python in MODULE uses transitively rules_jvm_external@6.3 which is not supported by bazel 6.4.0
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
)

bazel_aspects_output_test_all_versions(
    name = "kotlin_project_test",
    bzlmod_project_path = "test-projects/bzlmod/kotlin-project",
    project_path = "test-projects/workspace/kotlin-project",
)

bazel_aspects_output_test_all_versions(
    name = "kotlin_project_with_enabled_rules_test",
    bzlmod_project_path = "test-projects/bzlmod/kotlin-project",
    enabled_rules = ["rules_kotlin"],
    project_path = "test-projects/workspace/kotlin-project",
)

bazel_aspects_output_test_all_versions(
    name = "enabled_rules_test",
    enabled_rules = [
        "io_bazel_rules_scala",
        "rules_java",
        "rules_jvm",
    ],
    project_path = "test-projects/workspace/enabled-rules-project",
)

bazel_diagnostics_output_test_all_versions(
    name = "scala_diagnostics_test",
    compile_targets = [
        ["//scala_targets:library"],
        ["//scala_targets:library"],
    ],
    enabled_rules = [
        "io_bazel_rules_scala",
        "rules_java",
        "rules_jvm",
    ],
    project_path = "test-projects/workspace/enabled-rules-project",
)

bazel_aspects_output_test_all_versions(
    name = "go_project_test",
    bzlmod_project_path = "test-projects/bzlmod/go-project",
    enabled_rules = ["io_bazel_rules_go"],
    gazelle_target = "//:gazelle",
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
)

bazel_aspects_output_test_all_versions(
    name = "build_and_sync_test",
    bzlmod_project_path = "test-projects/bzlmod/build-and-sync-test",
    skipped_versions_for_bzlmod = [
        "6.4.0",
    ],
)

# bazel_aspects_output_test_all_versions(
#     name = "nested_modules_test",
#     bzlmod_project_path = "test-projects/bzlmod/nested-modules",
#     targets = [
#         "@//...",
#         "@inner//...",
#     ],
# )

bazel_aspects_output_test_all_versions(
    name = "external_autoloads_test",
    bzlmod_project_path = "test-projects/bzlmod/external-autoloads-project",
    skipped_versions_for_bzlmod = [
        "6.4.0",
        "7.4.0",
    ],
)

test_suite(
    name = "all_tests",
    tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
    # we need to include all of them manually because of the "manual" tag
    tests = [
        ":build_and_sync_test",
        ":enabled_rules_test",
        ":external_autoloads_test",
        ":go_project_test",
        ":kotlin_project_test",
        ":kotlin_project_with_enabled_rules_test",
        ":local_jdk_test",
        # ":nested_modules_test",
        ":python_project_test",
        ":remote_jdk_test",
        ":sample_repo_partial_sync_test",
        ":sample_repo_test",
        ":java_diagnostics_test",
        ":scala_diagnostics_test",
    ],
)

# Just a reminder - have you read the note at the top of this file?
