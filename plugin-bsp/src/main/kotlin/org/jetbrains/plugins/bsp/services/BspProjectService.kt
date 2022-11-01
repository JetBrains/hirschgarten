package org.jetbrains.plugins.bsp.services

// TODO: we should have some basic project service class, but i dont know yet what should be there (???)

public class UninitializedServiceVariableException(
  propertyName: String,
  serviceName: String?,
) : IllegalStateException(
  "Property '$propertyName' in service '$serviceName' is not initialized! " +
    "Service hasn't been initialized yet or the service state is invalid."
)
