load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load("@bazel_skylib//lib:paths.bzl", "paths")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "integration_test_utils")
load("@rules_shell//shell:sh_test.bzl", "sh_test")

wrapper_script = "@//rules/bazel_integration_test:acceptance_test_wrapper.sh"

def _find_workspace_file(ctx):
    workspace_file = paths.join(ctx.attr.workspace_path, "WORKSPACE")
    workspace_module = paths.join(ctx.attr.workspace_path, "MODULE.bazel")
    for file in ctx.attr.srcs[DefaultInfo].files.to_list():
        if file.path.endswith(workspace_file) or file.path.endswith(workspace_module):
            return [DefaultInfo(files = depset([file]))]

def _find_expected_output(ctx):
    for file in ctx.attr.srcs[DefaultInfo].files.to_list():
        if file.path.endswith("expected_output_{}.txt".format(ctx.attr.suffix)):
            return [DefaultInfo(files = depset([file]))]

find_workspace_file = rule(
    implementation = _find_workspace_file,
    attrs = {
        "srcs": attr.label(
            allow_files = True,
        ),
        "workspace_path": attr.string(),
    },
)

find_expected_output = rule(
    implementation = _find_expected_output,
    attrs = {
        "srcs": attr.label(
            allow_files = True,
        ),
        "suffix": attr.string(),
    },
)

def bazel_aspects_output_test_all_versions(
        name,
        project_path = None,
        bzlmod_project_path = None,
        env = {},
        inherited_env_names = [],
        enabled_rules = [],
        targets = [],
        gazelle_target = None,
        skipped_versions_for_bzlmod = []):
    bazel_acceptance_test_all_versions(
        name = name,
        test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelTestRunner",
        project_path = project_path,
        bzlmod_project_path = bzlmod_project_path,
        env = env,
        inherited_env_names = inherited_env_names,
        enabled_rules = enabled_rules,
        targets = targets,
        gazelle_target = gazelle_target,
        skipped_versions_for_bzlmod = skipped_versions_for_bzlmod,
        remove_system_specific_aspect_output = True,
    )

def bazel_diagnostics_output_test_all_versions(
        name,
        project_path = None,
        bzlmod_project_path = None,
        env = {},
        inherited_env_names = [],
        enabled_rules = [],
        targets = [],
        compile_targets = [],
        gazelle_target = None,
        skipped_versions_for_bzlmod = []):
    bazel_acceptance_test_all_versions(
        name = name,
        test_runner = "//server/e2e/src/main/kotlin/org/jetbrains/bazel:BazelTestRunnerWithDiagnostics",
        project_path = project_path,
        bzlmod_project_path = bzlmod_project_path,
        env = env,
        compile_targets = compile_targets,
        inherited_env_names = inherited_env_names,
        enabled_rules = enabled_rules,
        targets = targets,
        gazelle_target = gazelle_target,
        skipped_versions_for_bzlmod = skipped_versions_for_bzlmod,
        check_diagnostics = True,
    )

