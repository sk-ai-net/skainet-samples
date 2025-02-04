package com.kkon.kmp.ai.sinus.approximator

import okio.BufferedSource
import sk.ai.net.Shape
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.io.csv.CsvParametersLoader
import sk.ai.net.io.mapper.NamesBasedValuesModelMapper
import sk.ai.net.nn.reflection.summary


class ASinusCalculator(private val handleSource: () -> BufferedSource) : SinusCalculator {
    val model = SineNN()



    override fun calculate(x: Double): Double =
        (model.of(x) as DoublesTensor)[0]

    override suspend fun loadModel() {
        print(model.summary(Shape(1)))
        val parametersLoader = CsvParametersLoader(handleSource)

        val mapper = NamesBasedValuesModelMapper()

        parametersLoader.load { name, shape ->
            mapper.mapToModel(model, mapOf(name to shape))
        }
    }
}


