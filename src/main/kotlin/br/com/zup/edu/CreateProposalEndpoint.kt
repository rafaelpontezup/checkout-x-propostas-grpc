package br.com.zup.edu

import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@ErrorHandler
@Singleton
open class CreateProposalEndpoint(@Inject private val repository: ProposalRepository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("new request: $request")

        if (repository.existsByDocument(request.document)) {
            throw ProposalAlreadyExistsException("proposal already exists")
        }

        val proposal = repository.save(request.toModel())

        // response
        val response = CreateProposalResponse.newBuilder()
                                    .setId(proposal.id.toString())
                                    .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                    .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}

fun CreateProposalRequest.toModel() : Proposal {
    return Proposal(
        name = this.name,
        document = this.document,
        email = this.email,
        address = this.address,
        salary = BigDecimal(this.salary)
    )
}

fun LocalDateTime.toGrpcTimestamp() : Timestamp {
    val instant = this.atZone(ZoneId.of("UTC")).toInstant()
    return Timestamp.newBuilder()
        .setSeconds(instant.epochSecond)
        .setNanos(instant.nano)
        .build()
}