def bazel_acceptance_test_all_versions(
        name,
        test_runner,
        project_path = None,
        bzlmod_project_path = None,
        env = {},
        inherited_env_names = [],
        enabled_rules = [],
        targets = [],
        compile_targets = [],
        gazelle_target = None,
        skipped_versions_for_bzlmod = [],
        remove_system_specific_aspect_output = False,
        check_diagnostics = False):
    envs = env or {}
    if targets:
        envs["TARGETS"] = ",".join(targets)
    if enabled_rules:
        envs["ENABLED_RULES"] = ",".join(enabled_rules)
    if gazelle_target:
        envs["GAZELLE_TARGET"] = gazelle_target
    for i in range(len(compile_targets)):
        envs["COMPILE_TARGETS_{}".format(i)] = ",".join(compile_targets[i])
    test_names = []
    if project_path != None:
        workspace_bazel_versions = ["6.4.0"]
        workspace_path = _convey_test_sources(
            name = name + "_workspace",
            project_path = project_path,
        )
        for bazel_version in workspace_bazel_versions:
            test_name = _testBazel(
                name = name + "_workspace",
                bazel_version = bazel_version,
                test_runner = test_runner,
                workspace_file_path = workspace_path.workspace_file_path,
                workspace_filegroup = workspace_path.workspace_filegroup,
                envs = envs,
                inherited_env_names = inherited_env_names,
                remove_system_specific_aspect_output = remove_system_specific_aspect_output,
                check_diagnostics = check_diagnostics,
            )
            test_names = [test_name]

    if bzlmod_project_path != None:
        bzlmod_bazel_versions = []
        if "8.0.0" not in skipped_versions_for_bzlmod:
            bzlmod_bazel_versions = ["8.0.0"]
        if "7.4.0" not in skipped_versions_for_bzlmod:
            bzlmod_bazel_versions += ["7.4.0"]
        if "6.4.0" not in skipped_versions_for_bzlmod:
            bzlmod_bazel_versions += ["6.4.0"]
        workspace_bzlmod = _convey_test_sources(
            name = name + "_bzlmod",
            project_path = bzlmod_project_path,
        )
        for bazel_version in bzlmod_bazel_versions:
            test_name = _testBazel(
                name = name,
                bazel_version = bazel_version,
                test_runner = test_runner,
                workspace_file_path = workspace_bzlmod.workspace_file_path,
                workspace_filegroup = workspace_bzlmod.workspace_filegroup,
                envs = envs,
                inherited_env_names = inherited_env_names,
                remove_system_specific_aspect_output = remove_system_specific_aspect_output,
                check_diagnostics = check_diagnostics,
            )
            test_names += [test_name]

    native.test_suite(
        name = name,
        tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
        tests = test_names,
    )

def _convey_test_sources(name, project_path):
    workspace_files = integration_test_utils.glob_workspace_files(project_path)
    workspace_filegroup = name + "_filegroup"
    native.filegroup(
        name = workspace_filegroup,
        srcs = workspace_files,
        testonly = True,
    )
    workspace_file_path = name + "_bazel_workspace_file"
    find_workspace_file(
        name = workspace_file_path,
        srcs = workspace_filegroup,
        workspace_path = project_path,
        testonly = True,
    )
    return struct(
        workspace_filegroup = workspace_filegroup,
        workspace_file_path = workspace_file_path,
    )

def _testBazel(
        name,
        bazel_version,
        test_runner,
        workspace_file_path,
        workspace_filegroup,
        envs,
        inherited_env_names,
        check_diagnostics,
        remove_system_specific_aspect_output):
    rule_name = integration_test_utils.bazel_integration_test_name(
        name,
        bazel_version,
    )
    bazel = bazel_binaries.label(bazel_version)
    expected_outputs = rule_name + "_expected_outputs"
    find_expected_output(
        name = expected_outputs,
        srcs = workspace_filegroup,
        suffix = rule_name,
        testonly = True,
    )
    args = [
        "--bazel",
        "$(location {})".format(bazel),
        "--test_runner",
        "$(location {})".format(test_runner),
        "--workspace",
        "$(location {})".format(workspace_file_path),
        "--expected_outputs",
        "$(location {})".format(expected_outputs),
    ]
    if remove_system_specific_aspect_output:
        args.extend(["--escape-aspects-output"])
    if check_diagnostics:
        args.extend(["--check-diagnostics"])
    sh_test(
        name = rule_name,
        args = args,
        srcs = [wrapper_script],
        data = [bazel, test_runner, workspace_file_path, workspace_filegroup, expected_outputs],
        env = envs,
        env_inherit = inherited_env_names,
        tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
    )

    return rule_name

def _calculate_new_version_name(old_name):
    if old_name.endswith(".bazelversion"):
        return old_name.removesuffix(".bazelversion") + "current"
    else:
        name_of_target_only_with_major, _, _ = old_name.rsplit("_", 2)
        return name_of_target_only_with_major + "_x"
