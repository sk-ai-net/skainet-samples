package com.kkon.kmp.ai.sinus.approximator

import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.dsl.network
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Module


interface SinusCalculator {
    fun calculate(x: Double): Double
    suspend fun loadModel()
}


class SineNN(override val name: String = "sin") : Module() {

    private val sineModule = network {
        input(1)
        dense(16) {
            activation = { it }
        }
        dense(16) {
            activation = { it }
        }
        dense(1)
    }
    override val modules: List<Module>
        get() = sineModule.modules

    override fun forward(input: Tensor): Tensor =
        sineModule.forward(input)
}

fun SineNN.of(angle: Double): Tensor =
    this.forward(DoublesTensor(Shape(1), listOf(angle.toDouble()).toDoubleArray()))