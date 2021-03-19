package br.com.zup.edu.shared.grpc

import io.micronaut.aop.Around
import io.micronaut.context.annotation.Type

@Around
@Type(ExceptionHandlerInterceptor::class)
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ErrorHandler()
