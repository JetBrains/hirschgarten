load("//aspects:utils/utils.bzl", "file_location", "is_external", "map", "update_sync_output_groups")

MAINLINE_SCALA_TOOLCHAIN = "@io_bazel_rules_scala//scala:toolchain_type"
ANNEX_SCALA_TOOLCHAIN = "@rules_scala_annex//rules/scala:toolchain_type"


SCALA_COMPILER_NAMES = [
    "scala3-compiler",
    "scala-compiler",
]

SCALA_LIBRARY_NAMES = [
    "scala3-interfaces",
    "scala3-library",
    "scala3-reflect",
    "scala-asm",
    "scala-library",
    "scala-reflect",
    "tasty-core",
    "compiler-interface",
]

def contains_substring(strings, name):
    for s in strings:
        if s in name:
            return True
    return False

def find_scalac_classpath(runfiles):
    result = []
    found_scala_compiler_jar = False
    for file in runfiles:
        name = file.basename
        if file.extension == "jar" and contains_substring(SCALA_COMPILER_NAMES, name):
            found_scala_compiler_jar = True
            result.append(file)
        elif file.extension == "jar" and contains_substring(SCALA_LIBRARY_NAMES, name):
            result.append(file)
    return result if found_scala_compiler_jar and len(result) >= 2 else []

def extract_scalatest_classpath(rule_attr):
    def extract_from_attr(attr):
        attr_value = getattr(rule_attr, attr, None)
        if attr_value != None and JavaInfo in attr_value:
            jars = getattr(attr_value[JavaInfo], "full_compile_jars", depset()).to_list()
            return map(file_location, jars)
        return []

    return (
        extract_from_attr("_scalatest") +
        extract_from_attr("_scalatest_runner") +
        extract_from_attr("_scalatest_reporter")
    )

def extract_scala_info_mainline(target, ctx, output_groups):
    scala_info = {}
    rule_attr = ctx.rule.attr
    common_scalac_opts = ctx.toolchains[MAINLINE_SCALA_TOOLCHAIN].scalacopts
    if hasattr(rule_attr, "_scalac"):
        scalac = rule_attr._scalac
        compiler_classpath = find_scalac_classpath(scalac.default_runfiles.files.to_list())
        if compiler_classpath:
            scala_info["compiler_classpath"] = map(file_location, compiler_classpath)
            if is_external(scalac):
                update_sync_output_groups(output_groups, "bsp-sync-artifact", depset(compiler_classpath))
    scala_info["scalac_opts"] = common_scalac_options + getattr(ctx.rule.attr, "scalacopts", [])
    scala_info["scalatest_classpath"] = extract_scalatest_classpath(ctx.rule.attr)
    return scala_info
    
def extract_scala_info_annex(target, ctx, output_groups):
    scala_info = {}
    rule_attr = ctx.rule.attr
    scala_configuration = ctx.toolchains[SCALA_TOOLCHAIN].scala_configuration
    common_scalac_options = scala_configuration.global_scalacopts

    classpath_files = []
    for target in scala_configuration.compiler_classpath:
        for file in target[JavaInfo].runtime_output_jars:
            classpath_files.append(file)
    compiler_classpath = find_scalac_classpath(classpath_files)

    if len(compiler_classpath) > 0:
        scala_info["compiler_classpath"] = map(file_location, compiler_classpath)

        if any([is_external(target) for target in scala_configuration.compiler_classpath]):
            update_sync_output_groups(
                output_groups,
                "external-deps-resolve",
                depset(compiler_classpath)
            )

    scala_info["scalac_opts"] = common_scalac_options + getattr(ctx.rule.attr, "scalacopts", [])
    scala_info["scalatest_classpath"] = extract_scalatest_classpath(ctx.rule.attr)
    return scala_info

def extract_scala_info(target, ctx, output_groups):
    kind = ctx.rule.kind
    if not kind.startswith("scala_") and not kind.startswith("thrift_"):
        return None, None

    rule_attr = ctx.rule.attr

    # check of _scala_toolchain is necessary, because SCALA_TOOLCHAIN will always be present
    if hasattr(rule_attr, "_scala_toolchain"):
        scala_info = extract_scala_info_mainline(target, ctx, output_groups)
    elif ANNEX_SCALA_TOOLCHAIN in ctx.toolchains:
        scala_info = extract_scala_info_annex(target, ctx, output_groups)
    else:
        scala_info = {}
        scala_info["scalac_opts"] =  getattr(ctx.rule.attr, "scalacopts", [])
        scala_info["scalatest_classpath"] = extract_scalatest_classpath(rule_attr)

    return dict(scala_target_info = struct(**scala_info)), None
