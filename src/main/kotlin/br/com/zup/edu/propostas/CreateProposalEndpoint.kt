package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import br.com.zup.edu.propostas.integration.FinancialClient
import br.com.zup.edu.propostas.integration.SubmitForAnalysisRequest
import br.com.zup.edu.propostas.integration.SubmitForAnalysisResponse
import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.transaction.SynchronousTransactionManager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class CreateProposalEndpoint(
    @Inject val repository: ProposalRespository,
    @Inject val transactionManager: SynchronousTransactionManager<Connection>,
    @Inject val financialClient: FinancialClient,
    @Inject val proposalIdGenerator: ProposalIdGenerator
) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

        // favoreça controle transacional programático
        val proposal = transactionManager.executeWrite {

            if (repository.existsByDocument(request.document)) {
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            val proposal = repository.save(request.toModel(proposalIdGenerator))

            val status = submitForAnalysis(proposal)
            proposal.updateStatus(status)
        }

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(proposal.id.toString())
                                        .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                        .build())
        responseObserver.onCompleted()
    }

    private fun submitForAnalysis(proposal: Proposal): ProposalStatus {
        /**
         * TODO: nao me preocupei com try-cath
         */
        val response = financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = proposal.document,
            name = proposal.name,
            proposalId = proposal.id.toString()
        ))

        val status = response.toModel()
        return status
    }

}

/**
 * Extension methods
 */

fun CreateProposalRequest.toModel(generator: ProposalIdGenerator): Proposal {
    return Proposal(
        document = document,
        email = email,
        name = name,
        address = address,
        salary = BigDecimal(salary),
        id = generator.generate()
    )
}

fun LocalDateTime.toGrpcTimestamp(): Timestamp {
    val instant = this.atZone(ZoneId.of("UTC")).toInstant()
    return Timestamp.newBuilder()
                    .setSeconds(instant.epochSecond)
                    .setNanos(instant.nano)
                    .build()
}