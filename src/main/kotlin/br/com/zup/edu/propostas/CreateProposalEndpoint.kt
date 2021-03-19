package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Singleton

@Singleton
class CreateProposalEndpoint : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

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