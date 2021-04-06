package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.PropostasGrpcServiceGrpc
import com.google.rpc.BadRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Bean
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

@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    val repository: ProposalRespository,
    val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub,
) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar nova proposta`() {
        // ação
        val response = grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setDocument("63657520325")
                                                .setName("Rafael Ponte")
                                                .setEmail("rafael.ponte@zup.com.br")
                                                .setAddress("Rua das Rosa, 375")
                                                .setSalary(2020.99)
                                                .build())

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
        }
    }

    @Test
    fun `nao deve criar nova proposta quando existir proposta para o mesmo documento`() {
        // cenário
        val existing  = repository.save(Proposal(
            document = "63657520325",
            email = "rafael.ponte@zup.com.br",
            name = "Rafael Ponte",
            address = "Rua das Rosas, 375",
            salary = BigDecimal("2020.99")
        ))

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                            .setDocument(existing.document)
                                            .setName("Outro nome")
                                            .setEmail("rafael.ponte@zup.com.br")
                                            .setAddress("Outro endereço qualquer")
                                            .setSalary(1000.0)
                                            .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("proposal already exists", status.description)
        }
    }

    @Test
    fun `nao deve criar proposta quando dados invalidos`() {
        // cenário e ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setSalary(-0.1)
                                                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("request with invalid parameters", status.description)
            assertThat(violationsFrom(this), containsInAnyOrder(
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
    class ClientsFactory {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    /**
     * This method may be extracted to a new class: StatusRuntimeExceptionUtils
     */
    fun violationsFrom(exception: StatusRuntimeException): List<Pair<String, String>>? {

        val details = StatusProto.fromThrowable(exception)
            ?.detailsList?.get(0)!!
            .unpack(BadRequest::class.java)

        return details.fieldViolationsList
            .map { it.field to it.description }
    }
}