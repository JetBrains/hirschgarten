/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.io

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtilRt
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.util.function.BiPredicate
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.setLastModifiedTime

/** File system operations. Mocked out in tests involving file manipulations.  */
open class FileOperationProvider {
  fun exists(file: Path): Boolean {
    return file.exists()
  }

  fun isDirectory(file: Path): Boolean {
    return file.isDirectory()
  }

  fun isFile(file: Path): Boolean {
    return file.isRegularFile()
  }

  fun getFileModifiedTime(file: Path): FileTime {
    return file.getLastModifiedTime()
  }

  fun setFileModifiedTime(file: Path, time: FileTime) {
    file.setLastModifiedTime(time)
  }

  fun getFileSize(file: Path): Long {
    return file.fileSize()
  }

  fun listFiles(file: Path): List<Path> {
    return file.listDirectoryEntries()
  }

  @Throws(IOException::class)
  fun createSymbolicLink(link: Path, target: Path) {
    Files.createSymbolicLink(link, target)
  }

  @Throws(IOException::class)
  fun readSymbolicLink(link: Path): Path? {
    return Files.readSymbolicLink(link)
  }

  fun isSymbolicLink(file: Path): Boolean {
    return Files.isSymbolicLink(file)
  }

  @Throws(IOException::class)
  fun getCanonicalFile(file: Path): Path? {
    return file.normalize()
  }

  @Throws(IOException::class)
  fun createTempFile(
    tempDirectory: Path, prefix: String?, suffix: String?, vararg attributes: FileAttribute<*>,
  ): Path {
    return Files.createTempFile(tempDirectory, prefix, suffix, *attributes)
  }

  @Throws(IOException::class)
  fun copy(source: Path, target: Path, vararg options: CopyOption): Path {
    return Files.copy(source, target, *options)
  }

  fun mkdirs(file: Path): Path {
    return  Files.createDirectories(file)
  }

  /** Walk through the directory and return all meet requirement files in it.  */
  @Throws(IOException::class)
  fun listFilesRecursively(
    dir: Path, maxDepth: Int, matcher: BiPredicate<Path, BasicFileAttributes>,
  ): List<Path> {
    Files.find(dir, maxDepth, matcher).use { stream ->
      return stream.collect(Collectors.toList())
    }
  }

  /**
   * Deletes the file or directory at the given path recursively, allowing insecure delete according
   * to `allowInsecureDelete`.
   *
   * @see RecursiveDeleteOption.ALLOW_INSECURE
   */
  @JvmOverloads
  @Throws(IOException::class)
  fun deleteRecursively(file: Path, allowInsecureDelete: Boolean = false) {
    if (allowInsecureDelete) {
      MoreFiles.deleteRecursively(file, RecursiveDeleteOption.ALLOW_INSECURE)
    } else {
      MoreFiles.deleteRecursively(file)
    }
  }

  /**
   * Deletes all files within the directory at the given path recursively, allowing insecure delete
   * according to `allowInsecureDelete`. Does not delete the directory itself.
   *
   * @see RecursiveDeleteOption.ALLOW_INSECURE
   */
  @Throws(IOException::class)
  fun deleteDirectoryContents(file: Path, allowInsecureDelete: Boolean) {
    if (allowInsecureDelete) {
      MoreFiles.deleteDirectoryContents(file, RecursiveDeleteOption.ALLOW_INSECURE)
    } else {
      MoreFiles.deleteDirectoryContents(file)
    }
  }

  // If the file is too big, this method can blow up RAM as it reads the file contents
  // entirely into memory. Only use this for small files.
  @Throws(IOException::class)
  fun readAllLines(file: Path): List<String> {
    return Files.readAllLines(file)
  }

  fun getTempDirectory(): Path {
    return Paths.get(FileUtilRt.getTempDirectory());
  }

  companion object {
    @JvmStatic
    val instance: FileOperationProvider
      get() = ApplicationManager.getApplication()
        .getService(FileOperationProvider::class.java)
  }
}
