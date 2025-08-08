load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load("@bazel_skylib//lib:paths.bzl", "paths")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "bazel_integration_test", "bazel_integration_tests", "integration_test_utils")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

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

def bazel_integration_test_all_versions(name, test_runner, project_path = None, bzlmod_project_path = None, env = {}, additional_env_inherit = [], exclude_bazel_7 = False):
    bazel_versions = []
    envs = " ".join(["{}={}".format(k, v) for k, v in env.items()])
    test_names = []
    if project_path != None:
        workspace_bazel_versions = ["6.4.0"]
        bazel_versions = workspace_bazel_versions
        workspace_path = _convey_test_sources(
            name = name + "_workspace",
            project_path = project_path,
        )
        for bazel_version in bazel_versions:
            test_name = _testBazel(
                name = name + "_workspace",
                bazel_version = bazel_version,
                test_runner = test_runner,
                workspace_file_path = workspace_path.workspace_file_path,
                workspace_filegroup = workspace_path.workspace_filegroup,
                envs = envs,
            )
            test_names = [test_name]

    if bzlmod_project_path != None:
        bzlmod_bazel_versions = ["8.0.0"]
        if not exclude_bazel_7:
            bzlmod_bazel_versions.append("7.4.0")
        bazel_versions += bzlmod_bazel_versions
        blzmod_name = name + "_bzlmod"
        workspace_bzlmod = _convey_test_sources(
            name = blzmod_name,
            project_path = bzlmod_project_path,
        )
        for bazel_version in bazel_versions:
            test_name = _testBazel(
                name = name,
                bazel_version = bazel_version,
                test_runner = test_runner,
                workspace_file_path = workspace_bzlmod.workspace_file_path,
                workspace_filegroup = workspace_bzlmod.workspace_filegroup,
                envs = envs,
            )
            test_names += [test_name]

    native.test_suite(
        name = name,
        tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
        tests = test_names,
    )

    for old_test_name in integration_test_utils.bazel_integration_test_names(name, bazel_versions):
        new_name = _calculate_new_version_name(old_test_name)

        native.test_suite(
            name = new_name,
            tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
            tests = [old_test_name],
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

def _testBazel(name, bazel_version, test_runner, workspace_file_path, workspace_filegroup, envs):
    rule_name = integration_test_utils.bazel_integration_test_name(
        name,
        bazel_version,
    )
    bazel = bazel_binaries.label(bazel_version)
    genrule_name = rule_name + "_genrule"
    native.genrule(
        name = genrule_name,
        testonly = True,
        outs = ["{}_actual_outputs.txt".format(rule_name)],
        tools = [test_runner, bazel, workspace_file_path, workspace_filegroup],
        cmd = "{} BIT_WORKSPACE_DIR=$(location {}) BIT_BAZEL_BINARY=$(location {}) $(location {}) > $@".format(envs, workspace_file_path, bazel, test_runner),
    )

    expected_outputs_name = rule_name + "_expected_outputs"
    find_expected_output(
        name = expected_outputs_name,
        srcs = workspace_filegroup,
        suffix = integration_test_utils.semantic_version_to_name(bazel_version),
        testonly = True,
    )

    diff_test(
        name = rule_name,
        file1 = ":{}".format(genrule_name),
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

def bazel_integration_test_current_version(name, test_runner, project_path, env = {}, additional_env_inherit = []):
    bazel_integration_test(
        name = name,
        timeout = "eternal",
        bazel_version = bazel_binaries.versions.current,
        test_runner = test_runner,
        workspace_path = project_path,
        env = env,
        additional_env_inherit = additional_env_inherit,
    )
