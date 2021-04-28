package br.com.zup.edu.shared.grpc

import io.micronaut.aop.Around
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@Around
@MustBeDocumented
@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
annotation class ErrorHandler()
