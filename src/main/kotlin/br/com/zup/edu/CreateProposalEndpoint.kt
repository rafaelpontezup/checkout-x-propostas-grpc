package br.com.zup.edu

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
import javax.transaction.Transactional

@ErrorHandler
@Singleton
class CreateProposalEndpoint(
    @Inject val repository: ProposalRepository,
    @Inject val transactionManager: SynchronousTransactionManager<Connection>,
) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("request: $request")

        // solucoes: 1) controle transacional PROGRAMATICO; 2) ou extrai logica para um service + @Transacional
        val proposal = transactionManager.executeWrite {
            if (repository.existsByDocument(request.document)) {
                throw ProposalAlreadyExistsException("proposal already exists")
            }

            repository.save(request.toModel())
        }

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                                .setProposalId(proposal.id.toString())
                                                .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                                .build())
        responseObserver.onCompleted()
    }

    fun CreateProposalRequest.toModel(): Proposal {
        return Proposal(
            document = document,
            name = name,
            email = email,
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

}