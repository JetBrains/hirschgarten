package com.example;

import example.common.Environment;
import example.common.Metadata;
import example.service.ServiceRequest;
import com.google.protobuf.Timestamp;

class Main {
  public Environment environment = Environment.PRODUCTION;

  public Metadata metadata = Metadata.newBuilder()
          .setEnvironment(environment)
          .setDescription("Example service request")
          .build();

  public ServiceRequest serviceRequest = ServiceRequest.newBuilder()
          .setRequestId("req-456")
          .setPriority(1500.50)
          .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
          .setMetadata(metadata)
          .build();
}
