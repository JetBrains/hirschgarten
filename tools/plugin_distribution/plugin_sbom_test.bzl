load("@rules_java//java:defs.bzl", "JavaInfo")

PackedJarsInfo = provider(
    doc = "The jars that the modules of a plugin pack, as `ij_plugin_module` names them in `packed_deps`.",
    fields = {"jars": "depset of File"},
)

def _packed_jars_aspect_impl(target, ctx):
    transitive = []
    for edge in ["content_modules", "descriptor_module"]:
        value = getattr(ctx.rule.attr, edge, None)
        for dep in (value if type(value) == "list" else [value] if value else []):
            transitive.append(dep[PackedJarsInfo].jars)

    # The same jars that `_ij_plugin_module_impl` hands to the packager.
    direct = []
    for dep in getattr(ctx.rule.attr, "packed_deps", []):
        java_info = dep[JavaInfo]
        if hasattr(java_info, "output_jar"):
            direct.append(java_info.output_jar)
        else:
            direct.extend(java_info.transitive_runtime_jars.to_list())
    return [PackedJarsInfo(jars = depset(direct, transitive = transitive))]

_packed_jars_aspect = aspect(
    implementation = _packed_jars_aspect_impl,
    attr_aspects = ["content_modules", "descriptor_module"],
)

def _plugin_sbom_test_impl(ctx):
    packed_jars = ctx.actions.declare_file(ctx.label.name + "_packed_jars.txt")
    names = sorted({jar.basename: True for jar in ctx.attr.plugin[PackedJarsInfo].jars.to_list()}.keys())
    ctx.actions.write(packed_jars, "".join([name + "\n" for name in names]))

    test_script = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(
        output = test_script,
        is_executable = True,
        content = """#!/usr/bin/env bash
set -euo pipefail

sbom="{sbom}"
packed="$(sort "{packed_jars}")"
listed="$(grep -o '"pkg:maven/[^"]*"' "$sbom" | sed -E 's|"pkg:maven/[^/]+/([^@]+)@([^"]+)"|\\1-\\2.jar|' | sort || true)"

if [[ "$packed" != "$listed" ]]; then
  echo "The plugin packs other jars than $sbom lists."
  echo "Add a package with a Maven purl to the SBOM for each new jar, and remove the package of each jar that the plugin no longer packs."
  diff <(echo "$listed") <(echo "$packed") | sed -e 's/^</  only in the SBOM:/' -e 's/^>/  only in the plugin:/' | grep '^  ' || true
  exit 1
fi
""".format(sbom = ctx.file.sbom.short_path, packed_jars = packed_jars.short_path),
    )
    return [DefaultInfo(executable = test_script, runfiles = ctx.runfiles(files = [ctx.file.sbom, packed_jars]))]

plugin_sbom_test = rule(
    doc = """Checks that the SBOM of a plugin lists a Maven package for each jar that the plugin packs.

    The packed jars are the `packed_deps` of the plugin modules. The test matches a jar by its Maven file name,
    `<artifact>-<version>.jar`. A library that the IDE supplies is not packed, so the SBOM of the IDE lists it.""",
    implementation = _plugin_sbom_test_impl,
    test = True,
    attrs = {
        "plugin": attr.label(
            doc = "The `ij_plugin` target.",
            mandatory = True,
            aspects = [_packed_jars_aspect],
        ),
        "sbom": attr.label(
            doc = "The SPDX JSON file of the plugin.",
            mandatory = True,
            allow_single_file = [".json"],
        ),
    },
)
