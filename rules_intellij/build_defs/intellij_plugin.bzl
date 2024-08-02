#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#

"""IntelliJ plugin target rule.

Creates a plugin jar with the given plugin xml and any
optional plugin xmls.

To provide optional plugin xmls, use the 'optional_plugin_dep'
rule. These will be renamed, put in the META-INF directory,
and the main plugin xml stamped with optional plugin dependencies
that point to the correct META-INF optional plugin xmls.

To associate a plugin.xml fragment with some code, you can add both to an
intellij_plugin_library rule and add that target as a dependency of an
intellij_plugin. The XML will get merged into the main META-INF/plugin.xml
file.

optional_plugin_dep(
  name = "optional_python_xml",
  plugin_xml = "my_optional_python_plugin.xml",
  module = ["com.idea.python.module.id"],
)

intellij_plugin_library(
  name = "piper_support",
  plugin_xmls = ["META-INF/piper-plugin.xml"],
  deps = [":piper_support_lib"],
)

intellij_plugin(
  name = "my_plugin",
  plugin_xml = ["my_plugin.xml"],
  optional_plugin_deps = [":optional_python_xml"],
  deps = [
    ":code_deps",
    ":piper_support",
  ],
)

"""

load("@rules_java//java:defs.bzl", "java_binary", "java_import")
load(
    "//build_defs:restrictions.bzl",
    "RestrictedInfo",
    "restricted_deps_aspect",
    "validate_restrictions",
    "validate_unchecked_internal",
)

_OptionalPluginDepsInfo = provider(fields = ["optional_plugin_deps"])

def _optional_plugin_dep_impl(ctx):
    attr = ctx.attr
    optional_plugin_deps = []
    if ctx.file.plugin_xml:
        optional_plugin_deps.append(struct(
            plugin_xml = ctx.file.plugin_xml,
            module = attr.module,
        ))
    return [_OptionalPluginDepsInfo(optional_plugin_deps = optional_plugin_deps)]

optional_plugin_dep = rule(
    implementation = _optional_plugin_dep_impl,
    attrs = {
        "plugin_xml": attr.label(mandatory = True, allow_single_file = [".xml"]),
        "module": attr.string_list(mandatory = True),
    },
)

_IntellijPluginLibraryInfo = provider(fields = ["plugin_xmls", "optional_plugin_deps", "java_info", "plugin_deps"])

def _intellij_plugin_library_impl(ctx):
    java_info = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])

    plugin_xmls = []
    for target in ctx.attr.plugin_xmls:
        for file in target.files.to_list():
            plugin_xmls.append(file)

    plugin_deps = ctx.attr.plugin_deps

    return [
        _IntellijPluginLibraryInfo(
            plugin_xmls = depset(plugin_xmls, order = "preorder"),
            optional_plugin_deps = [
                dep[_OptionalPluginDepsInfo]
                for dep in ctx.attr.optional_plugin_deps
            ],
            java_info = java_info,
            plugin_deps = plugin_deps,
        ),
        java_info,
    ]

intellij_plugin_library = rule(
    implementation = _intellij_plugin_library_impl,
    attrs = {
        "deps": attr.label_list(providers = [JavaInfo]),
        "plugin_xmls": attr.label_list(allow_files = [".xml"]),
        "optional_plugin_deps": attr.label_list(providers = [_OptionalPluginDepsInfo]),
        "plugin_deps": attr.string_list(),
    },
)

def _merge_plugin_xmls(ctx):
    dep_plugin_xmls = []
    for dep in ctx.attr.deps:
        if _IntellijPluginLibraryInfo in dep:
            dep_plugin_xmls.append(dep[_IntellijPluginLibraryInfo].plugin_xmls)
    plugin_xmls = depset([ctx.file.plugin_xml], transitive = dep_plugin_xmls, order = "preorder")

    if len(plugin_xmls.to_list()) == 1:
        return plugin_xmls.to_list()[0]

    merged_name = "merged_plugin_xml_for_" + ctx.label.name + ".xml"
    merged_file = ctx.actions.declare_file(merged_name)
    ctx.actions.run(
        executable = ctx.executable._merge_xml_binary,
        arguments = ["--output", merged_file.path] + [xml.path for xml in plugin_xmls.to_list()],
        inputs = plugin_xmls,
        outputs = [merged_file],
        progress_message = "Merging plugin xmls",
        mnemonic = "MergePluginXmls",
    )
    return merged_file

