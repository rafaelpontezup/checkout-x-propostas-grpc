 package br.com.zup.edu.shared.grpc

import br.com.zup.edu.CreateProposalEndpoint
import br.com.zup.edu.ProposalAlreadyExistsException
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorHandler::class)
class ExceptionHandlerInterceptor : MethodInterceptor<CreateProposalEndpoint, Any?> {

    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun intercept(context: MethodInvocationContext<CreateProposalEndpoint, Any?>): Any? {
        try {
            return context.proceed()
        } catch (t: Throwable) {
            LOGGER.error("Handling exception thrown by: ${context.targetMethod}", t)

            val statusError = when(t) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(t.message).asRuntimeException()
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(t.message).asRuntimeException()
                is ProposalAlreadyExistsException -> Status.ALREADY_EXISTS.withDescription(t.message).asRuntimeException()
                is ConstraintViolationException -> handleContraintViolationException(t)
                else -> Status.UNKNOWN.withDescription("unexpected error happened").asRuntimeException()
            }

            val responseObserver = context.parameterValues[1] as StreamObserver<*>
            responseObserver.onError(statusError)
            return null
        }
    }

     private fun handleContraintViolationException(e: ConstraintViolationException): StatusRuntimeException {

         val badRequest = BadRequest.newBuilder()
             .addAllFieldViolations(e.constraintViolations.map {
                 BadRequest.FieldViolation.newBuilder()
                     .setField(it.propertyPath.last().name) // save.entity.document -> document
                     .setDescription(it.message)
                     .build()
             })
             .build()

         val statusProto = com.google.rpc.Status.newBuilder()
             .setCode(Code.INVALID_ARGUMENT_VALUE)
             .setMessage("invalid parameters")
             .addDetails(com.google.protobuf.Any.pack(badRequest)) // qualquer coisa!
             .build()

         LOGGER.info("statusProto: ${statusProto}")

         val exception = io.grpc.protobuf.StatusProto.toStatusRuntimeException(statusProto)
         return exception
     }

}