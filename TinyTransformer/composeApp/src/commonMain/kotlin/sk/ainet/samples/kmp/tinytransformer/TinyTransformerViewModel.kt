package sk.ainet.samples.kmp.tinytransformer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import sk.ainet.app.samples.tinytransformer.AttentionSnapshot
import sk.ainet.app.samples.tinytransformer.DEFAULT_CONTEXT_LEN
import sk.ainet.app.samples.tinytransformer.DEFAULT_CORPUS
import sk.ainet.app.samples.tinytransformer.DEFAULT_EPOCHS
import sk.ainet.app.samples.tinytransformer.DEFAULT_LEARNING_RATE
import sk.ainet.app.samples.tinytransformer.DEFAULT_MAX_VOCAB
import sk.ainet.app.samples.tinytransformer.Predictor
import sk.ainet.app.samples.tinytransformer.TinyTransformerTrainer
import sk.ainet.app.samples.tinytransformer.WordTokenizer
import sk.ainet.samples.kmp.tinytransformer.i18n.Language

data class TinyTransformerUiState(
    val language: Language = Language.EN,
    // (1) data
    val corpusText: String = DEFAULT_CORPUS.joinToString("\n"),
    val maxVocab: Int = DEFAULT_MAX_VOCAB,
    val contextLen: Int = DEFAULT_CONTEXT_LEN,
    val vocabSize: Int? = null,
    val windowCount: Int? = null,
    val dataReady: Boolean = false,
    // (2) training
    val epochs: Int = DEFAULT_EPOCHS,
    val learningRate: Float = DEFAULT_LEARNING_RATE,
    val epoch: Int = 0,
    val lossHistory: List<Float> = emptyList(),
    val attention: AttentionSnapshot? = null,
    val isTraining: Boolean = false,
    val isTrained: Boolean = false,
    // (3) embeddings
    val embeddings: List<Pair<String, FloatArray>> = emptyList(),
    // (4) prediction
    val prompt: String = "Der Fisch",
    val prediction: Predictor.Prediction? = null,
    val predictionError: String? = null,
)

class TinyTransformerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TinyTransformerUiState())
    val uiState: StateFlow<TinyTransformerUiState> = _uiState.asStateFlow()

    private var trainer: TinyTransformerTrainer? = null
    private var trainingJob: Job? = null

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(
            language = if (_uiState.value.language == Language.EN) Language.DE else Language.EN
        )
    }

    fun onCorpusChanged(text: String) {
        _uiState.value = _uiState.value.copy(corpusText = text)
        invalidateModel()
    }

    fun onResetCorpus() {
        _uiState.value = _uiState.value.copy(corpusText = DEFAULT_CORPUS.joinToString("\n"))
        invalidateModel()
    }

    fun onMaxVocabChanged(value: Int) {
        _uiState.value = _uiState.value.copy(maxVocab = value)
        invalidateModel()
    }

    fun onContextLenChanged(value: Int) {
        _uiState.value = _uiState.value.copy(contextLen = value)
        invalidateModel()
    }

    fun onEpochsChanged(value: Int) {
        _uiState.value = _uiState.value.copy(epochs = value)
    }

    fun onLearningRateChanged(value: Float) {
        _uiState.value = _uiState.value.copy(learningRate = value)
    }

    fun onPromptChanged(value: String) {
        _uiState.value = _uiState.value.copy(prompt = value)
    }

    /** (1) Tokenize: build vocabulary + windows and a fresh (untrained) model. */
    fun tokenize() {
        stopTraining()
        val state = _uiState.value
        val lines = state.corpusText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val vocab = WordTokenizer.buildVocab(lines, state.maxVocab)
        val windows = WordTokenizer.windows(lines, vocab, state.contextLen)
        val newTrainer = TinyTransformerTrainer(vocab, windows, state.contextLen)
        trainer = newTrainer
        _uiState.value = state.copy(
            vocabSize = vocab.sizeWithoutSpecials,
            windowCount = windows.size,
            dataReady = windows.isNotEmpty(),
            epoch = 0,
            lossHistory = emptyList(),
            attention = null,
            isTrained = false,
            embeddings = newTrainer.embeddingTable(),
            prediction = null,
            predictionError = null,
        )
    }

    /** (2) Train: collect the trainer's progress flow until done or stopped. */
    fun startTraining() {
        val currentTrainer = trainer ?: return
        if (_uiState.value.isTraining) return

        val epochs = _uiState.value.epochs
        val learningRate = _uiState.value.learningRate

        _uiState.value = _uiState.value.copy(
            isTraining = true,
            epoch = 0,
            lossHistory = emptyList(),
        )

        trainingJob = viewModelScope.launch(Dispatchers.Default) {
            currentTrainer.train(epochs, learningRate)
                .conflate()
                .collect { progress ->
                    val state = _uiState.value
                    val refreshEmbeddings = progress.epoch % 10 == 0 || progress.isCompleted
                    _uiState.value = state.copy(
                        epoch = progress.epoch,
                        lossHistory = state.lossHistory + progress.loss,
                        attention = progress.attention,
                        isTraining = !progress.isCompleted,
                        isTrained = state.isTrained || progress.isCompleted,
                        embeddings = if (refreshEmbeddings) currentTrainer.embeddingTable() else state.embeddings,
                    )
                }
        }
    }

    fun stopTraining() {
        trainingJob?.cancel()
        trainingJob = null
        val state = _uiState.value
        if (state.isTraining) {
            // A partially trained model is still usable for prediction.
            _uiState.value = state.copy(
                isTraining = false,
                isTrained = state.epoch > 0,
                embeddings = trainer?.embeddingTable() ?: state.embeddings,
            )
        }
    }

    /** (4) Predict the next word for the prompt. */
    fun predict() {
        val currentTrainer = trainer
        val state = _uiState.value
        if (currentTrainer == null || !state.dataReady) {
            _uiState.value = state.copy(prediction = null, predictionError = "tokenize")
            return
        }
        if (!state.isTrained) {
            _uiState.value = state.copy(prediction = null, predictionError = "train")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val prediction = currentTrainer.predictor().predictNext(state.prompt)
            _uiState.value = if (prediction == null) {
                _uiState.value.copy(prediction = null, predictionError = "empty")
            } else {
                _uiState.value.copy(
                    prediction = prediction,
                    predictionError = null,
                    attention = prediction.attention,
                )
            }
        }
    }

    private fun invalidateModel() {
        stopTraining()
        trainer = null
        _uiState.value = _uiState.value.copy(
            vocabSize = null,
            windowCount = null,
            dataReady = false,
            epoch = 0,
            lossHistory = emptyList(),
            attention = null,
            isTrained = false,
            embeddings = emptyList(),
            prediction = null,
            predictionError = null,
        )
    }
}
