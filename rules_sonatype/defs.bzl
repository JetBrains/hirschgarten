load("//private/rules:sonatype_dependencies.bzl", _sonatype_dependencies = "sonatype_dependencies")
load("//private/rules:sonatype_java_export.bzl", _sonatype_java_export = "sonatype_java_export")

sonatype_java_export = _sonatype_java_export
sonatype_dependencies = _sonatype_dependencies
