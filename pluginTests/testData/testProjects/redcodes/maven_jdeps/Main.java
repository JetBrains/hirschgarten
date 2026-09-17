package com.example;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class Main {
    void foo() {
        // DefaultCredentialsProvider inherits from ToCopyableBuilder which defines the copy() method
        // But ToCopyableBuilder is from a transitive dep (@maven//:software_amazon_awssdk_utils)
        // rules_java allows this as long as we don't mention the class name directly
        DefaultCredentialsProvider.create().copy(builder -> {});
    }
}
