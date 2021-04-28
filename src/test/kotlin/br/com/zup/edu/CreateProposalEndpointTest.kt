package br.com.zup.edu

import com.google.rpc.BadRequest
import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*
import javax.inject.Singleton
import javax.transaction.Transactional

@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    val repository: ProposalRepository,
    val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub,
) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    /**
     * extra: exemplificando TDD
     */
    @Test
    fun `nao deve criar nova proposta quando documento comecar com 3`() {
        // cenário

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                                    .setDocument("38341104008")
                                                    .setEmail("rafael.ponte@zup.com.br")
                                                    .setName("Rafael Ponte")
                                                    .setAddress("Rua das Rosas, 375")
                                                    .setSalary(30000.99)
                                                    .build())
        }

        // validação
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("esse documento não é suportado pelo nosso sistema", status.description)
        }
    }

    @Test
    fun `deve criar uma nova proposta`() {

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

    @Test
    fun `nao deve criar uma proposta quando ja existe proposta com mesmo documento`() {
        // cenário
        val existente = repository.save(Proposal(
            document = "62625314087",
            name = "Yuri Matheus",
            email = "yuri.matheus@zup.com.br",
            address = "Rua dos Endpoints",
            salary = BigDecimal(1001.12)
        ))

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                            .setDocument(existente.document)
                                            .setEmail("rafael.ponte@zup.com.br")
                                            .setName("Rafael Ponte")
                                            .setAddress("Rua das Rosas, 375")
                                            .setSalary(30000.99)
                                            .build())
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("proposal already exists", status.description)
        }
    }

    @Test
    fun `nao deve criar nova proposta quando parametros forem invalidos`() {

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setSalary(-0.1)
                                                .build())
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("invalid parameters", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("name", "must not be blank"),
                Pair("document", "must not be blank"),
                Pair("document", "document is not a valid CPF or CNPJ"),
                Pair("email", "must not be blank"),
                Pair("address", "must not be blank"),
                Pair("salary", "must be greater than or equal to 0"),
            ))
        }
    }

    @Factory
    class Clients {

        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    fun StatusRuntimeException.violations(): List<Pair<String, String>> {

        val details = StatusProto.fromThrowable(this)
            ?.detailsList?.get(0)!!
            .unpack(BadRequest::class.java)

        val violations: List<Pair<String, String>> = details.fieldViolationsList
            .map { Pair(it.field, it.description) }

        return violations
    }

}