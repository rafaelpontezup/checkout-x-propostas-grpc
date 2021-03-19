package br.com.zup.edu.shared.grpc

import br.com.zup.edu.propostas.ProposalAlreadyExistsException
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.BindableService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class)
class ExceptionHandlerInterceptor : MethodInterceptor<BindableService, Any?> {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun intercept(context: MethodInvocationContext<BindableService, Any?>): Any? {
        try {
            return context.proceed()
        } catch (e: Exception) {
            LOGGER.error("${e.message}")

            val statusError = when(e) {
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is ProposalAlreadyExistsException -> Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException()
                is ConstraintViolationException -> handleConstraintViolationException(e)
                else -> Status.UNKNOWN.withDescription("unexepected error happened").asRuntimeException()
            }

            val responseObserver = context.parameterValues[1] as StreamObserver<*>
            responseObserver.onError(statusError)
            return null
        }
    }

    private fun handleConstraintViolationException(e: ConstraintViolationException): StatusRuntimeException {
        LOGGER.error("${e.message}")

        val badRequest = BadRequest.newBuilder()
            .addAllFieldViolations(e.constraintViolations.map {
                BadRequest.FieldViolation.newBuilder()
                    .setField(it.propertyPath.last().name) // save.entity.document -> document
                    .setDescription(it.message) // document must not be blank
                    .build()
            })
            .build()

        val statusProto = com.google.rpc.Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("request with invalid parameters")
            .addDetails(com.google.protobuf.Any.pack(badRequest)) // details = lista de detail
            .build()

        LOGGER.error("${statusProto}")
        return StatusProto.toStatusRuntimeException(statusProto)
    }

}