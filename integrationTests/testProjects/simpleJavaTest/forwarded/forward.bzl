load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _forward(ctx):
    return [ctx.attr.dep[JavaInfo]]

forward = rule(
    implementation = _forward,
    attrs = {
        "dep": attr.label(),
    },
)
