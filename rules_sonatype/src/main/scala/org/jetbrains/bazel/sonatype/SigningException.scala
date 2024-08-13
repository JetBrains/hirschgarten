package org.jetbrains.bazel
package sonatype

import java.io.IOException

final case class SigningException(message: String) extends IOException(message)