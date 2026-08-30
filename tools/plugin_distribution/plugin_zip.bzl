load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")
load("@community//platform/build-scripts/bazel-rules:dev_dist_content.bzl", "DevDistContentInfo")

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

def _plugin_directory_name(main_module):
    """The archive directory the packager gives a plugin whose layout names none.

    `PluginLayout` strips the `intellij.` prefix and puts a dash where each dot was. It names the main jar after the
    same string. `UltimateRepositoryModules` registers `intellij.bazel.plugin` with a plain `pluginAuto`, so the plugin
    takes both defaults.
    """
    if main_module.startswith("intellij."):
        main_module = main_module[len("intellij."):]
    return main_module.replace(".", "-")

_IMPLICIT_LIBRARY_ATTR = attr.label(
    doc = """The library a content leaf carries whether or not the plugin declares it.

`_collect_libraries` in `dev_dist_content.bzl` appends the Kotlin standard library to every plugin. The converter puts
it into a module's `runtime_deps`, and no plugin says anything by naming it. The platform ships it, so a plugin archive
holds no file for it. Named again here rather than read off the leaf. A different implicit library then stops matching
this label, and the check below refuses the content.""",
    default = "@lib//:kotlin-stdlib",
)

def _expected_zip_entries(label, implicit_library, content_info):
    """Every file the plugin archive must hold, read off the plugin's dev-distribution content.

    The content leaf is the plugin's checked-in record of what it is made of, generated from the project model. Each
    prepacked relation carries the `lib/`-relative path the plugin puts that jar at, so the leaf already names the
    archive layout. The production packager computes that layout again, from the JPS model and its own conventions. It
    reads no relation. A production build leaves `prepackedPluginContent` empty, and only a dev build fills it. So the
    two sides have two producers, and this comparison is a real one.

    The main module's jar is the one file no relation names. The packager puts it at the layout's main jar name, which
    is the archive directory plus `.jar`. See `_plugin_directory_name`.
    """
    prepacked = content_info.prepacked_plugin_jars.to_list()
    if not prepacked:
        fail("%s: the content names no prepacked jar, so it names no file of the archive" % label)

    # A member that is not prepacked reaches the archive inside a jar the content does not name. The rule would then
    # pass an archive it checked less of. The main module is the one such member with a derived path, and `module_jars`
    # always holds its jar.
    module_jars = content_info.module_jars.to_list()
    declared_libraries = [
        entry.label
        for entry in content_info.library_jars.to_list()
        if entry.label != str(implicit_library.label)
    ]
    if len(module_jars) != 1 or declared_libraries:
        fail(
            "%s: the content names %d module jars and the libraries %s, and only the main module's jar has a derived path" %
            (label, len(module_jars), declared_libraries),
        )

    main_modules = sorted({record.plugin_main_module: True for record in prepacked}.keys())
    if len(main_modules) != 1:
        fail("%s: the content names the jars of %s, and one archive holds one plugin" % (label, main_modules))

    directory = _plugin_directory_name(main_modules[0])
    entries = ["%s/lib/%s.jar" % (directory, directory)]
    for record in prepacked:
        entries.append("%s/lib/%s" % (directory, record.relative_output_file))
    return sorted(entries)

def _bazel_plugin_layout_test_impl(ctx):
    plugin_zip = _single_file(ctx.attr.plugin_zip, "plugin_zip")
    checker = ctx.executable._checker
    expected_entries = ctx.actions.declare_file(ctx.label.name + "_expected_entries.txt")
    test_script = ctx.actions.declare_file(ctx.label.name)

    ctx.actions.write(
        output = expected_entries,
        content = "\n".join(_expected_zip_entries(
            label = ctx.label,
            implicit_library = ctx.attr._implicit_library,
            content_info = ctx.attr.plugin_content[DevDistContentInfo],
        )) + "\n",
    )

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
  --expected_entries "$(resolve_runfile "{expected_entries}")" \\
  --plugin_zip "$(resolve_runfile "{plugin_zip}")"
""".format(
            checker = _runfile_path(checker),
            expected_entries = _runfile_path(expected_entries),
            plugin_zip = _runfile_path(plugin_zip),
        ),
    )

    runfiles = ctx.runfiles(files = [checker, expected_entries, plugin_zip])
    runfiles = runfiles.merge(ctx.attr._checker[DefaultInfo].default_runfiles)
    runfiles = runfiles.merge(ctx.attr.plugin_zip[DefaultInfo].default_runfiles)

    return [DefaultInfo(executable = test_script, runfiles = runfiles)]

bazel_plugin_layout_test = rule(
    implementation = _bazel_plugin_layout_test_impl,
    test = True,
    attrs = {
        "plugin_content": attr.label(
            doc = "The plugin's `dev_dist_plugin_content` target, which names the jars the archive must hold.",
            mandatory = True,
            providers = [DevDistContentInfo],
        ),
        "plugin_zip": attr.label(mandatory = True, allow_single_file = True),
        "_implicit_library": _IMPLICIT_LIBRARY_ATTR,
        "_checker": attr.label(
            default = Label("//plugins/bazel/tools/plugin_distribution:check_plugin_zip_layout"),
            executable = True,
            cfg = "target",
        ),
    },
)