def _synthetic_plugin_id(modules):
    return struct(name = "___".join(modules), is_synthetic = len(modules) > 1)

def _synthetic_optional_dep_file(ctx, modules):
    synthname = ctx.actions.declare_file("synthetic_" + "_".join(modules[:-1]) + ".xml")
    file = _filename_for_module_dependency(_synthetic_plugin_id(modules).name)
    ctx.actions.write(
        synthname,
        """
        <idea-plugin>
           <depends optional="true" config-file="{0}">{1}</depends>
        </idea-plugin>
        """.format(file, modules[-1]),
    )
    return synthname

"""
IntelliJ allows plugins to have code that is executed only if a particular external plugin is active. In order to do
this, one have to create a separate config XML file and include it with <depends optional=true> directive.

Unfortunately, the directive does not cover case when one wants to write code dependent on more than one plugin
i. e. make it active only if all required plugins are installed and enabled.

A trick to overcome this limitation is to create a synthetic file, which only purpose is to be conditionally activated
by a `<depends>first_plugin</depends>` directive on the and contain another <depends>second_plugin</depends> directive
for the second plugin.

Te purpose of `_create_dependency_file_chain` method is to implement this trick. It receives a `_OptionalPluginXmlInfo`
structure, which contains the xml file and N of its plugin dependencies. Then it creates actions to generate N-1
.xml files that make up the dependency chain.

It returns a dictionary, which values are the file names (including the original one from the provider), and its keys
are either module names, or synthetic module names it depends on. If a file is expected to contain a code
dependent on plugin.A and plugin.B, then its synthetic module name will be `module.A___module.B`.

Once the file is processed through _merge_xml_binary, its content is copied to `optional-module.A___module.B.xml' file.
This file will be included in `optional-module.A.xml` file via a dependents directive like this:
```
<depends optional="true" config-file="optional-module.A___module.B.xml">module.B</depends>
```

https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html#optional-plugin-dependencies
"""

def _create_dependency_file_chain(ctx, xml):
    module = sorted(xml.module)
    chained_files_dict = {}
    for i in range(1, len(module) + 1):
        synthname = _synthetic_optional_dep_file(ctx, module[:(i + 1)]) if i < len(module) else xml.plugin_xml
        chained_files_dict[_synthetic_plugin_id(module[:i])] = synthname
    return chained_files_dict

def _merge_plugin_deps(ctx):
    # Collect plugin deps for both deps and the plugin_deps attribute (and remove duplicates)
    plugin_deps = {}
    for plugin_dep in ctx.attr.plugin_deps:
        plugin_deps[plugin_dep] = True
    for dep in ctx.attr.deps:
        if _IntellijPluginLibraryInfo in dep:
            for plugin_dep in dep[_IntellijPluginLibraryInfo].plugin_deps:
                plugin_deps[plugin_dep] = True

    return list(plugin_deps.keys())

def _merge_optional_plugin_deps(ctx):
    # Collect optional plugin xmls for both deps and the optional_plugin_deps attribute
    module_to_xmls = {}
    optional_plugin_deps_providers = []
    for dep in ctx.attr.deps:
        if _IntellijPluginLibraryInfo in dep:
            optional_plugin_deps_providers.extend(
                dep[_IntellijPluginLibraryInfo].optional_plugin_deps,
            )
    optional_plugin_deps_providers.extend(
        [target[_OptionalPluginDepsInfo] for target in ctx.attr.optional_plugin_deps],
    )
    for provider in optional_plugin_deps_providers:
        for xml in provider.optional_plugin_deps:
            dependency_file_chain = _create_dependency_file_chain(ctx, xml)
            for module, plugin_dep in dependency_file_chain.items():
                plugin_deps = module_to_xmls.setdefault(module, [])
                plugin_deps.append(plugin_dep)

    # Merge xmls with the same module dependency
    module_to_merged_xmls = {}
    for module, plugin_xmls in module_to_xmls.items():
        merged_name = "merged_xml_for_" + module.name + "_" + ctx.label.name + ".xml"
        merged_file = ctx.actions.declare_file(merged_name)
        ctx.actions.run(
            executable = ctx.executable._merge_xml_binary,
            arguments = ["--output", merged_file.path] + [plugin_xml.path for plugin_xml in plugin_xmls],
            inputs = list(plugin_xmls),
            outputs = [merged_file],
            progress_message = "Merging optional xmls",
            mnemonic = "MergeOptionalXmls",
        )
        module_to_merged_xmls[module] = merged_file
    return module_to_merged_xmls

