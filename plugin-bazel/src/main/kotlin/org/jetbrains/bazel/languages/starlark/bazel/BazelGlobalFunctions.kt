package org.jetbrains.bazel.languages.starlark.bazel

data class BazelGlobalFunctionParameter(
  val name: String,
  val default: String,
  val positional: Boolean,
  val required: Boolean = false,
  val docString: String? = null,
)

data class BazelGlobalFunction(
  val name: String,
  val docString: String? = null,
  val params: List<BazelGlobalFunctionParameter> = emptyList(),
)

object BazelGlobalFunctions {
  val STARLARK_FUNCTIONS =
    mapOf(
      "abs" to BazelGlobalFunction("abs"),
      "all" to BazelGlobalFunction("all"),
      "any" to BazelGlobalFunction("any"),
      "bool" to BazelGlobalFunction("bool"),
      "dict" to BazelGlobalFunction("dict"),
      "dir" to BazelGlobalFunction("dir"),
      "enumerate" to BazelGlobalFunction("enumerate"),
      "fail" to BazelGlobalFunction("fail"),
      "float" to BazelGlobalFunction("float"),
      "getattr" to BazelGlobalFunction("getattr"),
      "hasattr" to BazelGlobalFunction("hasattr"),
      "hash" to BazelGlobalFunction("hash"),
      "int" to BazelGlobalFunction("int"),
      "len" to BazelGlobalFunction("len"),
      "list" to BazelGlobalFunction("list"),
      "max" to BazelGlobalFunction("max"),
      "min" to BazelGlobalFunction("min"),
      "print" to BazelGlobalFunction("print"),
      "range" to BazelGlobalFunction("range"),
      "repr" to BazelGlobalFunction("repr"),
      "reversed" to BazelGlobalFunction("reversed"),
      "sorted" to BazelGlobalFunction("sorted"),
      "str" to BazelGlobalFunction("str"),
      "tuple" to BazelGlobalFunction("tuple"),
      "type" to BazelGlobalFunction("type"),
      "zip" to BazelGlobalFunction("zip"),
    )

  val EXTENSION_FUNCTIONS =
    mapOf(
      "analysis_test_transition" to BazelGlobalFunction("analysis_test_transition"),
      "aspect" to BazelGlobalFunction("aspect"),
      "configuration_field" to BazelGlobalFunction("configuration_field"),
      "depset" to BazelGlobalFunction("depset"),
      "exec_group" to BazelGlobalFunction("exec_group"),
      "load" to BazelGlobalFunction("load"),
      "module_extension" to BazelGlobalFunction("module_extension"),
      "provider" to BazelGlobalFunction("provider"),
      "repository_rule" to BazelGlobalFunction("repository_rule"),
      "rule" to BazelGlobalFunction("rule"),
      "select" to BazelGlobalFunction("select"),
      "subrule" to BazelGlobalFunction("subrule"),
      "tag_class" to BazelGlobalFunction("tag_class"),
      "visibility" to BazelGlobalFunction("visibility"),
    )

  val BUILD_FUNCTIONS =
    mapOf(
      "depset" to BazelGlobalFunction("depset"),
      "existing_rule" to BazelGlobalFunction("existing_rule"),
      "existing_rules" to BazelGlobalFunction("existing_rules"),
      "exports_files" to BazelGlobalFunction("exports_files"),
      "glob" to BazelGlobalFunction("glob"),
      "load" to BazelGlobalFunction("load"),
      "module_name" to BazelGlobalFunction("module_name"),
      "module_version" to BazelGlobalFunction("module_version"),
      "package" to BazelGlobalFunction("package"),
      "package_group" to BazelGlobalFunction("package_group"),
      "package_name" to BazelGlobalFunction("package_name"),
      "package_relative_label" to BazelGlobalFunction("package_relative_label"),
      "repo_name" to BazelGlobalFunction("repo_name"),
      "repository_name" to BazelGlobalFunction("repository_name"),
      "select" to BazelGlobalFunction("select"),
      "subpackages" to BazelGlobalFunction("subpackages"),
    )

