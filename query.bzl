load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _runtime_classpath_impl(ctx):
    transitive_inputs = []
    target = ctx.attr.target
    if JavaInfo in target:
        info = target[JavaInfo]
        transitive_inputs.append(info.transitive_runtime_jars)
        if hasattr(info, "compilation_info"):
            compilation_info = info.compilation_info
            if hasattr(compilation_info, "runtime_classpath"):
                transitive_inputs.append(compilation_info.runtime_classpath)
    else:
        files = []
        for f in target[DefaultInfo].files.to_list():
            if not f.extension == "jar":
                fail("unexpected file type in java_single_jar.deps: %s" % f.path)
            files.append(f)
        transitive_inputs.append(depset(files))
    inputs = depset(transitive = transitive_inputs)

    if hasattr(java_common, "JavaRuntimeClasspathInfo"):
        deploy_env_jars = depset(transitive = [
#             dep[java_common.JavaRuntimeClasspathInfo].runtime_classpath
#             for dep in ctx.attr.deploy_env
        ])
        excluded_jars = {jar: None for jar in deploy_env_jars.to_list()}
        if excluded_jars:
            inputs = depset([jar for jar in inputs.to_list() if jar not in excluded_jars])

    print("inputs: ", inputs)
    files = depset([])
    providers = [DefaultInfo(
        files = files,
        runfiles = ctx.runfiles(transitive_files = files),
    )]
    if hasattr(java_common, "JavaRuntimeClasspathInfo"):
        providers.append(java_common.JavaRuntimeClasspathInfo(runtime_classpath = inputs))
    return providers

runtime_classpath = rule(
    implementation = _runtime_classpath_impl,
    attrs = {
        "target": attr.label(
            mandatory = True,
            providers = [JavaInfo],
        ),
    },
)
