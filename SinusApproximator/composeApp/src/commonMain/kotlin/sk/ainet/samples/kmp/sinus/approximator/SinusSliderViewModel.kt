package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.ainet.app.samples.sinus.MLPSinusCalculator
import sk.ainet.app.samples.sinus.PretrainedSinusCalculator
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

sealed interface ModelLoadingState {
    data object Loading : ModelLoadingState
    data object Success : ModelLoadingState
    data class Error(val message: String) : ModelLoadingState
    data object Initial : ModelLoadingState
}

class SinusSliderViewModel() : ViewModel() {
    private val calculator = MLPSinusCalculator()
    private val pretrainedCalculator = PretrainedSinusCalculator()

    private val _modelLoadingState = MutableStateFlow<ModelLoadingState>(ModelLoadingState.Initial)
    val modelLoadingState: StateFlow<ModelLoadingState> = _modelLoadingState.asStateFlow()

    // Expose the neural network model (MLP calculator)
    val neuralNetworkModel get() = calculator.model

    var sliderValue by mutableStateOf(0f)
        private set

    var sinusValue by mutableStateOf(0.0)
        private set

    // For backward compatibility (kept but not used by UI anymore). Defaults to KAN value.
    var modelSinusValue by mutableStateOf(0.0f)
        private set

    // Both models at once
    var modelSinusValueKan by mutableStateOf(0.0f)
        private set

    var modelSinusValueMlp by mutableStateOf(0.0f)
        private set

    var modelSinusValuePretrained by mutableStateOf(0.0f)
        private set

    var errorValue by mutableStateOf(0.0)
        private set

    var errorValueKan by mutableStateOf(0.0)
        private set

    var errorValueMlp by mutableStateOf(0.0)
        private set

    var errorValuePretrained by mutableStateOf(0.0)
        private set

    // Formatted values for display
    var formattedAngle by mutableStateOf("0.0000")
        private set

    var formattedSinusValue by mutableStateOf("0.00000")
        private set

    var formattedModelSinusValue by mutableStateOf("0.00000")
        private set

    var formattedErrorValue by mutableStateOf("0.00000")
        private set

    // New formatted values for dual display
    var formattedModelSinusValueKan by mutableStateOf("0.00000")
        private set

    var formattedModelSinusValueMlp by mutableStateOf("0.00000")
        private set

    var formattedModelSinusValuePretrained by mutableStateOf("0.00000")
        private set

    var formattedErrorValueKan by mutableStateOf("0.00000")
        private set

    var formattedErrorValueMlp by mutableStateOf("0.00000")
        private set

    var formattedErrorValuePretrained by mutableStateOf("0.00000")
        private set

    fun formatValue(value: Double, decimals: Int = 5): String {
        return value.formatDecimal(decimals)
    }

    fun formatValue(value: Float, decimals: Int = 5): String {
        return value.formatDecimal(decimals)
    }

    private fun Double.formatDecimal(decimals: Int): String {
        val factor = 10.0.pow(decimals.toDouble())
        return (round(this * factor) / factor).toString()
    }

    private fun Float.formatDecimal(decimals: Int): String {
        return this.toDouble().formatDecimal(decimals)
    }

    private fun updateFormattedValues() {
        formattedAngle = sliderValue.formatDecimal(4)
        formattedSinusValue = sinusValue.formatDecimal(5)
        formattedModelSinusValue = modelSinusValue.formatDecimal(5)
        formattedErrorValue = errorValue.formatDecimal(5)

        // New ones
        formattedModelSinusValueKan = modelSinusValueKan.formatDecimal(5)
        formattedModelSinusValueMlp = modelSinusValueMlp.formatDecimal(5)
        formattedModelSinusValuePretrained = modelSinusValuePretrained.formatDecimal(5)
        formattedErrorValueKan = errorValueKan.formatDecimal(5)
        formattedErrorValueMlp = errorValueMlp.formatDecimal(5)
        formattedErrorValuePretrained = errorValuePretrained.formatDecimal(5)
    }

    fun updateSliderValue(value: Float) {
        sliderValue = value
        sinusValue = sin(value.toDouble())
        // Compute models
        modelSinusValueMlp = calculator.calculate(value)
        modelSinusValuePretrained = pretrainedCalculator.calculate(value)

        // Keep legacy fields aligned to MLP for compatibility
        modelSinusValue = modelSinusValueMlp
        modelSinusValueKan = modelSinusValueMlp

        // Errors
        errorValueMlp = abs(sinusValue - modelSinusValueMlp)
        errorValuePretrained = abs(sinusValue - modelSinusValuePretrained)
        errorValueKan = errorValueMlp
        // Legacy single error equals MLP error
        errorValue = errorValueMlp
        updateFormattedValues()
    }

    fun loadModel() {
        viewModelScope.launch {
            _modelLoadingState.value = ModelLoadingState.Loading
            try {
                // Load models (currently no-ops as they are preloaded with weights)
                calculator.loadModel()
                pretrainedCalculator.loadModel()
                _modelLoadingState.value = ModelLoadingState.Success
                // Recalculate values after model is loaded
                updateSliderValue(sliderValue)
            } catch (e: Exception) {
                _modelLoadingState.value = ModelLoadingState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
