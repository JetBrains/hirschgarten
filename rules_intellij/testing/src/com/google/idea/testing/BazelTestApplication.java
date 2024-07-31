package com.google.idea.testing;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@TestOnly
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(BazelTestApplicationExtension.class)
public @interface BazelTestApplication {}
