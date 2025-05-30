package org.jetbrains.bazel.server.bep;

import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BepStreamObserver implements StreamObserver<PublishBuildToolEventStreamRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BepStreamObserver.class);

  private static final String BUILD_EVENT_TYPE_URL =
      "type.googleapis.com/build_event_stream.BuildEvent";

  private final BepServer bepServer;
  private final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver;

  public BepStreamObserver(
      BepServer bepServer, StreamObserver<PublishBuildToolEventStreamResponse> responseObserver) {
    this.bepServer = bepServer;
    this.responseObserver = responseObserver;
  }

  @Override
  public void onNext(PublishBuildToolEventStreamRequest request) {
    if (isRequestBazelBuildEvent(request)) {
      bepServer.handleEvent(request.getOrderedBuildEvent().getEvent());
    }

    PublishBuildToolEventStreamResponse response =
        PublishBuildToolEventStreamResponse.newBuilder()
            .setStreamId(request.getOrderedBuildEvent().getStreamId())
            .setSequenceNumber(request.getOrderedBuildEvent().getSequenceNumber())
            .build();

    responseObserver.onNext(response);
  }

  @Override
  public void onError(Throwable throwable) {
    LOGGER.debug("Error from BEP stream: {}", throwable.toString());
  }

  @Override
  public void onCompleted() {
    responseObserver.onCompleted();
  }

  private boolean isRequestBazelBuildEvent(PublishBuildToolEventStreamRequest request) {
    return request
        .getOrderedBuildEvent()
        .getEvent()
        .getBazelEvent()
        .getTypeUrl()
        .equals(BUILD_EVENT_TYPE_URL);
  }
}
