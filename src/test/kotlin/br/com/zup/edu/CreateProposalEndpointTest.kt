package br.com.zup.edu

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import javax.inject.Singleton

@MicronautTest
internal class CreateProposalEndpointTest(
    val repository: ProposalRepository,
    val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub,
) {

    @Test
    fun `deve criar uma nova proposta`() {
        // cenário
        repository.deleteAll()

        // ação
        val response = grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setDocument("63657520325")
                                                .setEmail("rafael.ponte@zup.com.br")
                                                .setName("Rafael Ponte")
                                                .setAddress("Rua das Rosas, 375")
                                                .setSalary(30000.99)
                                                .build())

        // validação
        with(response) {
            assertNotNull(proposalId)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(proposalId))) // verifica efeito colateral
        }
    }

    @Factory
    class Clients {

        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}