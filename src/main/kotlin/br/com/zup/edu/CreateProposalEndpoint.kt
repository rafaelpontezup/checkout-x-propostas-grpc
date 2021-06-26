package br.com.zup.edu

import br.com.zup.edu.integration.FinancialClient
import br.com.zup.edu.integration.SubmitForAnalysisRequest
import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpStatus
import io.micronaut.transaction.SynchronousTransactionManager
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class CreateProposalEndpoint(
    @Inject private val repository: ProposalRepository,
    @Inject val transactionManager: SynchronousTransactionManager<Any>, // PlatformTransactionManager
    @Inject val financialClient: FinancialClient
) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("new request: $request")

        /**
         * Opções para lidar com controle transacional com gRPC:
         *  1. auto-commit do repository
         *  2. extrair logica para um Service com @Transactional
         *  3. controle transacional programatico
         */
        val proposal = transactionManager.executeWrite { // inicia transacao

            if (repository.existsByDocument(request.document)) {
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            val proposal = repository.save(request.toModel())

            // submete proposta para analise
            val status = submitForAnalysis(proposal)
            proposal.updateStatus(status) // atualiza e ja retorna a propria instancia
        } // commit

        // response
        val response = CreateProposalResponse.newBuilder()
                                    .setId(proposal.id.toString())
                                    .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                    .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun submitForAnalysis(proposal: Proposal): ProposalStatus {

        /**
         * Parece que a partir da versao 2.5.x o Micronaut não lança mais exception quando o retorno é
         * diferente de 2xx e 404, e o tipo de retorno do método é HttpResponse!
         */
        val response = financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = proposal.document,
            name = proposal.name,
            proposalId = proposal.id.toString()
        ))

        return when(response.status) {
            HttpStatus.CREATED -> response.body().toModel()
            HttpStatus.UNPROCESSABLE_ENTITY -> ProposalStatus.NOT_ELIGIBLE
            else -> {
                LOGGER.error("it's impossible to submit proposal for analysis. Server responded with '${response.status.code} - ${response.status}'")
                throw IllegalStateException("it's impossible to submit proposal for analysis")
            }
        }
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