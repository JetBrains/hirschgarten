load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load("@bazel_skylib//lib:paths.bzl", "paths")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "integration_test_utils")

escape_script = "@//rules/bazel_integration_test:escape_system_specifics.sh"

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

def _read_env_vars(ctx):
    output = ctx.actions.declare_file(ctx.label.name + ".txt")
    cmd = " ".join(["{0}=${0}".format(env_var) for env_var in ctx.attr.names])
    ctx.actions.run_shell(
        outputs = [output],
        command = "echo {} >> {}".format(cmd, output.path),
        use_default_shell_env = True,
    )
    return [DefaultInfo(files = depset([output]))]

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

read_env_vars = rule(
    implementation = _read_env_vars,
    attrs = {
        "names": attr.string_list(),
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
    envs = ["{}=\"{}\"".format(k, v) for k, v in env.items()]
    if targets:
        envs += ["TARGETS=\"" + ",".join(targets) + "\""]
    if enabled_rules:
        envs += ["ENABLED_RULES=\"" + ",".join(enabled_rules) + "\""]
    if gazelle_target:
        envs += ["GAZELLE_TARGET=\"" + gazelle_target + "\""]
    for i in range(len(compile_targets)):
        envs += ["COMPILE_TARGETS_{}=\"{}\"".format(i, ",".join(compile_targets[i]))]
    all_envs = " ".join(envs)
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
                envs = all_envs,
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
                envs = all_envs,
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
    inherited_envs = rule_name + "_inherited_vars"
    read_env_vars(
        name = inherited_envs,
        names = inherited_env_names,
    )
    bazel = bazel_binaries.label(bazel_version)
    genrule_name = rule_name + "_genrule"
    native.genrule(
        name = genrule_name,
        testonly = True,
        srcs = [inherited_envs],
        outs = ["{}_actual_outputs.txt".format(rule_name)],
        tools = [test_runner, bazel, workspace_file_path, workspace_filegroup],
        cmd = """
            set -euo pipefail
            set -a
            . $(location {inherited_envs})
            set +a
            BIT_WORKSPACE_DIR=$(location {workspace_file_path}) BIT_BAZEL_BINARY=$(location {bazel}) {envs} $(location {test_runner}) > $@
        """.format(
            inherited_envs = inherited_envs,
            workspace_file_path = workspace_file_path,
            bazel = bazel,
            envs = envs,
            test_runner = test_runner,
        ),
    )

    expected_outputs_name = rule_name + "_expected_outputs"
    find_expected_output(
        name = expected_outputs_name,
        srcs = workspace_filegroup,
        suffix = rule_name,
        testonly = True,
    )
    espaced_genrule = genrule_name + "_espaced"
    flags = []
    if not remove_system_specific_aspect_output:
        flags += ["--no-aspects-output"]
    if check_diagnostics:
        flags += ["--check-diagnostics"]
    flag_args = " ".join(flags)
    native.genrule(
        name = espaced_genrule,
        srcs = [genrule_name],
        outs = ["{}_actual_outputs_escaped.txt".format(rule_name)],
        tools = [escape_script],
        testonly = True,
        cmd = "$(location {}) $(location {}) {} > $@".format(escape_script, genrule_name, flag_args),
    )

    diff_test(
        name = rule_name,
        file1 = ":{}".format(espaced_genrule),
        file2 = ":{}".format(expected_outputs_name),
        tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
    )

    return rule_name

def _calculate_new_version_name(old_name):
    if old_name.endswith(".bazelversion"):
        return old_name.removesuffix(".bazelversion") + "current"
    else:
        name_of_target_only_with_major, _, _ = old_name.rsplit("_", 2)
        return name_of_target_only_with_major + "_x"