def _add_dependencies_to_plugin_xml(ctx, input_plugin_xml_file, optional_plugin_deps, plugin_deps):
    if not optional_plugin_deps and not plugin_deps:
        return input_plugin_xml_file

    # Add optional dependencies into the plugin xml
    args = []
    final_plugin_xml_file = ctx.actions.declare_file("final_plugin_xml_" + ctx.label.name + ".xml")
    args.extend(["--plugin_xml", input_plugin_xml_file.path])
    args.extend(["--output", final_plugin_xml_file.path])
    if plugin_deps:
        args.extend(["--plugin_deps"])
        for module in plugin_deps:
            args.append(module)
    args.append("--")
    for module in optional_plugin_deps:
        args.append(module)
        args.append(_filename_for_module_dependency(module))
    ctx.actions.run(
        executable = ctx.executable._append_optional_xml_elements,
        arguments = args,
        inputs = [input_plugin_xml_file],
        outputs = [final_plugin_xml_file],
        progress_message = "Adding dependencies to final plugin xml",
        mnemonic = "AddModuleDependencies",
    )
    return final_plugin_xml_file

def _filename_for_module_dependency(module):
    """A unique filename for the optional xml dependency for a given module."""
    return "optional-" + module + ".xml"

def _package_meta_inf_files(ctx, final_plugin_xml_file, module_to_merged_xmls):
    jar_name = ctx.attr.jar_name
    jar_file = ctx.actions.declare_file(jar_name)

    args = []
    args.extend(["--deploy_jar", ctx.file.deploy_jar.path])
    args.extend(["--output", jar_file.path])
    args.extend([final_plugin_xml_file.path, "plugin.xml"])
    for module, merged_xml in module_to_merged_xmls.items():
        args.append(merged_xml.path)
        args.append(_filename_for_module_dependency(module.name))
    for plugin_icon_file in ctx.files.plugin_icons:
        args.append(plugin_icon_file.path)
        args.append(plugin_icon_file.basename)
    ctx.actions.run(
        executable = ctx.executable._package_meta_inf_files,
        arguments = args,
        inputs = [ctx.file.deploy_jar, final_plugin_xml_file] + module_to_merged_xmls.values() + ctx.files.plugin_icons,
        outputs = [jar_file],
        mnemonic = "PackagePluginJar",
        progress_message = "Packaging plugin jar",
    )
    return jar_file

def _intellij_plugin_java_deps_impl(ctx):
    java_infos = [dep[_IntellijPluginLibraryInfo].java_info for dep in ctx.attr.deps]
    return [java_common.merge(java_infos)]

_intellij_plugin_java_deps = rule(
    implementation = _intellij_plugin_java_deps_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [[_IntellijPluginLibraryInfo]],
        ),
    },
)

def _intellij_plugin_jar_impl(ctx):
    augmented_xml = _merge_plugin_xmls(ctx)
    optional_module_to_merged_xmls = _merge_optional_plugin_deps(ctx)
    optional_modules = [k.name for k in optional_module_to_merged_xmls.keys() if not k.is_synthetic]

    modules = _merge_plugin_deps(ctx)

    final_plugin_xml_file = _add_dependencies_to_plugin_xml(ctx, augmented_xml, optional_modules, modules)
    jar_file = _package_meta_inf_files(ctx, final_plugin_xml_file, optional_module_to_merged_xmls)
    files = depset([jar_file])

    if ctx.attr.restrict_deps:
        dependencies = {}
        unchecked_transitive = []
        roots = []
        for k in ctx.attr.restricted_deps:
            if RestrictedInfo in k:
                dependencies.update(k[RestrictedInfo].dependencies)
                unchecked_transitive.append(k[RestrictedInfo].unchecked)
                roots.append(k[RestrictedInfo].roots)

        # Uncomment the next line to see all buildable roots:
        # fail("".join(["     " + str(t) + "\n" for t in depset(transitive=roots).to_list()]))
        validate_restrictions(dependencies)
        unchecked = [str(t.label) for t in depset(direct = [], transitive = unchecked_transitive).to_list()]
        validate_unchecked_internal(unchecked)

    return DefaultInfo(
        files = files,
    )

