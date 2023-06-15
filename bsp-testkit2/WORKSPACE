workspace(name = "bsp-testkit")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

SKYLIB_VERSION = "1.2.1"

http_archive(
    name = "bazel_skylib",
    sha256 = "f7be3474d42aae265405a592bb7da8e171919d74c16f082a5457840f06054728",
    type = "tar.gz",
    url = "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(SKYLIB_VERSION, SKYLIB_VERSION),
)

RULES_SCALA_COMMIT = "9b85affa2e08a350a4315882b602eda55b262356"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "b9eaf9bd026b5b1702d6705b4e953a5b73c49705552a6f2da83edd995b2dbe1f",
    strip_prefix = "rules_scala-%s" % RULES_SCALA_COMMIT,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % RULES_SCALA_COMMIT,
)

load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config(scala_version = "2.13.8")

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

load("@io_bazel_rules_scala//testing:scalatest.bzl", "scalatest_repositories", "scalatest_toolchain")

scalatest_repositories()

scalatest_toolchain()

RULES_JVM_EXTERNAL_TAG = "4.2"

RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-{}".format(RULES_JVM_EXTERNAL_TAG),
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/{}.zip".format(RULES_JVM_EXTERNAL_TAG),
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
     "com.google.code.gson:gson:2.8.9",
     "com.google.guava:guava:31.1-jre",
     "org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.12.0",
     "ch.epfl.scala:bsp4j:2.1.0-M4",
     "org.junit.jupiter:junit-jupiter:5.9.3",
     "org.scala-lang:scala-library:2.13.8",
    ],
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
        "https://repo.maven.apache.org/maven2",
    ],
)
