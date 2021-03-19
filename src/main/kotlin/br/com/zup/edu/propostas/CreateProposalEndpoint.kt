package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class CreateProposalEndpoint(@Inject val repository: ProposalRespository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

        val proposal = Proposal(
            document = request.document,
            email = request.email,
            name = request.name,
            address = request.address,
            salary = BigDecimal(request.salary)
        )

        repository.save(proposal)

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(UUID.randomUUID().toString())
                                        .setCreatedAt(LocalDateTime.now().let {
                                            val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                                            Timestamp.newBuilder()
                                                .setSeconds(createdAt.epochSecond)
                                                .setNanos(createdAt.nano)
                                                .build()
                                        }).build())
        responseObserver.onCompleted()
    }

}