SonatypePublishInfo = provider(
    fields={
        "coordinates": "Maven coordinates for the project, which may be None",
        "pom": "Pom.xml file for metadata",
        "javadocs": "Javadoc jar file for documentation files",
        "artifact_jar": "Jar with the code and metadata for execution",
        "source_jar": "Jar with the source code for review",
    },
)

_TEMPLATE = """#!/usr/bin/env bash

echo "Uploading {coordinates} to {maven_repo}"
./uploader {user} {password} {maven_repo} {profile} {coordinates} {artifact} {source} {doc} {pom}
"""

_RULES_JVM_TEMPLATE = """#!/usr/bin/env bash

echo "Uploading {coordinates} to {maven_repo}"
./uploader {maven_repo} {gpg_sign} {user} {password} {coordinates} pom.xml artifact.jar source.jar doc.jar
"""


def _rules_jvm_publish(ctx, executable, maven_repo, gpg_sign, user, password, uploader_attr, uploader_exec):
    ctx.actions.write(
        output=executable,
        is_executable=True,
        content=_RULES_JVM_TEMPLATE.format(
            coordinates=ctx.attr.coordinates,
            gpg_sign=gpg_sign,
            maven_repo=maven_repo,
            password=password,
            user=user,
        ),
    )

    return DefaultInfo(
        files=depset([executable]),
        executable=executable,
        runfiles=ctx.runfiles(
            symlinks={
                "artifact.jar": ctx.file.artifact_jar,
                "doc.jar": ctx.file.javadocs,
                "pom.xml": ctx.file.pom,
                "source.jar": ctx.file.source_jar,
                "uploader": uploader_exec,
            },
            collect_data=True,
        ).merge(uploader_attr[DefaultInfo].data_runfiles),
    )


def _sonatype_publish(ctx, executable, maven_repo, user, password, profile, group_id, artifact, version, uploader_attr,
                      uploader_exec):
    filename = "{}/{}/{}/{}-{}".format(group_id, artifact, version, artifact, version)
    artifact_jar = "%s.jar" % filename
    docs_jar = "%s-javadoc.jar" % filename
    sources_jar = "%s-sources.jar" % filename
    pom_file = "%s.pom" % filename

    ctx.actions.write(
        output=executable,
        is_executable=True,
        content=_TEMPLATE.format(
            coordinates=ctx.attr.coordinates,
            maven_repo=maven_repo,
            password=password,
            user=user,
            profile=profile,
            artifact=artifact_jar,
            source=sources_jar,
            doc=docs_jar,
            pom=pom_file,
        ),
    )

    return DefaultInfo(
        files=depset([executable]),
        executable=executable,
        runfiles=ctx.runfiles(
            symlinks={
                artifact_jar: ctx.file.artifact_jar,
                docs_jar: ctx.file.javadocs,
                sources_jar: ctx.file.source_jar,
                pom_file: ctx.file.pom,
                "uploader": uploader_exec,
            },
            collect_data=True,
        ).merge(uploader_attr[DefaultInfo].data_runfiles),
    )


def _publish(ctx):
    executable = ctx.actions.declare_file("%s-publisher" % ctx.attr.name)

    profile = ctx.attr.maven_profile
    coordinates_split = ctx.attr.coordinates.split(":")
    group_id = coordinates_split[0].replace(".", "/")
    artifact = coordinates_split[1]
    version = coordinates_split[2]
    maven_repo = ctx.var.get("maven_repo", "https://oss.sonatype.org/service/local")
    user = ctx.var.get("maven_user", "''")
    password = ctx.var.get("maven_password", "''")

    if maven_repo.startswith("file://"):
        return _rules_jvm_publish(ctx, executable, maven_repo, ctx.var.get("gpg_sign", "false"), user, password,
                                  ctx.attr._rules_jvm_external_uploader,
                                  ctx.executable._rules_jvm_external_uploader)
    elif version.endswith("-SNAPSHOT"):
        return _rules_jvm_publish(ctx, executable, "{}/staging/deploy/maven2/".format(maven_repo),
                                  ctx.var.get("gpg_sign", "true"), user,
                                  password, ctx.attr._rules_jvm_external_uploader,
                                  ctx.executable._rules_jvm_external_uploader)
    else:
        return _sonatype_publish(ctx, executable, maven_repo, user, password, profile,
                                 group_id, artifact, version,
                                 ctx.attr._uploader, ctx.executable._uploader)


def _sonatype_publish_impl(ctx):
    default_info = _publish(ctx)

    return [
        default_info,
        _get_publish_info(ctx),
    ]


def _get_publish_info(ctx):
    return SonatypePublishInfo(
        coordinates=ctx.attr.coordinates,
        artifact_jar=ctx.file.artifact_jar,
        javadocs=ctx.file.javadocs,
        source_jar=ctx.file.source_jar,
        pom=ctx.file.pom,
    )


sonatype_publish = rule(
    _sonatype_publish_impl,
    doc="""Publish artifacts to a maven repository.

The maven repository may accessed locally remotely using an `https://` URL.
The following flags may be set using `--define`:
  maven_repo: A URL for the repo to use. May be "https" or "file".
  maven_user: The user name to use when uploading to the maven repository.
  maven_password: The password to use when uploading to the maven repository.

When signing with GPG, the current default key is used.
""",
    executable=True,
    attrs={
        "coordinates": attr.string(
            mandatory=True,
        ),
        "maven_profile": attr.string(
            mandatory=True,
        ),
        "pom": attr.label(
            mandatory=True,
            allow_single_file=True,
        ),
        "javadocs": attr.label(
            mandatory=True,
            allow_single_file=True,
        ),
        "artifact_jar": attr.label(
            mandatory=True,
            allow_single_file=True,
        ),
        "source_jar": attr.label(
            mandatory=True,
            allow_single_file=True,
        ),
        "_uploader": attr.label(
            executable=True,
            cfg="host",
            default="//src/main/scala/org/jetbrains/bazel:SonatypePublisher",
            allow_files=True,
        ),
        "_rules_jvm_external_uploader": attr.label(
            executable=True,
            cfg="host",
            default="//src/main/java/org/jetbrains/bazel:MavenPublisher",
            allow_files=True,
        )
    },
)
