"""The loose files of an installed plugin, at both places where the platform looks for them."""

def _lib_copy_path(plugin_dir, loose_file):
    """plugins/foo/bin/helper -> plugins/foo/lib/bin/helper"""
    return "%s/lib/%s" % (plugin_dir, loose_file[len(plugin_dir) + 1:])

def plugin_bundled_files(name, plugin_dirs):
    """A filegroup with every plugin file outside lib/, plus a copy of each under lib/.

    Under Bazel tests the platform resolves plugin resources as <jar>/../.., which lands on the plugin dir
    for lib/x.jar but on lib/ for lib/modules/x.jar. Put the files at both.
    """
    group_srcs = []
    for plugin_dir in plugin_dirs:
        bundled_files = native.glob([plugin_dir + "/**"], exclude = [plugin_dir + "/lib/**"], allow_empty = True)
        if not bundled_files:
            continue

        copy_to = {f: _lib_copy_path(plugin_dir, f) for f in bundled_files}
        copy_rule = "%s_%s_under_lib" % (name, plugin_dir.replace("/", "_"))
        native.genrule(
            name = copy_rule,
            srcs = bundled_files,
            outs = copy_to.values(),
            cmd = "\n".join([
                "cp $(execpath %s) $(RULEDIR)/%s" % (loose_file, lib_copy)
                for loose_file, lib_copy in copy_to.items()
            ]),
        )

        group_srcs += bundled_files + [":" + copy_rule]

    native.filegroup(name = name, srcs = group_srcs)
