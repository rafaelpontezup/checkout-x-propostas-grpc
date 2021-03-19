package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
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

        val proposal = request.toModel()
        try {
            repository.save(proposal)
        } catch (e: ConstraintViolationException) {
            LOGGER.error(e.message)
            responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("request with invalid parameters")
                            .withCause(e) // it's NOT sent to the client
                            .asRuntimeException())
            return // XXX: it's important to stop the flow
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