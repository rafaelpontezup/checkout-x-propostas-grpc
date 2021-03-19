package br.com.zup.edu.shared.grpc

import io.micronaut.aop.Around
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Around
@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
annotation class ErrorHandler {

}
