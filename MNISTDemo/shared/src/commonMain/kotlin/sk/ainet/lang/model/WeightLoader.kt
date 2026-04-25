package sk.ainet.lang.model

import kotlinx.io.Buffer
import sk.ainet.io.gguf.GGUFReader
import sk.ainet.lang.nn.Module
import sk.ainet.lang.tensor.data.FloatArrayTensorData
import sk.ainet.lang.types.FP32

/**
 * Common-code GGUF weight loader. Works on every Kotlin target (JVM, Android, JS, wasmJs)
 * because it relies only on `FloatArrayTensorData.buffer` — no JVM reflection.
 *
 * Tensor names in [bytes] are matched to [Module.trainableParameters] by exact name.
 */
fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray) {
    val reader = GGUFReader(Buffer().apply { write(bytes) })
    val tensorMap = reader.tensors.associateBy { it.name }

    module.trainableParameters().forEach { param ->
        val readerTensor = tensorMap[param.name] ?: return@forEach
        val data = param.value.data
        if (data is FloatArrayTensorData<*>) {
            val buf = data.buffer
            readerTensor.data.forEachIndexed { idx, value ->
                buf[idx] = (value as Number).toFloat()
            }
        }
    }
}
