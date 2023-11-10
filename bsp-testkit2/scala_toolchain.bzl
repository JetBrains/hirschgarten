load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

def _scala_toolchain(_ctx):
    scala_register_toolchains()

scala_toolchain = module_extension(implementation = _scala_toolchain)