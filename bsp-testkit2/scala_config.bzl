load("@io_bazel_rules_scala//:scala_config.bzl", _scala_config_orig="scala_config")

def _scala_config(_ctx):
    _scala_config_orig(scala_version = "2.13.12")


scala_config = module_extension(implementation = _scala_config)
