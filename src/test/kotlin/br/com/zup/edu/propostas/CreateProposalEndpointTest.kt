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

/**
 * ROTEIRO do Checkout Estendido:
 *
 * 1. contextualização: apresentação da aplicação
 *  1.1. explica e navega pelo codigo
 *  1.2. testa manualmente com BloomRPC
 * 2. introdução a testes, tipos de testes
 * 3. unidade X integração
 * 4. criando classe de teste com @MicronautTest
 * 5. executando teste (vazio) e entendendo o contexto do micronaut (banco, server grpc etc)
 *  5.1. configurando datasource de testes: H2
 * 6. escreve teste happy-path "deve criar nova proposta"
 *  6.1. 3 blocos de um teste
 *  6.2. fabricando o gRPC client com @Factory, ip/porta do channel etc
 *  6.3. injetando gRPC client
 *  6.4. validando efeitos colateriais: assertTrue(repository.existsById(UUID.fromString(id)))
 *  6.5. entendendo o problema de usar @Transactional em endpoints gRPC
 *  6.6. avaliando alternativas: auto-commit, Service e SynchronousTransactionManager
 * 7. escreve teste alternativo "nao deve criar proposta quando documento existente"
 *  6.1. validando somente contrato da API (status code e description)
 *  6.2. por que quebrou? entendendo @MicronautTest(transactional = false)
 *  6.3. (opcional) habilitando logs de transacoes
 * 8. escreve teste alternativo "nao deve criar propostas quando parametros de entrada forem invalidos"
 *  8.1. validando somente contrato da API (status code e description)
 *  8.2. validando violações da Bean Validation com Hamcrest .hasItem()
 *  8.3. validando violações da Bean Validation com Hamcrest .containsInAnyOrder()
 *  8.3. melhorando com Extension Functions: StatusRuntimeException.violations()
 * 9. evitando repetição de código com @BeforeEach
 * 10. duvidas?
 */
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
    class ClientsFactory {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    /**
     * TODO: mover para arquivo separado para ser reutilizado entre vários testes
     */
    private fun StatusRuntimeException.violations(): List<Pair<String, String>>? {
        val details = StatusProto.fromThrowable(this)
            ?.detailsList?.get(0)!!
            .unpack(BadRequest::class.java)

        return details.fieldViolationsList
            .map { it.field to it.description }
    }
}