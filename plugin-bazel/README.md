[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Bazel by JetBrains for IntelliJ IDEA

<!-- Plugin description -->
Bazel by JetBrains for IntelliJ IDEA.

This plugin lets you import and work with Bazel projects with IntelliJ IDEA, GoLand, and PyCharm.

It supports working with Bazel projects that include Java, Kotlin, Scala, Python, and Go targets. Support for other 
languages and frameworks is planned and will be released over time.

As of the 2025.2 release of IntelliJ IDEA, this plugin is generally available and running on production deployments on 
large enterprise repositories.

## Features

- open Bazel projects by selecting a folder
- define project views to open a focused subset of large projects
- automatically detect project model changes and offer to synchronize
- target tree to search for and run actions on Bazel targets
- run and debug runnable and testable targets from target tree or gutter icons
- test result display
- Starlark support, including syntax highlighting, completion, navigation
- Starlark debugging

To learn more about available features, please check the [documentation](https://www.jetbrains.com/help/idea/bazel.html).

<!-- Plugin description end -->


## Installation

1. in IntelliJ IDEA, go to Preferences | Plugins
2. Go to Marketplace tab.
3. Then look for "Bazel" (check the description to be sure you don't install the legacy plugin version) and click Install.
4. Restart IntelliJ IDEA. You're ready!

## Building, running, contributing changes

Please follow the [Plugin development setup guide](https://github.com/JetBrains/hirschgarten/blob/main/docs/dev/development_setup.md)
