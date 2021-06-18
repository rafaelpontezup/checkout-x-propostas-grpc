package br.com.zup.edu

import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.transaction.SynchronousTransactionManager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@ErrorHandler
@Singleton
class CreateProposalEndpoint(
    @Inject private val repository: ProposalRepository,
    @Inject val transactionManager: SynchronousTransactionManager<Any> // PlatformTransactionManager
) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("new request: $request")

        /**
         * 1. auto-commit do repository
         * 2. extrair logica para um Service com @Transactional
         * 3. controle transacional programatico
         */
        val proposal = transactionManager.executeWrite { // transacao

            if (repository.existsByDocument(request.document)) { // participar tx
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            repository.save(request.toModel()) // participar tx
        } // commit

        // response
        val response = CreateProposalResponse.newBuilder()
                                    .setId(proposal.id.toString())
                                    .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                    .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
    // commit -> INSERT

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