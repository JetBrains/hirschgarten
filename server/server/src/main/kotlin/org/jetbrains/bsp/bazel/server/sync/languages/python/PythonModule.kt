package org.jetbrains.bsp.bazel.server.sync.languages.python

import java.net.URI
import org.jetbrains.bsp.bazel.server.model.LanguageData

data class PythonModule(val interpreter: URI?, val version: String?) : LanguageData