_intellij_plugin_jar = rule(
    implementation = _intellij_plugin_jar_impl,
    attrs = {
        "deploy_jar": attr.label(mandatory = True, allow_single_file = [".jar"]),
        "plugin_xml": attr.label(mandatory = True, allow_single_file = [".xml"]),
        "optional_plugin_deps": attr.label_list(providers = [_OptionalPluginDepsInfo]),
        "plugin_deps": attr.string_list(),
        "jar_name": attr.string(mandatory = True),
        "deps": attr.label_list(providers = [[_IntellijPluginLibraryInfo]]),
        "restrict_deps": attr.bool(),
        "restricted_deps": attr.label_list(aspects = [restricted_deps_aspect]),
        "plugin_icons": attr.label_list(allow_files = True),
        "_merge_xml_binary": attr.label(
            default = Label("@rules_intellij//build_defs:merge_xml"),
            executable = True,
            cfg = "exec",
        ),
        "_append_optional_xml_elements": attr.label(
            default = Label("@rules_intellij//build_defs:append_optional_xml_elements"),
            executable = True,
            cfg = "exec",
        ),
        "_package_meta_inf_files": attr.label(
            default = Label("@rules_intellij//build_defs:package_meta_inf_files"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def intellij_plugin(name, deps, plugin_xml, plugin_deps = [], optional_plugin_deps = [], jar_name = None, extra_runtime_deps = [], plugin_icons = [], restrict_deps = False, deploy_env = [], **kwargs):
    """Creates an intellij plugin from the given deps and plugin.xml.

    Args:
      name: The name of the target
      deps: Any java dependencies or intellij_plugin_library rules rolled up into the plugin jar.
      plugin_xml: An xml file to be placed in META-INF/plugin.jar
      optional_plugin_deps: A list of optional_plugin_dep targets.
      jar_name: The name of the final plugin jar, or <name>.jar if None
      extra_runtime_deps: runtime_deps added to java_binary or java_test calls
      plugin_icons: Plugin logo files to be placed in META-INF. Follow https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html#plugin-logo-requirements
      **kwargs: Any further arguments to be passed to the final target
    """
    java_deps_name = name + "_java_deps"
    binary_name = name + "_binary"
    deploy_jar = binary_name + "_deploy.jar"
    _intellij_plugin_java_deps(
        name = java_deps_name,
        deps = deps,
    )
    java_binary(
        name = binary_name,
        runtime_deps = [":" + java_deps_name] + extra_runtime_deps,
        create_executable = 0,
        deploy_env = deploy_env,
    )

    if not ("testonly" in kwargs and kwargs["testonly"]):
        DELETE_ENTRIES = [
            # TODO(b/255334320) Remove these 2 (and hopefully the entire zip -d invocation)
            "com/google/common/util/concurrent/ListenableFuture.class",
        ]
        deploy_jar = binary_name + "_cleaned.jar"
        native.genrule(
            name = binary_name + "_cleaned",
            srcs = [binary_name + "_deploy.jar"],
            outs = [deploy_jar],
            cmd = "\n".join([
                # zip -d operates on a single zip file, modifying it in place. So
                # make a copy of our input first, and make it writable.
                "cp $< $@",
                "chmod u+w $@",
                "zip -q -d $@ " + " ".join(DELETE_ENTRIES) + " || true ",
            ]),
            message = "Applying workarounds to plugin jar",
        )

    jar_target_name = name + "_intellij_plugin_jar"
    _intellij_plugin_jar(
        name = jar_target_name,
        deploy_jar = deploy_jar,
        jar_name = jar_name or (name + ".jar"),
        deps = deps,
        restrict_deps =
            select({
                "@rules_intellij//intellij_platform_sdk:android-studio-intellij-ext": restrict_deps,
                "//conditions:default": False,
            }),
        restricted_deps = deps if restrict_deps else [],
        plugin_xml = plugin_xml,
        plugin_deps = plugin_deps,
        optional_plugin_deps = optional_plugin_deps,
        plugin_icons = plugin_icons,
    )

    # included (with tag) as a hack so that IJwB can recognize this is an intellij plugin
    java_import(
        name = name,
        jars = [jar_target_name],
        tags = ["intellij-plugin"] + kwargs.pop("tags", []),
        **kwargs
    )
