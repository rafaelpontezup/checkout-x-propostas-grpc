package br.com.zup.edu.shared.grpc

import br.com.zup.edu.propostas.CreateProposalEndpoint
import br.com.zup.edu.propostas.ProposalAlreadyExistsException
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class) // supported from Micronaut 2.4 onwards (https://docs.micronaut.io/2.4.0/guide/index.html#aop)
class ExceptionHandlerInterceptor : MethodInterceptor<CreateProposalEndpoint, Any?> { // or BindableService

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun intercept(context: MethodInvocationContext<CreateProposalEndpoint, Any?>): Any? {
        try {
            return context.proceed()
        } catch (e: Exception) {
            LOGGER.error(e.message)

            /**
             * That's a simple solution and in my opinion it's good enough for a microservice.
             *
             * But if you want a better or reusable solution you can use OO and polymorphism to make it
             * more flexible as you can see here:
             *
             * - https://gist.github.com/rponte/8f23557d7e3079c9e644e620df293420#file-a01_exceptionhandlerinterceptor-kt-L26
             */
            val statusError = when (e) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
                is ConstraintViolationException -> handleConstraintValidationException(e)
                is ProposalAlreadyExistsException -> Status.ALREADY_EXISTS.withDescription(e.message).asRuntimeException()
                else -> Status.UNKNOWN.withDescription("unexpected error happened").asRuntimeException()
            }

            val responseObserver = context.parameterValues[1] as StreamObserver<*>
            responseObserver.onError(statusError)
            return null
        }
    }

    private fun handleConstraintValidationException(e: ConstraintViolationException): StatusRuntimeException {
        val badRequest = BadRequest.newBuilder() // com.google.rpc.BadRequest
            .addAllFieldViolations(e.constraintViolations.map {
                BadRequest.FieldViolation.newBuilder()
                    .setField(it.propertyPath.last().name) // propertyPath=save.entity.email
                    .setDescription(it.message)
                    .build()
            }
            ).build()

        val statusProto = com.google.rpc.Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage("request with invalid parameters")
            .addDetails(com.google.protobuf.Any.pack(badRequest)) // com.google.protobuf.Any
            .build()

        LOGGER.info("$statusProto")
        return StatusProto.toStatusRuntimeException(statusProto) // io.grpc.protobuf.StatusProto
    }

}