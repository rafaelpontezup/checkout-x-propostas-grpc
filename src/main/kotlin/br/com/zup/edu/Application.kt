package br.com.zup.edu

import io.micronaut.runtime.Micronaut.build

fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("br.com.zup.edu")
		.start()
}

