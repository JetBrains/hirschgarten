package com.example

import example.common.{Environment, Metadata}
import example.service.ServiceRequest
import com.google.protobuf.timestamp.Timestamp

object Main {
  val environment = Environment.PRODUCTION

  val metadata = Metadata(
    environment = Environment.STAGING,
    description = "Example service request"
  )

  val serviceRequest = ServiceRequest(
    requestId = "req-456",
    priority = 1500.50,
    createdAt = Some(Timestamp(seconds = System.currentTimeMillis() / 1000)),
    metadata = Some(metadata)
  )
}
