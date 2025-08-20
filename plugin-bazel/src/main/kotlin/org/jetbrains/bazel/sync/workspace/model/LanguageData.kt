package org.jetbrains.bazel.sync.workspace.model

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
interface LanguageData
