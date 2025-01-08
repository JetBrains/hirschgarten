load("//aspects:utils/utils.bzl", "file_location", "is_external", "map", "update_sync_output_groups")

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

def extract_scala_info(target, ctx, output_groups, **kwargs):
    kind = ctx.rule.kind
    if not kind.startswith("scala_") and not kind.startswith("thrift_"):
        return None, None

    SCALA_TOOLCHAIN = "@io_bazel_rules_scala//scala:toolchain_type"

    scala_info = {}

    rule_attr = ctx.rule.attr

    # check of _scala_toolchain is necessary, because SCALA_TOOLCHAIN will always be present
    if hasattr(rule_attr, "_scala_toolchain"):
        common_scalac_opts = ctx.toolchains[SCALA_TOOLCHAIN].scalacopts
        if hasattr(rule_attr, "_scalac"):
            scalac = rule_attr._scalac
            compiler_classpath = find_scalac_classpath(scalac.default_runfiles.files.to_list())
            if compiler_classpath:
                scala_info["compiler_classpath"] = map(file_location, compiler_classpath)
                if is_external(scalac):
                    update_sync_output_groups(output_groups, "external-deps-resolve", depset(compiler_classpath))
    else:
        common_scalac_opts = []
    scala_info["scalac_opts"] = common_scalac_opts + getattr(ctx.rule.attr, "scalacopts", [])

    scala_info["scalatest_classpath"] = extract_scalatest_classpath(rule_attr)

    return dict(scala_target_info = struct(**scala_info)), None
