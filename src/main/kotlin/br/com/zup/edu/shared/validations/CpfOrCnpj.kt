package br.com.zup.edu.shared.validations

import org.hibernate.validator.constraints.CompositionType
import org.hibernate.validator.constraints.ConstraintComposition
import org.hibernate.validator.constraints.br.CNPJ
import org.hibernate.validator.constraints.br.CPF
import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@CPF
@CNPJ
@ReportAsSingleViolation
@ConstraintComposition(CompositionType.OR)
@Constraint(validatedBy = []) // sem validador customizado
@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD, PROPERTY_GETTER)
annotation class CpfOrCnpj(
    val message: String = "document is not a valid CPF or CNPJ",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = [],
)
