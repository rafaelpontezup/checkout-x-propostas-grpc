package br.com.zup.edu

import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@Singleton
open class CreateProposalEndpoint(@Inject val repository: ProposalRepository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    val LOGGER: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("request: $request")

        val proposal = Proposal(
            document = request.document,
            name = request.name,
            email = request.email,
            address = request.address,
            salary = BigDecimal(request.salary)
        )

        try {
            repository.save(proposal)
        } catch (e: ConstraintViolationException) {
            LOGGER.error(e.message, e)
            responseObserver.onError(Status.INVALID_ARGUMENT
                                            .withDescription("invalid parameters")
                                            .withCause(e)
                                            .asRuntimeException())
            return
        }

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                                .setProposalId(proposal.id.toString())
                                                .setCreatedAt(proposal.createdAt.let {
                                                    val instant = it.atZone(ZoneId.of("UTC")).toInstant()
                                                    Timestamp.newBuilder()
                                                        .setSeconds(instant.epochSecond)
                                                        .setNanos(instant.nano)
                                                        .build()
                                                })
                                                .build())
        responseObserver.onCompleted()
    }

}