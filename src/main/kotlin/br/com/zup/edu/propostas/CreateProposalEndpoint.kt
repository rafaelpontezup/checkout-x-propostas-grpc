package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import com.google.protobuf.Any
import com.google.protobuf.Timestamp
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@Singleton
open class CreateProposalEndpoint(@Inject val repository: ProposalRespository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

        if (repository.existsByDocument(request.document)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                                .withDescription("proposal already exists")
                                .asRuntimeException())
            return // it's important to stop the flow
        }

        val proposal = try {
            repository.save(request.toModel())
        } catch (e: ConstraintViolationException) {

            LOGGER.error(e.message)

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
                .addDetails(Any.pack(badRequest)) // com.google.protobuf.Any
                .build()

            LOGGER.info("$statusProto")
            responseObserver.onError(StatusProto.toStatusRuntimeException(statusProto)) // io.grpc.protobuf.StatusProto
            return // it's important to stop the flow
        }

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(proposal.id.toString())
                                        .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                        .build())
        responseObserver.onCompleted()
    }

}

/**
 * Extension methods
 */

fun CreateProposalRequest.toModel(): Proposal {
    return Proposal(
        document = document,
        email = email,
        name = name,
        address = address,
        salary = BigDecimal(salary)
    )
}

fun LocalDateTime.toGrpcTimestamp(): Timestamp {
    val instant = this.atZone(ZoneId.of("UTC")).toInstant()
    return Timestamp.newBuilder()
                    .setSeconds(instant.epochSecond)
                    .setNanos(instant.nano)
                    .build()
}