package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import br.com.zup.edu.integration.FinancialClient
import br.com.zup.edu.integration.SubmitForAnalysisRequest
import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.transaction.SynchronousTransactionManager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@ErrorHandler
@Singleton
open class CreateProposalEndpoint(
    @Inject val repository: ProposalRespository,
    @Inject val financialClient: FinancialClient,
    @Inject val transactionManager: SynchronousTransactionManager<Connection>
    ) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

//    @Transactional // nao use com endpoints gRPC
    // abrir tx
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

        /**
         * Alternativas a @Transactional no endpoint:
         *
         * 1. auto-commit=true -> deixa o repository commitar
         * 2. usar uma classe de Service + @Transactional
         * 3. controle transacional programatico
         */
        val proposal = transactionManager.executeWrite { // auto-commit=false

            if (repository.existsByDocument(request.document)) {
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            val proposal = repository.save(request.toModel())

            // integra com API financeira
            val status = submitForAnalysis(proposal)
            proposal.updateStatus(newStatus = status)
        } // commit

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(proposal.id.toString())
                                        .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                        .build())
        responseObserver.onCompleted()

        // 1-100ms
    }

    private fun submitForAnalysis(proposal: Proposal): ProposalStatus {

        val response = financialClient.submitForAnalysis(
            SubmitForAnalysisRequest(
                document = proposal.document,
                name = proposal.name,
                proposalId = proposal.id.toString()
            )
        )

        return response.toModel()
    }
    // comitar tx

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