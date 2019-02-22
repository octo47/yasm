package com.github.octo47.yasm;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * Indicates that fields, method parameters, method return types, and type parameters
 * within a package are {@link Nonnull} unless explicitly annotated with {@link Nullable}.
 * This annotation is a generalization of {@link javax.annotation.ParametersAreNonnullByDefault}.
 */
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface ElementsAreNonnullByDefault {
}