  val MODULE_FUNCTIONS =
    mapOf(
      "archive_override" to
        BazelGlobalFunction(
          "archive_override",
          """
          Specifies that this dependency should come from an archive file (zip, gzip, etc) at a certain location, instead of from a registry. Effectively, this dependency will be backed by an http_archive rule.
          This directive only takes effect in the root module; in other words, if a module is used as a dependency by others, its own overrides are ignored.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "module_name",
              "\'\'",
              true,
              required = true,
              "The name of the Bazel module dependency to apply this override to.",
            ),
          ),
        ),
      "bazel_dep" to
        BazelGlobalFunction(
          "bazel_dep",
          """
          Declares a direct dependency on another Bazel module.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "name",
              "\'\'",
              false,
              required = true,
              "The name of the module to be added as a direct dependency.",
            ),
            BazelGlobalFunctionParameter(
              "version",
              "\'\'",
              false,
              required = false,
              "The version of the module to be added as a direct dependency.",
            ),
            BazelGlobalFunctionParameter(
              "max_compatibility_level",
              "-1",
              false,
              required = false,
              "The maximum compatibility_level supported for the module to be added as a direct dependency. The version of the module implies the minimum compatibility_level supported, as well as the maximum if this attribute is not specified.",
            ),
            BazelGlobalFunctionParameter(
              "repo_name",
              "\'\'",
              false,
              required = false,
              """The name of the external repo representing this dependency. This is by default the name of the module. Can be set to None to make this dependency a "nodep" dependency: in this case, this bazel_dep specification is only honored if the target module already exists in the dependency graph by some other means.""",
            ),
            BazelGlobalFunctionParameter(
              "dev_dependency",
              "False",
              false,
              required = false,
              "If true, this dependency will be ignored if the current module is not the root module or --ignore_dev_dependency is enabled.",
            ),
          ),
        ),
      "git_override" to
        BazelGlobalFunction(
          "git_override",
          """
          Specifies that this dependency should come from a certain commit in a Git repository, instead of from a registry. Effectively, this dependency will be backed by a git_repository rule.
          This directive only takes effect in the root module; in other words, if a module is used as a dependency by others, its own overrides are ignored.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "module_name",
              "\'\'",
              true,
              required = true,
              "The name of the Bazel module dependency to apply this override to.",
            ),
          ),
        ),
      "include" to
        BazelGlobalFunction(
          "include",
          """
          Includes the contents of another MODULE.bazel-like file. Effectively, include() behaves as if the included file is textually placed at the location of the include() call, except that variable bindings (such as those used for use_extension) are only ever visible in the file they occur in, not in any included or including files.
          Only the root module may use include(); it is an error if a bazel_dep's MODULE file uses include().
          
          Only files in the main repo may be included.
          
          include() allows you to segment the root module file into multiple parts, to avoid having an enormous MODULE.bazel file or to better manage access control for individual semantic segments.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "label",
              "\'\'",
              true,
              required = true,
              """The label pointing to the file to include. The label must point to a file in the main repo; in other words, it must start with double slashes (//). The name of the file must end with .MODULE.bazel and must not start with ..""",
            ),
          ),
        ),
      "local_path_override" to
        BazelGlobalFunction(
          "local_path_override",
          """
          Specifies that this dependency should come from a certain directory on local disk, instead of from a registry. Effectively, this dependency will be backed by a local_repository rule.
          This directive only takes effect in the root module; in other words, if a module is used as a dependency by others, its own overrides are ignored.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "module_name",
              "\'\'",
              true,
              required = true,
              "The name of the Bazel module dependency to apply this override to.",
            ),
            BazelGlobalFunctionParameter(
              "path",
              "\'\'",
              true,
              required = true,
              "The path to the directory where this module is.",
            ),
          ),
        ),
      "module" to
        BazelGlobalFunction(
          "module",
          """
          Declares certain properties of the Bazel module represented by the current Bazel repo. These properties are either essential metadata of the module (such as the name and version), or affect behavior of the current module and its dependents.
          It should be called at most once, and if called, it must be the very first directive in the MODULE.bazel file. It can be omitted only if this module is the root module (as in, if it's not going to be depended on by another module).
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "name",
              "\'\'",
              false,
              required = false,
              "The name of the module. Can be omitted only if this module is the root module (as in, if it's not going to be depended on by another module). A valid module name must: 1) only contain lowercase letters (a-z), digits (0-9), dots (.), hyphens (-), and underscores (_); 2) begin with a lowercase letter; 3) end with a lowercase letter or digit.",
            ),
            BazelGlobalFunctionParameter(
              "version",
              "\'\'",
              false,
              required = false,
              "The version of the module. Can be omitted only if this module is the root module (as in, if it's not going to be depended on by another module). The version must be in a relaxed SemVer format; see the documentation for more details.",
            ),
            BazelGlobalFunctionParameter(
              "compatibility_level",
              "0",
              false,
              required = false,
              """The compatibility level of the module; this should be changed every time a major incompatible change is introduced. This is essentially the "major version" of the module in terms of SemVer, except that it's not embedded in the version string itself, but exists as a separate field. Modules with different compatibility levels participate in version resolution as if they're modules with different names, but the final dependency graph cannot contain multiple modules with the same name but different compatibility levels (unless multiple_version_override is in effect). See the documentation for more details.""",
            ),
            BazelGlobalFunctionParameter(
              "repo_name",
              "\'\'",
              false,
              required = false,
              """The name of the repository representing this module, as seen by the module itself. By default, the name of the repo is the name of the module. This can be specified to ease migration for projects that have been using a repo name for itself that differs from its module name.""",
            ),
            BazelGlobalFunctionParameter(
              "bazel_compatibility",
              "[]",
              false,
              required = false,
              """A list of bazel versions that allows users to declare which Bazel versions are compatible with this module. It does NOT affect dependency resolution, but bzlmod will use this information to check if your current Bazel version is compatible. The format of this value is a string of some constraint values separated by comma. Three constraints are supported: <=X.X.X: The Bazel version must be equal or older than X.X.X. Used when there is a known incompatible change in a newer version. >=X.X.X: The Bazel version must be equal or newer than X.X.X.Used when you depend on some features that are only available since X.X.X. -X.X.X: The Bazel version X.X.X is not compatible. Used when there is a bug in X.X.X that breaks you, but fixed in later versions.""",
            ),
          ),
        ),
      "multiple_version_override" to
        BazelGlobalFunction(
          "multiple_version_override",
          """
          Specifies that a dependency should still come from a registry, but multiple versions of it should be allowed to coexist. See the documentation for more details. This directive only takes effect in the root module; in other words, if a module is used as a dependency by others, its own overrides are ignored.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "module_name",
              "\'\'",
              true,
              required = true,
              "The name of the Bazel module dependency to apply this override to.",
            ),
            BazelGlobalFunctionParameter(
              "versions",
              "",
              true,
              required = true,
              """
              Explicitly specifies the versions allowed to coexist. These versions must already be present in the dependency graph pre-selection. Dependencies on this module will be "upgraded" to the nearest higher allowed version at the same compatibility level, whereas dependencies that have a higher version than any allowed versions at the same compatibility level will cause an error.
              """.trimIndent(),
            ),
            BazelGlobalFunctionParameter(
              "registry",
              "\'\'",
              false,
              required = true,
              "Overrides the registry for this module; instead of finding this module from the default list of registries, the given registry should be used.",
            ),
          ),
        ),
      "override_repo" to
        BazelGlobalFunction(
          "override_repo",
          """
          Overrides one or more repos defined by the given module extension with the given repos visible to the current module. This is ignored if the current module is not the root module or `--ignore_dev_dependency` is enabled.
          Use inject_repo instead to add a new repo.                                                                                                                                                     , listOf()
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "extension_proxy",
              "\'\'",
              true,
              required = true,
              "A module extension proxy object returned by a use_extension call.",
            ),
          ),
        ),
      "register_execution_platforms" to
        BazelGlobalFunction(
          "register_execution_platforms",
          """
          Specifies already-defined execution platforms to be registered when this module is selected. Should be absolute target patterns (ie. beginning with either @ or //). See toolchain resolution for more information. Patterns that expand to multiple targets, such as :all, will be registered in lexicographical order by name.
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "dev_dependency",
              "False",
              false,
              required = false,
              "If true, the execution platforms will not be registered if the current module is not the root module or `--ignore_dev_dependency` is enabled.",
            ),
            BazelGlobalFunctionParameter(
              "platform_labels",
              "[]",
              false,
              required = true,
              "The target patterns to register.",
            ),
          ),
        ),
      "register_toolchains" to
        BazelGlobalFunction(
          "register_toolchains",
          """
          Specifies already-defined toolchains to be registered when this module is selected. Should be absolute target patterns (ie. beginning with either @ or //). See toolchain resolution for more information. Patterns that expand to multiple targets, such as :all, will be registered in lexicographical order by target name (not the name of the toolchain implementation).
          """.trimIndent(),
          listOf(
            BazelGlobalFunctionParameter(
              "dev_dependency",
              "False",
              false,
              required = false,
              "If true, the toolchains will not be registered if the current module is not the root module or `--ignore_dev_dependency` is enabled.",
            ),
            BazelGlobalFunctionParameter(
              "toolchain_labels",
              "[]",
              false,
              required = true,
              "The target patterns to register.",
            ),
          ),
        ),
      "single_version_override" to
        BazelGlobalFunction(
          name = "single_version_override",
          docString =
            """
            Specifies that a dependency should still come from a registry, but its version should be pinned, 
            or its registry overridden, or a list of patches applied. This directive only takes effect in the 
            root module; in other words, if a module is used as a dependency by others, its own overrides are ignored.
            """.trimIndent(),
          params =
            listOf(
              BazelGlobalFunctionParameter(
                name = "module_name",
                default = "",
                positional = true,
                required = true,
                docString = "The name of the Bazel module dependency to apply this override to.",
              ),
              BazelGlobalFunctionParameter(
                name = "version",
                default = "''",
                positional = false,
                docString = "Overrides the declared version of this module in the dependency graph.",
              ),
              BazelGlobalFunctionParameter(
                name = "registry",
                default = "''",
                positional = false,
                docString = "Overrides the registry for this module.",
              ),
              BazelGlobalFunctionParameter(
                name = "patches",
                default = "[]",
                positional = false,
                docString = "A list of labels pointing to patch files to apply for this module.",
              ),
              BazelGlobalFunctionParameter(
                name = "patch_cmds",
                default = "[]",
                positional = false,
                docString = "Sequence of Bash commands to be applied on Linux/MacOS after patches are applied.",
              ),
              BazelGlobalFunctionParameter(
                name = "patch_strip",
                default = "0",
                positional = false,
                docString = "Same as the --strip argument of Unix patch.",
              ),
            ),
        ),
      "use_extension" to
        BazelGlobalFunction(
          name = "use_extension",
          docString =
            """
            Returns a proxy object representing a module extension; its methods can be invoked to create 
            module extension tags.
            """.trimIndent(),
          params =
            listOf(
              BazelGlobalFunctionParameter(
                name = "extension_bzl_file",
                default = "",
                positional = true,
                required = true,
                docString = "A label to the Starlark file defining the module extension.",
              ),
              BazelGlobalFunctionParameter(
                name = "extension_name",
                default = "",
                positional = true,
                required = true,
                docString = "The name of the module extension to use. A symbol with this name must be exported by the Starlark file.",
              ),
              BazelGlobalFunctionParameter(
                name = "dev_dependency",
                default = "False",
                positional = false,
                """If true, this usage of the module extension will be ignored if the current module is not the root module or `--ignore_dev_dependency` is enabled.""",
              ),
              BazelGlobalFunctionParameter(
                name = "isolate",
                default = "False",
                positional = false,
                docString =
                  """
                  Experimental. If true, this usage of the module extension will be isolated from all other usages, 
                  both in this and other modules. Tags created for this usage do not affect other usages and the 
                  repositories generated by the extension for this usage will be distinct from all other repositories 
                  generated by the extension. This parameter is currently experimental and only available with the 
                  flag --experimental_isolated_extension_usages.
                  """.trimIndent(),
              ),
            ),
        ),
      "use_repo" to
        BazelGlobalFunction(
          name = "use_repo",
          docString =
            """
            Imports one or more repos generated by the given module extension into the scope of the current module.
            """.trimIndent(),
          params =
            listOf(
              BazelGlobalFunctionParameter(
                name = "extension_proxy",
                default = "",
                positional = true,
                required = true,
                docString = "A module extension proxy object returned by a use_extension call.",
              ),
            ),
        ),
      "use_repo_rule" to
        BazelGlobalFunction(
          name = "use_repo_rule",
          docString =
            """
            Returns a proxy value that can be directly invoked in the MODULE.bazel file as a repository rule, one or more times. 
            Repos created in such a way are only visible to the current module, under the name declared using the name attribute 
            on the proxy. The implicit Boolean dev_dependency attribute can also be used on the proxy to denote that a certain 
            repo is only to be created when the current module is the root module.
            """.trimIndent(),
          params =
            listOf(
              BazelGlobalFunctionParameter(
                name = "repo_rule_bzl_file",
                default = "",
                positional = true,
                required = true,
                docString = "A label to the Starlark file defining the repo rule.",
              ),
              BazelGlobalFunctionParameter(
                name = "repo_rule_name",
                default = "",
                positional = true,
                required = true,
                docString = "The name of the repo rule to use. A symbol with this name must be exported by the Starlark file.",
              ),
            ),
        ),
    )

  val WORKSPACE_FUNCTIONS =
    mapOf(
      "bind" to BazelGlobalFunction("bind"),
      "register_execution_platforms" to BazelGlobalFunction("register_execution_platforms"),
      "register_toolchains" to BazelGlobalFunction("register_toolchains"),
      "workspace" to BazelGlobalFunction("workspace"),
    )
}
