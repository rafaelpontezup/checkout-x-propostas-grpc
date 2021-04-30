package br.com.zup.edu

import br.com.zup.edu.integration.FinancialClient
import br.com.zup.edu.integration.SubmitForAnalysisRequest
import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.transaction.SynchronousTransactionManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class CreateProposalEndpoint(
    @Inject val repository: ProposalRepository,
    @Inject val transactionManager: SynchronousTransactionManager<Connection>,
    @Inject val financialClient: FinancialClient,
    @Inject val proposalIdGenerator: ProposalIdGenerator
) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("request: $request")

        // extra: exemplificando TDD
        if (request.document.startsWith("3")) {
            throw IllegalStateException("esse documento não é suportado pelo nosso sistema")
        }

        // solucoes: 1) controle transacional PROGRAMATICO; 2) ou extrai logica para um service + @Transacional
        val proposal = transactionManager.executeWrite {

            if (repository.existsByDocument(request.document)) {
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            val proposal = repository.save(request.toModel(proposalIdGenerator))

            val status = submitForAnalysis(proposal)
            proposal.updateStatus(status)
        }

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                                .setProposalId(proposal.id.toString())
                                                .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                                .build())
        responseObserver.onCompleted()
    }

    private fun submitForAnalysis(proposal: Proposal): ProposalStatus {

        val response = financialClient.submitForAnalysis(SubmitForAnalysisRequest( // x987
            document = proposal.document,
            name = proposal.name,
            proposalId = proposal.id.toString()
        ))

        return response.toModel()
    }

    fun CreateProposalRequest.toModel(proposalIdGenerator: ProposalIdGenerator): Proposal {
        return Proposal(
            document = document,
            name = name,
            email = email,
            address = address,
            salary = BigDecimal(salary),
            id = proposalIdGenerator.generate()
        )
    }

    fun LocalDateTime.toGrpcTimestamp(): Timestamp {
        val instant = this.atZone(ZoneId.of("UTC")).toInstant()
        return Timestamp.newBuilder()
                    .setSeconds(instant.epochSecond)
                    .setNanos(instant.nano)
                    .build()
    }

}