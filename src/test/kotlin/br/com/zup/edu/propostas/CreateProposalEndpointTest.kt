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
import javax.inject.Inject

/**
 * Teste de Integração
 */
@MicronautTest(transactional = false) // transactional
internal class CreateProposalEndpointTest(
    @Inject val repository: ProposalRespository,
    @Inject val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub
) {

    /**
     * 1. happy-path - OK
     * 2. proposta duplicada - OK
     * 3. erros de validação
     */

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar nova proposta`() {
        // ação
        val request = CreateProposalRequest.newBuilder()
                                        .setDocument("63657520325")
                                        .setName("Rafael Ponte")
                                        .setEmail("rafael.ponte@zup.com.br")
                                        .setAddress("Rua das Rosas, 375")
                                        .setSalary(30000.99)
                                        .build()

        val response = grpcClient.create(request)

        // validação
        with(response) {
            assertNotNull(id, "campo id")
            assertNotNull(createdAt, "campo data de criacao")
            assertTrue(repository.existsById(UUID.fromString(id)), "proposta no banco de dados") // validando o efeito colateral
        }
    }

    @Test
    fun `nao deve criar nova proposta quando existir outra proposta com mesmo documento`() {
        // cenário
        val existente = repository.save(Proposal(
            document = "63657520325",
            name = "Yuri Matheus",
            email = "yuri.matheus@zup.com.br",
            address = "Rua da JVM, 14",
            salary = BigDecimal("5000")
        ))

        // ação
        val exception = assertThrows<StatusRuntimeException> {
           grpcClient.create(CreateProposalRequest.newBuilder()
                                    .setDocument(existente.document)
                                    .setName("Rafael Ponte")
                                    .setEmail("rafael.ponte@zup.com.br")
                                    .setAddress("Rua das Rosas, 375")
                                    .setSalary(30000.99)
                                    .build())
        }

        // validação
        with(exception) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code, "status")
            assertEquals("proposal already exists", this.status.description, "description")
        }
    } // commit!!

    @Test
    fun `nao deve criar nova proposta quando dados forem invalidos`() {
        // ação
        val exception = assertThrows<StatusRuntimeException> {
                grpcClient.create(CreateProposalRequest.newBuilder()
                                        .setSalary(-0.1)
                                        .build())
        }

        // validação
        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("request with invalid parameters", this.status.description)
            assertThat(this.violations(), containsInAnyOrder(
                Pair("name" , "must not be blank"),
                Pair("document" , "must not be blank"),
                Pair("document" , "document is not a valid CPF or CNPJ"),
                Pair("email" , "must not be blank"),
                Pair("address" , "must not be blank"),
                Pair("salary" , "must be greater than or equal to 0")
            ))
        }
    }

    @Factory
    class ClientsFactory {
        @Bean // toda vida que tem @Inject um novo objeto eh criado
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    // extension function
    fun StatusRuntimeException.violations(): List<Pair<String, String>> {

        val badRequest = StatusProto.fromThrowable(this)
            ?.detailsList!!.get(0) // Any
            .unpack(BadRequest::class.java)

        val violations: List<Pair<String, String>> = badRequest.fieldViolationsList.map {
            it.field to it.description // Pair(it.field, it.description)
        }

        return violations
    }
}