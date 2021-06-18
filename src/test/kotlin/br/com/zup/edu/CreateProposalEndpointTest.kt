package br.com.zup.edu

import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    @Inject val repository: ProposalRepository,
    @Inject val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub
) {

    /**
     * 1. happy path - ok
     * 2. proposta duplicada - ok
     * 3. dados invalidos - ok
     */

    @BeforeEach
    fun init() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar nova proposta`() {

        // ação
        val request = CreateProposalRequest.newBuilder()
                                        .setName("Rafael Ponte")
                                        .setDocument("63657520325")
                                        .setEmail("rafael.ponte@zup.com.br")
                                        .setAddress("Rua das Rosas, 375")
                                        .setSalary(30000.0)
                                        .build()

        val response = grpcClient.create(request)

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
        }
    }

    @Test
    fun `nao deve criar proposta quando proposta com mesmo documento ja existir`() {
        // cenario
        val existente = repository.save(
            Proposal(
                name = "Yuri",
                document = "63657520325",
                email = "yuri.matheus@zup.com.br",
                address = "Rua das Tabajaras",
                salary = BigDecimal("20000.0")
            )
        )

        // ação
        val request = CreateProposalRequest.newBuilder()
                                    .setName("Rafael Ponte")
                                    .setDocument(existente.document) // MESMO CPF
                                    .setEmail("rafael.ponte@zup.com.br")
                                    .setAddress("Rua das Rosas, 375")
                                    .setSalary(30000.0)
                                    .build()

        val error = assertThrows<StatusRuntimeException>() {
            grpcClient.create(request) // abriu uma nova thread
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
            assertEquals("proposal already exists", this.status.description)
        }
    }

    @Test
    fun `nao deve criar proposta quando dados de entrada forem invalidos`() {

        // ação
        val request = CreateProposalRequest.newBuilder().build() // invalido

        val error = assertThrows<StatusRuntimeException>() {
            grpcClient.create(request)
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("invalid parameters", this.status.description)
        }
    }

    @Factory
    class ClientsFactory {
        @Bean
        fun stub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}