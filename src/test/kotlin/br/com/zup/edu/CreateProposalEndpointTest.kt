package br.com.zup.edu

import com.google.rpc.BadRequest
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    @Inject val repository: ProposalRepository,
    @Inject val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub) {

    /**
     * Cenarios de testes:
     *
     * - happy path: criar uma nova proposta - OK
     * - proposta existente - OK
     * - dados de entrada invalidos - OK
     */

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar uma nova proposta`() {

        // ação
        val response = grpcClient.create(CreateProposalRequest.newBuilder()
                                                        .setName("Rafael Ponte")
                                                        .setDocument("63657520325")
                                                        .setEmail("rafael.ponte@zup.com.br")
                                                        .setAddress("Rua das Rosas, 375")
                                                        .setSalary(2400.90)
                                                        .build())

        // validação
        with(response) {
            assertNotEquals("", id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id))) // efeito colateral
        }
    }

    @Test
    fun `nao deve criar proposta quando ja existir uma proposta com mesmo documento`() {
        // cenario
        repository.save(Proposal(
            name = "Rafael Ponte",
            document = "63657520325",
            email = "rafael.ponte@zup.com.br",
            address = "Rua das Rosas, 375",
            salary = BigDecimal("2400.90")
        ))

        // açao
        val error = assertThrows<StatusRuntimeException>() {
            grpcClient.create(CreateProposalRequest.newBuilder()
                            .setName("Yuri Matheus")
                            .setDocument("63657520325")
                            .setEmail("yuri.matheus@zup.com.br")
                            .setAddress("Rua das Tabajaras, 100")
                            .setSalary(32400.90)
                            .build())
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("proposal already exists", status.description)
        }
    }

    @Test
    fun `nao deve criar proposta quando dados de entrada forem invalidos`() {

        // açao
        val statusRuntimeException = assertThrows<StatusRuntimeException>() {
            grpcClient.create(CreateProposalRequest.newBuilder().setSalary(-0.1).build())
        }

        // validaçao
        with(statusRuntimeException) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("invalid parameters", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("document", "must not be blank"),
                Pair("document", "document is not a valid CPF or CNPJ"),
                Pair("address", "must not be blank"),
                Pair("name", "must not be blank"),
                Pair("email", "must not be blank"),
                Pair("salary", "must be greater than or equal to 0")
            ))
        }
    }

    @Factory
    class ClientsFactory {
        @Singleton
        fun stub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub? {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    /**
     * Extension Function
     */
    fun StatusRuntimeException.violations() : List<Pair<String, String>> {

        val details = StatusProto.fromThrowable(this)?.detailsList
            ?.get(0)!!
            .unpack(BadRequest::class.java)

        val violations = details.fieldViolationsList.map {
            Pair(it.field, it.description)
        }

        return violations
    }
}