package org.jetbrains.bazel.test.stamper;

import org.jetbrains.bazel.test.Test;
import org.jetbrains.bazel.test.TestData;
import org.objectweb.asm.*;
import picocli.CommandLine;

import static picocli.CommandLine.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class TestStamper {
  static class Cli {
    @Option(names = "--output")
    Path output;

    @Parameters(paramLabel = "FILE")
    Path[] inputFiles;
  }

  public static void main(String[] args) throws IOException {
    final var cli = new Cli();
    new CommandLine(cli).parseArgs(args);
    new Runner(cli).run();
  }

  record Runner(Cli cli) {
    private static final Set<String> TEST_MARKER_ANNOTATIONS
      = Set.of("org/jetbrains/bazel/test/framework/annotation/BazelTest");
    private static final String TEST_CLASSES_FILE_NAME = "test_metadata.proto";

    record Ctx(TestData.Builder builder) {
    }

    public void run() throws IOException {
      final var archives = Arrays.stream(cli.inputFiles)
        .filter(it -> it.toString().endsWith(".jar"))
        .filter(Files::exists)
        .toList();
      final var ctx = new Ctx(TestData.newBuilder());
      for (var archive : archives) {
        // this is faster than NIO
        try (final var zip = new ZipFile(archive.toFile())) {
          final var entries = zip.entries();
          while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
              continue;
            }
            // accept only class files
            if (!entry.getName().endsWith(".class")) {
              continue;
            }
            try (final var entryStream = zip.getInputStream(entry)) {
              this.processClassFile(ctx, entryStream);
            }
          }
        }
      }

      try (final var outArchive = new ZipOutputStream(Files.newOutputStream(cli.output))) {
        final var data = ctx.builder.build();
        outArchive.putNextEntry(new ZipEntry(TEST_CLASSES_FILE_NAME));
        outArchive.write(data.toByteArray());
      }
    }

    private void processClassFile(Ctx ctx, InputStream stream) throws IOException {
      final var reader = new ClassReader(stream);
      final var visitor = new Visitor(ctx);
      // only read class file metadata
      reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
    }

    private static class Visitor extends ClassVisitor {
      private final Ctx ctx;

      private String className;
      private boolean marked;

      public Visitor(Ctx ctx) {
        super(Opcodes.ASM9);
        this.ctx = ctx;
      }

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
      }

      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        final var orig = super.visitAnnotation(descriptor, visible);
        final var internalName = Type.getType(descriptor).getInternalName();
        if (TEST_MARKER_ANNOTATIONS.contains(internalName)) {
          final var builder = Test.newBuilder()
            .setClassName(this.className.replace("/", "."))
            .setIsIdeStarter(false);
          return new TestAnnotationVisitor(builder) {
            @Override
            public void visitEnd() {
              super.visitEnd();
              ctx.builder.addTests(builder);
              marked = true;
            }
          };
        }
        return orig;
      }

      @Override
      public void visitEnd() {
        if (!this.marked) {
          if (this.className.endsWith("Test")) {
            final var test = Test.newBuilder()
              .setClassName(this.className.replace("/", "."))
              .setIsIdeStarter(false);
            this.ctx.builder.addTests(test);
          }
        }
        super.visitEnd();
      }
    }

    private static class TestAnnotationVisitor extends AnnotationVisitor {

      private final Test.Builder builder;

      public TestAnnotationVisitor(Test.Builder builder) {
        super(Opcodes.ASM9);
        this.builder = builder;
      }

      @Override
      public void visitEnum(String name, String descriptor, String value) {
        super.visitEnum(name, descriptor, value);
        if ("kind".equals(name)) {
          if (value.equals("UNIT_TEST")) {
            this.builder.setIsIdeStarter(false);
          }
          else if (value.equals("IDE_STARTER")) {
            this.builder.setIsIdeStarter(true);
          }
        }
      }
    }
  }
}