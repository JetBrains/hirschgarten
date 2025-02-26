# Copyright 2019-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

load(
    "@bazel_tools//tools/build_defs/cc:action_names.bzl",
    "ACTION_NAMES",
)
load("//aspects:utils/utils.bzl", "create_struct", "do_starlark_string_expansion", "file_location", "files_to_list", "update_sync_output_groups")

# Defensive list of features that can appear in the C++ toolchain, but which we
# definitely don't want to enable (when enabled, they'd contribute command line
# flags that don't make sense in the context of intellij info).
UNSUPPORTED_FEATURES = [
    "thin_lto",
    "module_maps",
    "use_header_modules",
    "fdo_instrument",
    "fdo_optimize",
]

def extract_cpp_info(target, ctx, output_groups, **kwargs):
    if CcInfo not in target:
        return None, None

    # ignore cc_proto_library, attach to proto_library with aspect attached instead
    if ctx.rule.kind == "cc_proto_library":
        return None, None

    # Go targets always provide CcInfo. Usually it's empty, but even if it isn't we don't handle it
    if ctx.rule.kind.startswith("go_"):
        return None, None

    headers = [
        file_location(f)
        for t in getattr(ctx.rule.attr, "hdrs", [])
        for f in files_to_list(t)
        if not f.is_directory
    ]
    textual_headers = [
        file_location(f)
        for t in getattr(ctx.rule.attr, "textual_hdrs", [])
        for f in files_to_list(t)
        if not f.is_directory
    ]

    target_copts = []
    if hasattr(ctx.rule.attr, "copts"):
        target_copts += ctx.rule.attr.copts
    extra_targets = []
    if hasattr(ctx.rule.attr, "additional_compiler_inputs"):
        extra_targets += ctx.rule.attr.additional_compiler_inputs
    target_copts = do_starlark_string_expansion(ctx, "copt", target_copts, extra_targets)

    compilation_context = target[CcInfo].compilation_context

    # Merge current compilation context with context of implementation dependencies.
    if hasattr(ctx.rule.attr, "implementation_deps"):
        implementation_deps = ctx.rule.attr.implementation_deps
        compilation_context = cc_common.merge_compilation_contexts(
            compilation_contexts =
                [compilation_context] + [impl[CcInfo].compilation_context for impl in implementation_deps],
        )
    external_includes = getattr(compilation_context, "external_includes", depset()).to_list()

    # external_includes available since bazel 7
    result = create_struct(
        copts = target_copts,
        headers = headers,
        textual_headers = textual_headers,
        transitive_define = compilation_context.defines.to_list(),
        transitive_include_directory = compilation_context.includes.to_list(),
        transitive_quote_include_directory = compilation_context.quote_includes.to_list(),
        transitive_system_include_directory = compilation_context.system_includes.to_list() + external_includes,
        include_prefix = getattr(ctx.rule.attr, "include_prefix", None),
        strip_include_prefix = getattr(ctx.rule.attr, "strip_include_prefix", None),
    )

    resolve_files = compilation_context.headers
    compile_files = target[OutputGroupInfo].compilation_outputs if hasattr(target[OutputGroupInfo], "compilation_outputs") else depset([])
    update_sync_output_groups(output_groups, "bsp-sync-artifact", resolve_files)
    update_sync_output_groups(output_groups, "bsp-build-artifact", compile_files)
    return dict(cpp_target_info = result), None

def extract_c_toolchain_info(target, ctx, **kwargs):
    if ctx.rule.kind != "cc_toolchain" and ctx.rule.kind != "cc_toolchain_suite" and ctx.rule.kind != "cc_toolchain_alias":
        return None, None
    if cc_common.CcToolchainInfo not in target:
        return None, None
    cpp_toolchain = target[cc_common.CcToolchainInfo]

    # cpp fragment to access bazel options
    cpp_fragment = ctx.fragments.cpp

    copts = cpp_fragment.copts
    cxxopts = cpp_fragment.cxxopts
    conlyopts = cpp_fragment.conlyopts
    feature_configuration = cc_common.configure_features(
        ctx = ctx,
        cc_toolchain = cpp_toolchain,
        requested_features = ctx.features,
        unsupported_features = ctx.disabled_features + UNSUPPORTED_FEATURES,
    )

    c_variables = cc_common.create_compile_variables(
        feature_configuration = feature_configuration,
        cc_toolchain = cpp_toolchain,
        user_compile_flags = copts + conlyopts,
    )
    cpp_variables = cc_common.create_compile_variables(
        feature_configuration = feature_configuration,
        cc_toolchain = cpp_toolchain,
        user_compile_flags = copts + cxxopts,
    )
    c_options = cc_common.get_memory_inefficient_command_line(
        feature_configuration = feature_configuration,
        action_name = ACTION_NAMES.c_compile,
        variables = c_variables,
    )
    cpp_options = cc_common.get_memory_inefficient_command_line(
        feature_configuration = feature_configuration,
        action_name = ACTION_NAMES.cpp_compile,
        variables = cpp_variables,
    )
    if (get_registry_flag(ctx, "_cpp_use_get_tool_for_action")):
        c_compiler = cc_common.get_tool_for_action(
            feature_configuration = feature_configuration,
            action_name = ACTION_NAMES.c_compile,
        )
        cpp_compiler = cc_common.get_tool_for_action(
            feature_configuration = feature_configuration,
            action_name = ACTION_NAMES.cpp_compile,
        )
    else:
        c_compiler = str(cpp_toolchain.compiler_executable)
        cpp_compiler = str(cpp_toolchain.compiler_executable)
    c_toolchain_info = create_struct(
        built_in_include_directory = [str(d) for d in cpp_toolchain.built_in_include_directories],
        c_option = c_options,
        cpp_option = cpp_options,
        c_compiler = c_compiler,
        cpp_compiler = cpp_compiler,
        target_name = cpp_toolchain.target_gnu_system_name,
    )
    return dict(c_toolchain_info = c_toolchain_info), None

def get_registry_flag(ctx, name):
    """Registry flags are passed to aspects using defines. See CppAspectArgsProvider."""

    return ctx.var.get(name) == "true"
