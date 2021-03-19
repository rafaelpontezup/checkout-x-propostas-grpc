package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
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
open class CreateProposalEndpoint(@Inject val repository: ProposalRespository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("$request")

        if (repository.existsByDocument(request.document)) {
            throw ProposalAlreadyExistsException("proposal already exists")
        }

        val proposal = repository.save(request.toModel()) // auto-commit=false

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(proposal.id.toString())
                                        .setCreatedAt(proposal.createdAt.toGrpTimestamp())
                                        .build())
        responseObserver.onCompleted()
    }

}

/**
 * Extension Methods
 */

fun CreateProposalRequest.toModel(): Proposal {
    return Proposal(
        document = this.document,
        email = this.email,
        name = this.name,
        address = this.address,
        salary = BigDecimal(this.salary)
    )
}

fun LocalDateTime.toGrpTimestamp() : Timestamp {
    return this.let {
        val instant = it.atZone(ZoneId.of("UTC")).toInstant()
        Timestamp
            .newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
    }
}