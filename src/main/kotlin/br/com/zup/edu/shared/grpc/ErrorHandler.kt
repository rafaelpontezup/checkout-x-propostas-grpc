package br.com.zup.edu.shared.grpc

import io.micronaut.aop.Around

@Around
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ErrorHandler()
