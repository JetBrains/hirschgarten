load("@rules_jvm_external//:specs.bzl", "maven", "parse")
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@io_bazel_rules_scala//scala:scala_cross_version.bzl", "default_maven_server_urls")

def _dependency(coordinates, exclusions = None):
    artifact = parse.parse_maven_coordinate(coordinates)
    return maven.artifact(
        group = artifact["group"],
        artifact = artifact["artifact"],
        packaging = artifact.get("packaging"),
        classifier = artifact.get("classifier"),
        version = artifact["version"],
        exclusions = exclusions,
    )

_deps = [
    _dependency("com.google.code.gson:gson:2.8.9"),
    _dependency("com.google.guava:guava:31.1-jre"),
    _dependency("ch.epfl.scala:bsp4j:2.1.0-M4"),
    _dependency("org.junit.jupiter:junit-jupiter:5.9.3"),
    _dependency("org.scala-lang:scala-library:2.13.8"),
]

def dependencies():
    maven_install(
        artifacts = _deps,
        repositories = ["https://oss.sonatype.org/content/repositories/snapshots"] + default_maven_server_urls(),
        fetch_sources = True,
    )
