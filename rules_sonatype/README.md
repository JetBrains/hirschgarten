bazel-sonatype 
======

A set of rules for publishing your project to the Maven Central repository through the REST API of Sonatype Nexus. Allows releasing Java projects to the [Maven central repository](https://repo1.maven.org/maven2/).

## Prerequisites

 * Create a Sonatype Repository account
   * Follow the instruction in the [Central Repository documentation site](http://central.sonatype.org).
     * Create a Sonatype account
     * Create a GPG key
     * Open a JIRA ticket to get a permission for synchronizing your project to the Central Repository (aka Maven Central).
   * GPG key must be available as the default key on the machine
    
## Configurations
In the Workspace file, the following must be added in order install:
- [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external)
- [bazel_skylib](https://github.com/bazelbuild/bazel-skylib)
- [rules_python](https://github.com/bazelbuild/rules_python)
- [zlib](https://zlib.net)
- [com_google_protobuf](https://github.com/protocolbuffers/protobuf)
- [rules_scala](https://github.com/bazelbuild/rules_scala)


```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# ======================================================================================================================
# ----------------------------------------------------------------------------------------------------------------------
# ======================================================================================================================
# rules_jvm_external

RULES_JVM_EXTERNAL_TAG = "4.2"

RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-{}".format(RULES_JVM_EXTERNAL_TAG),
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/{}.zip".format(RULES_JVM_EXTERNAL_TAG),
)

# ----------------------------------------------------------------------------------------------------------------------
load("@bazel_sonatype//:defs.bzl", "sonatype_dependencies")

sonatype_dependencies()

# ======================================================================================================================
# bazel_skylib

BAZEL_SKYLIB_TAG = "1.2.0"

BAZEL_SKYLIB_SHA = "af87959afe497dc8dfd4c6cb66e1279cb98ccc84284619ebfec27d9c09a903de"

http_archive(
    name = "bazel_skylib",
    sha256 = BAZEL_SKYLIB_SHA,
    url = "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(BAZEL_SKYLIB_TAG, BAZEL_SKYLIB_TAG),
)

# ======================================================================================================================
# rules_python - required by com_google_protobuf

RULES_PYTHON_TAG = "0.6.0"

RULES_PYTHON_SHA = "a30abdfc7126d497a7698c29c46ea9901c6392d6ed315171a6df5ce433aa4502"

http_archive(
    name = "rules_python",
    sha256 = RULES_PYTHON_SHA,
    strip_prefix = "rules_python-{}".format(RULES_PYTHON_TAG),
    url = "https://github.com/bazelbuild/rules_python/archive/{}.tar.gz".format(RULES_PYTHON_TAG),
)

# ======================================================================================================================
# zlib - required by com_google_protobuf

ZLIB_TAG = "1.2.11"

ZLIB_SHA = "c3e5e9fdd5004dcb542feda5ee4f0ff0744628baf8ed2dd5d66f8ca1197cb1a1"

http_archive(
    name = "zlib",
    build_file = "@com_google_protobuf//:third_party/zlib.BUILD",
    sha256 = ZLIB_SHA,
    strip_prefix = "zlib-{}".format(ZLIB_TAG),
    url = "https://zlib.net/zlib-{}.tar.gz".format(ZLIB_TAG),
)

# ======================================================================================================================
# com_google_protobuf -  required by io_bazel_rules_scala

COM_GOOGLE_PROTOBUF_TAG = "3.19.4"

COM_GOOGLE_PROTOBUF_SHA = "3bd7828aa5af4b13b99c191e8b1e884ebfa9ad371b0ce264605d347f135d2568"

http_archive(
    name = "com_google_protobuf",
    sha256 = COM_GOOGLE_PROTOBUF_SHA,
    strip_prefix = "protobuf-{}".format(COM_GOOGLE_PROTOBUF_TAG),
    url = "https://github.com/protocolbuffers/protobuf/archive/v{}.tar.gz".format(COM_GOOGLE_PROTOBUF_TAG),
)

# ======================================================================================================================
# io_bazel_rules_scala

IO_BAZEL_RULES_SCALA_TAG = "20220201"

IO_BAZEL_RULES_SCALA_SHA = "77a3b9308a8780fff3f10cdbbe36d55164b85a48123033f5e970fdae262e8eb2"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = IO_BAZEL_RULES_SCALA_SHA,
    strip_prefix = "rules_scala-{}".format(IO_BAZEL_RULES_SCALA_TAG),
    url = "https://github.com/bazelbuild/rules_scala/releases/download/{}/rules_scala-{}.zip".format(IO_BAZEL_RULES_SCALA_TAG, IO_BAZEL_RULES_SCALA_TAG),
)

# ----------------------------------------------------------------------------------------------------------------------
load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config()

# ----------------------------------------------------------------------------------------------------------------------
load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

# ----------------------------------------------------------------------------------------------------------------------
load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

# ======================================================================================================================

BAZEL_SONATYPE_TAG = "0.0.1"
http_archive(
    name = "bazel_sonatype",
    strip_prefix = "bazel-sonatype-{}".format(BAZEL_SONATYPE_TAG),
    url = "https://github.com/JetBrains/bazel-sonatype/archive/{}.zip".format(BAZEL_SONATYPE_TAG),
)

load("@bazel_sonatype//:defs.bzl", "sonatype_dependencies")

sonatype_dependencies()
```

A `sonatype_java_export` must be defined in order to publish. This is similar to a java_library but with some new parameters:

```python
# /src/BUILD
load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")

sonatype_java_export(
    name = "project_name",
    maven_coordinates = "com.example:project:0.0.1",
    maven_profile = "com.example",
    pom_template = "//:pom.xml", # Omittable but sonatype will reject reject publishing without more information than the template has
    srcs = glob(["*.java"]),
    deps = [
        "//src",
    ],
)
```

The pom file must be defined for publishing to Sonatype's Release, with the following format, **remember to update the template with your data!**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>

    <groupId>{groupId}</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>{version}</version>

    <name>Your project name</name>
    <description>Some description</description>
    <url>https://www.yourcompany.com/</url>

    <dependencies>
        {dependencies}
    </dependencies>
    <developers>
        <developer> 
            <id>developerid</id>
            <name>Your Name</name>
            <email>your@email.com</email>
            <url>https://github.com/developerid</url>
        </developer>
    <licenses>
        <license>
            <name>Your licence</name>
            <url>http://yourlicence.txt</url>
        </license>
    </licenses>
    <scm>
        <connection>scm:git:github.com/yourcompany/project.git</connection>
        <developerConnection>scm:git:git@github.com:yourcompany/project.git</developerConnection>
        <url>https://github.com/yourcompany/project</url>
    </scm>
</project>
```

You must leave all variables in brackets like that, since that will be filled internally. Make sure to add the following fields:
- Project Name
- Project Description
- Developer Information
- License Information
- SCM URL

Publishing can now be done using `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=https://oss.sonatype.org/service/local" \ # Defaults to the legacy Sonatype repository
  --define "maven_user=user" \
  --define "maven_password=password" \
  //src:project_name.publish
```

It is also possible to publish a snapshot of a build. This can be done by guaranteeing that the version in the `sonatype_java_export`'s coordinates ends with -SNAPSHOT

```python
# /src/BUILD
load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")

sonatype_java_export(
    name = "project_name",
    maven_coordinates = "com.example:project:0.0.1-SNAPSHOT",
    maven_profile = "com.example",
    pom_template = "//:pom.xml", # Omittable
    srcs = glob(["*.java"]),
    deps = [
        "//src",
    ],
)
```

And then running `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=https://oss.sonatype.org/service/local" \ # Defaults to the legacy Sonatype repository
  --define "maven_user=user" \
  --define "maven_password=password" \
  --define "gpg_sign=true" \ # Defaults to true
  //src:project_name.publish
```

Publishing locally all the artifacts is also possible, if a file based url is provided for `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=file://$HOME/.m2/repository" \ # Defaults to the legacy Sonatype repository
  --define "gpg_sign=false" \ # Defaults to false
  //src:project_name.publish
```
