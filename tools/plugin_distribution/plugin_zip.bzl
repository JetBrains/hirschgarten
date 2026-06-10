load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _bazel_plugin_zip_impl(ctx):
    output = ctx.actions.declare_file(ctx.attr.zip_filename)
    args = ctx.actions.args()
    args.add("--output", output)
    args.add("--platform", ctx.attr._platform_version[BuildSettingInfo].value)

    ctx.actions.run(
        executable = ctx.executable._builder,
        arguments = [args],
        inputs = ctx.files.versions_files,
        outputs = [output],
        mnemonic = "BazelPluginZip",
        progress_message = "Creating Bazel plugin distribution zip",
    )

    return [DefaultInfo(files = depset([output]))]

bazel_plugin_zip = rule(
    implementation = _bazel_plugin_zip_impl,
    attrs = {
        "versions_files": attr.label_list(mandatory = True, allow_files = [".bzl"]),
        "zip_filename": attr.string(mandatory = True),
        "_platform_version": attr.label(default = Label("//plugins/bazel:platform_version")),
        "_builder": attr.label(
            default = Label("//build:bazel_plugin_build_target"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _single_file(target, attr_name):
    files = target.files.to_list()
    if len(files) != 1:
        fail("%s must point to a single file, got %s files from %s" % (attr_name, len(files), target.label))
    return files[0]

def _runfile_path(file):
    if file.short_path.startswith("../"):
        return file.short_path[3:]
    return file.short_path

def _bazel_plugin_layout_test_impl(ctx):
    plugin_content_yaml = _single_file(ctx.attr.plugin_content_yaml, "plugin_content_yaml")
    plugin_zip = _single_file(ctx.attr.plugin_zip, "plugin_zip")
    checker = ctx.executable._checker
    test_script = ctx.actions.declare_file(ctx.label.name)

    ctx.actions.write(
        output = test_script,
        is_executable = True,
        content = """#!/usr/bin/env bash
set -euo pipefail

resolve_runfile() {{
  local path="$1"
  if [[ -n "${{RUNFILES_DIR:-}}" && -e "$RUNFILES_DIR/$path" ]]; then
    printf '%s\\n' "$RUNFILES_DIR/$path"
  elif [[ -e "$path" ]]; then
    printf '%s\\n' "$path"
  else
    printf 'Cannot resolve runfile: %s\\n' "$path" >&2
    exit 1
  fi
}}

"$(resolve_runfile "{checker}")" \\
  --plugin_content_yaml "$(resolve_runfile "{plugin_content_yaml}")" \\
  --plugin_zip "$(resolve_runfile "{plugin_zip}")" \\
  --plugin_root "{plugin_root}"
""".format(
            checker = _runfile_path(checker),
            plugin_content_yaml = _runfile_path(plugin_content_yaml),
            plugin_zip = _runfile_path(plugin_zip),
            plugin_root = ctx.attr.plugin_root,
        ),
    )

    runfiles = ctx.runfiles(files = [checker, plugin_content_yaml, plugin_zip])
    runfiles = runfiles.merge(ctx.attr._checker[DefaultInfo].default_runfiles)
    runfiles = runfiles.merge(ctx.attr.plugin_zip[DefaultInfo].default_runfiles)

    return [DefaultInfo(executable = test_script, runfiles = runfiles)]

bazel_plugin_layout_test = rule(
    implementation = _bazel_plugin_layout_test_impl,
    test = True,
    attrs = {
        "plugin_content_yaml": attr.label(mandatory = True, allow_single_file = True),
        "plugin_root": attr.string(default = "bazel-plugin"),
        "plugin_zip": attr.label(mandatory = True, allow_single_file = True),
        "_checker": attr.label(
            default = Label("//plugins/bazel/tools/plugin_distribution:check_plugin_zip_layout"),
            executable = True,
            cfg = "target",
        ),
    },
)
