// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: google/devtools/build/v1/build_events.proto
// Protobuf Java Version: 4.29.5

package com.google.devtools.build.v1;

/**
 * <pre>
 * The type of console output stream.
 * </pre>
 *
 * Protobuf enum {@code google.devtools.build.v1.ConsoleOutputStream}
 */
public enum ConsoleOutputStream
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <pre>
   * Unspecified or unknown.
   * </pre>
   *
   * <code>UNKNOWN = 0;</code>
   */
  UNKNOWN(0),
  /**
   * <pre>
   * Normal output stream.
   * </pre>
   *
   * <code>STDOUT = 1;</code>
   */
  STDOUT(1),
  /**
   * <pre>
   * Error output stream.
   * </pre>
   *
   * <code>STDERR = 2;</code>
   */
  STDERR(2),
  UNRECOGNIZED(-1),
  ;

  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 29,
      /* patch= */ 5,
      /* suffix= */ "",
      ConsoleOutputStream.class.getName());
  }
  /**
   * <pre>
   * Unspecified or unknown.
   * </pre>
   *
   * <code>UNKNOWN = 0;</code>
   */
  public static final int UNKNOWN_VALUE = 0;
  /**
   * <pre>
   * Normal output stream.
   * </pre>
   *
   * <code>STDOUT = 1;</code>
   */
  public static final int STDOUT_VALUE = 1;
  /**
   * <pre>
   * Error output stream.
   * </pre>
   *
   * <code>STDERR = 2;</code>
   */
  public static final int STDERR_VALUE = 2;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static ConsoleOutputStream valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static ConsoleOutputStream forNumber(int value) {
    switch (value) {
      case 0: return UNKNOWN;
      case 1: return STDOUT;
      case 2: return STDERR;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<ConsoleOutputStream>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      ConsoleOutputStream> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<ConsoleOutputStream>() {
          public ConsoleOutputStream findValueByNumber(int number) {
            return ConsoleOutputStream.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return com.google.devtools.build.v1.BuildEventProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final ConsoleOutputStream[] VALUES = values();

  public static ConsoleOutputStream valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private ConsoleOutputStream(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:google.devtools.build.v1.ConsoleOutputStream)
}

