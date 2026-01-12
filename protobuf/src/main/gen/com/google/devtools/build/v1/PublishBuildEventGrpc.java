package com.google.devtools.build.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: google/devtools/build/v1/publish_build_event.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class PublishBuildEventGrpc {

  private PublishBuildEventGrpc() {}

  public static final java.lang.String SERVICE_NAME = "google.devtools.build.v1.PublishBuildEvent";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishLifecycleEventRequest,
      com.google.protobuf.Empty> getPublishLifecycleEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishLifecycleEvent",
      requestType = com.google.devtools.build.v1.PublishLifecycleEventRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishLifecycleEventRequest,
      com.google.protobuf.Empty> getPublishLifecycleEventMethod() {
    io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishLifecycleEventRequest, com.google.protobuf.Empty> getPublishLifecycleEventMethod;
    if ((getPublishLifecycleEventMethod = PublishBuildEventGrpc.getPublishLifecycleEventMethod) == null) {
      synchronized (PublishBuildEventGrpc.class) {
        if ((getPublishLifecycleEventMethod = PublishBuildEventGrpc.getPublishLifecycleEventMethod) == null) {
          PublishBuildEventGrpc.getPublishLifecycleEventMethod = getPublishLifecycleEventMethod =
              io.grpc.MethodDescriptor.<com.google.devtools.build.v1.PublishLifecycleEventRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishLifecycleEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.devtools.build.v1.PublishLifecycleEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new PublishBuildEventMethodDescriptorSupplier("PublishLifecycleEvent"))
              .build();
        }
      }
    }
    return getPublishLifecycleEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest,
      com.google.devtools.build.v1.PublishBuildToolEventStreamResponse> getPublishBuildToolEventStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishBuildToolEventStream",
      requestType = com.google.devtools.build.v1.PublishBuildToolEventStreamRequest.class,
      responseType = com.google.devtools.build.v1.PublishBuildToolEventStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest,
      com.google.devtools.build.v1.PublishBuildToolEventStreamResponse> getPublishBuildToolEventStreamMethod() {
    io.grpc.MethodDescriptor<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest, com.google.devtools.build.v1.PublishBuildToolEventStreamResponse> getPublishBuildToolEventStreamMethod;
    if ((getPublishBuildToolEventStreamMethod = PublishBuildEventGrpc.getPublishBuildToolEventStreamMethod) == null) {
      synchronized (PublishBuildEventGrpc.class) {
        if ((getPublishBuildToolEventStreamMethod = PublishBuildEventGrpc.getPublishBuildToolEventStreamMethod) == null) {
          PublishBuildEventGrpc.getPublishBuildToolEventStreamMethod = getPublishBuildToolEventStreamMethod =
              io.grpc.MethodDescriptor.<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest, com.google.devtools.build.v1.PublishBuildToolEventStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishBuildToolEventStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.devtools.build.v1.PublishBuildToolEventStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.devtools.build.v1.PublishBuildToolEventStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PublishBuildEventMethodDescriptorSupplier("PublishBuildToolEventStream"))
              .build();
        }
      }
    }
    return getPublishBuildToolEventStreamMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PublishBuildEventStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventStub>() {
        @java.lang.Override
        public PublishBuildEventStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublishBuildEventStub(channel, callOptions);
        }
      };
    return PublishBuildEventStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static PublishBuildEventBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventBlockingV2Stub>() {
        @java.lang.Override
        public PublishBuildEventBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublishBuildEventBlockingV2Stub(channel, callOptions);
        }
      };
    return PublishBuildEventBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PublishBuildEventBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventBlockingStub>() {
        @java.lang.Override
        public PublishBuildEventBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublishBuildEventBlockingStub(channel, callOptions);
        }
      };
    return PublishBuildEventBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PublishBuildEventFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PublishBuildEventFutureStub>() {
        @java.lang.Override
        public PublishBuildEventFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PublishBuildEventFutureStub(channel, callOptions);
        }
      };
    return PublishBuildEventFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void publishLifecycleEvent(com.google.devtools.build.v1.PublishLifecycleEventRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishLifecycleEventMethod(), responseObserver);
    }

    /**
     */
    default io.grpc.stub.StreamObserver<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest> publishBuildToolEventStream(
        io.grpc.stub.StreamObserver<com.google.devtools.build.v1.PublishBuildToolEventStreamResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getPublishBuildToolEventStreamMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PublishBuildEvent.
   */
  public static abstract class PublishBuildEventImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PublishBuildEventGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PublishBuildEvent.
   */
  public static final class PublishBuildEventStub
      extends io.grpc.stub.AbstractAsyncStub<PublishBuildEventStub> {
    private PublishBuildEventStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublishBuildEventStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublishBuildEventStub(channel, callOptions);
    }

    /**
     */
    public void publishLifecycleEvent(com.google.devtools.build.v1.PublishLifecycleEventRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishLifecycleEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest> publishBuildToolEventStream(
        io.grpc.stub.StreamObserver<com.google.devtools.build.v1.PublishBuildToolEventStreamResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getPublishBuildToolEventStreamMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PublishBuildEvent.
   */
  public static final class PublishBuildEventBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<PublishBuildEventBlockingV2Stub> {
    private PublishBuildEventBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublishBuildEventBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublishBuildEventBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty publishLifecycleEvent(com.google.devtools.build.v1.PublishLifecycleEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishLifecycleEventMethod(), getCallOptions(), request);
    }

    /**
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<com.google.devtools.build.v1.PublishBuildToolEventStreamRequest, com.google.devtools.build.v1.PublishBuildToolEventStreamResponse>
        publishBuildToolEventStream() {
      return io.grpc.stub.ClientCalls.blockingBidiStreamingCall(
          getChannel(), getPublishBuildToolEventStreamMethod(), getCallOptions());
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service PublishBuildEvent.
   */
  public static final class PublishBuildEventBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PublishBuildEventBlockingStub> {
    private PublishBuildEventBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublishBuildEventBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublishBuildEventBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty publishLifecycleEvent(com.google.devtools.build.v1.PublishLifecycleEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishLifecycleEventMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PublishBuildEvent.
   */
  public static final class PublishBuildEventFutureStub
      extends io.grpc.stub.AbstractFutureStub<PublishBuildEventFutureStub> {
    private PublishBuildEventFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PublishBuildEventFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PublishBuildEventFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> publishLifecycleEvent(
        com.google.devtools.build.v1.PublishLifecycleEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishLifecycleEventMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUBLISH_LIFECYCLE_EVENT = 0;
  private static final int METHODID_PUBLISH_BUILD_TOOL_EVENT_STREAM = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH_LIFECYCLE_EVENT:
          serviceImpl.publishLifecycleEvent((com.google.devtools.build.v1.PublishLifecycleEventRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH_BUILD_TOOL_EVENT_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.publishBuildToolEventStream(
              (io.grpc.stub.StreamObserver<com.google.devtools.build.v1.PublishBuildToolEventStreamResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPublishLifecycleEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.devtools.build.v1.PublishLifecycleEventRequest,
              com.google.protobuf.Empty>(
                service, METHODID_PUBLISH_LIFECYCLE_EVENT)))
        .addMethod(
          getPublishBuildToolEventStreamMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.google.devtools.build.v1.PublishBuildToolEventStreamRequest,
              com.google.devtools.build.v1.PublishBuildToolEventStreamResponse>(
                service, METHODID_PUBLISH_BUILD_TOOL_EVENT_STREAM)))
        .build();
  }

  private static abstract class PublishBuildEventBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PublishBuildEventBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.google.devtools.build.v1.BackendProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PublishBuildEvent");
    }
  }

  private static final class PublishBuildEventFileDescriptorSupplier
      extends PublishBuildEventBaseDescriptorSupplier {
    PublishBuildEventFileDescriptorSupplier() {}
  }

  private static final class PublishBuildEventMethodDescriptorSupplier
      extends PublishBuildEventBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PublishBuildEventMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PublishBuildEventGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PublishBuildEventFileDescriptorSupplier())
              .addMethod(getPublishLifecycleEventMethod())
              .addMethod(getPublishBuildToolEventStreamMethod())
              .build();
        }
      }
    }
    return result;
  }
}
