package com.kkon.kmp.ai.sinus.approximator

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import sk.ai.net.Shape
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.io.csv.CsvParametersLoader
import sk.ai.net.io.mapper.NamesBasedValuesModelMapper
import sk.ai.net.nn.reflection.flattenParams
import sk.ai.net.nn.reflection.summary

fun fileSource(path: String): Source {
    val p = Path(path)
    return SystemFileSystem.source(p).buffered()
}


class ASinusCalculator(private val handleSource: () -> Source) : SinusCalculator {
    val model = SineNN()


    override fun calculate(x: Double): Double =
        (model.of(x) as DoublesTensor)[0]


    override suspend fun loadModel() {
        print(model.summary(Shape(1)))
        val parametersLoader = CsvParametersLoader(handleSource)

        val mapper = NamesBasedValuesModelMapper()
        print(model.summary(Shape(1)))

        parametersLoader.load { name, shape ->
            mapper.mapToModel(model, mapOf(name to shape))
        }
        val params = flattenParams(model)
        print(params)
    }
}


