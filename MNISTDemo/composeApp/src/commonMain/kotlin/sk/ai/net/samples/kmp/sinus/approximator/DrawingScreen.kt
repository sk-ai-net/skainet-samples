package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kkon.kmp.ai.sinus.approximator.ADigitClassifier
import com.kkon.kmp.ai.sinus.approximator.DigitClassifier
import kotlinx.coroutines.launch
import kotlinx.io.Source
import mnistdemo.composeapp.generated.resources.Res
import mnistdemo.composeapp.generated.resources.img_1
import mnistdemo.composeapp.generated.resources.img_10
import mnistdemo.composeapp.generated.resources.img_11
import mnistdemo.composeapp.generated.resources.img_12
import mnistdemo.composeapp.generated.resources.img_13
import mnistdemo.composeapp.generated.resources.img_14
import mnistdemo.composeapp.generated.resources.img_15
import mnistdemo.composeapp.generated.resources.img_16
import mnistdemo.composeapp.generated.resources.img_17
import mnistdemo.composeapp.generated.resources.img_18
import mnistdemo.composeapp.generated.resources.img_19
import mnistdemo.composeapp.generated.resources.img_2
import mnistdemo.composeapp.generated.resources.img_20
import mnistdemo.composeapp.generated.resources.img_21
import mnistdemo.composeapp.generated.resources.img_22
import mnistdemo.composeapp.generated.resources.img_23
import mnistdemo.composeapp.generated.resources.img_24
import mnistdemo.composeapp.generated.resources.img_25
import mnistdemo.composeapp.generated.resources.img_26
import mnistdemo.composeapp.generated.resources.img_27
import mnistdemo.composeapp.generated.resources.img_28
import mnistdemo.composeapp.generated.resources.img_29
import mnistdemo.composeapp.generated.resources.img_3
import mnistdemo.composeapp.generated.resources.img_30
import mnistdemo.composeapp.generated.resources.img_31
import mnistdemo.composeapp.generated.resources.img_32
import mnistdemo.composeapp.generated.resources.img_33
import mnistdemo.composeapp.generated.resources.img_34
import mnistdemo.composeapp.generated.resources.img_35
import mnistdemo.composeapp.generated.resources.img_36
import mnistdemo.composeapp.generated.resources.img_37
import mnistdemo.composeapp.generated.resources.img_38
import mnistdemo.composeapp.generated.resources.img_39
import mnistdemo.composeapp.generated.resources.img_4
import mnistdemo.composeapp.generated.resources.img_40
import mnistdemo.composeapp.generated.resources.img_41
import mnistdemo.composeapp.generated.resources.img_42
import mnistdemo.composeapp.generated.resources.img_43
import mnistdemo.composeapp.generated.resources.img_44
import mnistdemo.composeapp.generated.resources.img_45
import mnistdemo.composeapp.generated.resources.img_46
import mnistdemo.composeapp.generated.resources.img_47
import mnistdemo.composeapp.generated.resources.img_48
import mnistdemo.composeapp.generated.resources.img_49
import mnistdemo.composeapp.generated.resources.img_5
import mnistdemo.composeapp.generated.resources.img_50
import mnistdemo.composeapp.generated.resources.img_51
import mnistdemo.composeapp.generated.resources.img_52
import mnistdemo.composeapp.generated.resources.img_53
import mnistdemo.composeapp.generated.resources.img_54
import mnistdemo.composeapp.generated.resources.img_55
import mnistdemo.composeapp.generated.resources.img_56
import mnistdemo.composeapp.generated.resources.img_57
import mnistdemo.composeapp.generated.resources.img_58
import mnistdemo.composeapp.generated.resources.img_59
import mnistdemo.composeapp.generated.resources.img_6
import mnistdemo.composeapp.generated.resources.img_60
import mnistdemo.composeapp.generated.resources.img_61
import mnistdemo.composeapp.generated.resources.img_62
import mnistdemo.composeapp.generated.resources.img_63
import mnistdemo.composeapp.generated.resources.img_64
import mnistdemo.composeapp.generated.resources.img_65
import mnistdemo.composeapp.generated.resources.img_66
import mnistdemo.composeapp.generated.resources.img_67
import mnistdemo.composeapp.generated.resources.img_68
import mnistdemo.composeapp.generated.resources.img_69
import mnistdemo.composeapp.generated.resources.img_7
import mnistdemo.composeapp.generated.resources.img_70
import mnistdemo.composeapp.generated.resources.img_71
import mnistdemo.composeapp.generated.resources.img_72
import mnistdemo.composeapp.generated.resources.img_73
import mnistdemo.composeapp.generated.resources.img_74
import mnistdemo.composeapp.generated.resources.img_75
import mnistdemo.composeapp.generated.resources.img_76
import mnistdemo.composeapp.generated.resources.img_77
import mnistdemo.composeapp.generated.resources.img_78
import mnistdemo.composeapp.generated.resources.img_79
import mnistdemo.composeapp.generated.resources.img_8
import mnistdemo.composeapp.generated.resources.img_80
import mnistdemo.composeapp.generated.resources.img_81
import mnistdemo.composeapp.generated.resources.img_82
import mnistdemo.composeapp.generated.resources.img_83
import mnistdemo.composeapp.generated.resources.img_84
import mnistdemo.composeapp.generated.resources.img_85
import mnistdemo.composeapp.generated.resources.img_86
import mnistdemo.composeapp.generated.resources.img_87
import mnistdemo.composeapp.generated.resources.img_88
import mnistdemo.composeapp.generated.resources.img_89
import mnistdemo.composeapp.generated.resources.img_9
import mnistdemo.composeapp.generated.resources.img_90
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// List of image resources
val IMAGE_RESOURCES = listOf(
    Res.drawable.img_1,
    Res.drawable.img_2,
    Res.drawable.img_3,
    Res.drawable.img_4,
    Res.drawable.img_5,
    Res.drawable.img_6,
    Res.drawable.img_7,
    Res.drawable.img_8,
    Res.drawable.img_9,
    Res.drawable.img_10,
    Res.drawable.img_11,
    Res.drawable.img_12,
    Res.drawable.img_13,
    Res.drawable.img_14,
    Res.drawable.img_15,
    Res.drawable.img_16,
    Res.drawable.img_17,
    Res.drawable.img_18,
    Res.drawable.img_19,
    Res.drawable.img_20,
    Res.drawable.img_21,
    Res.drawable.img_22,
    Res.drawable.img_23,
    Res.drawable.img_24,
    Res.drawable.img_25,
    Res.drawable.img_26,
    Res.drawable.img_27,
    Res.drawable.img_28,
    Res.drawable.img_29,
    Res.drawable.img_30,
    Res.drawable.img_31,
    Res.drawable.img_32,
    Res.drawable.img_33,
    Res.drawable.img_34,
    Res.drawable.img_35,
    Res.drawable.img_36,
    Res.drawable.img_37,
    Res.drawable.img_38,
    Res.drawable.img_39,
    Res.drawable.img_40,
    Res.drawable.img_41,
    Res.drawable.img_42,
    Res.drawable.img_43,
    Res.drawable.img_44,
    Res.drawable.img_45,
    Res.drawable.img_46,
    Res.drawable.img_47,
    Res.drawable.img_48,
    Res.drawable.img_49,
    Res.drawable.img_50,
    Res.drawable.img_51,
    Res.drawable.img_52,
    Res.drawable.img_53,
    Res.drawable.img_54,
    Res.drawable.img_55,
    Res.drawable.img_56,
    Res.drawable.img_57,
    Res.drawable.img_58,
    Res.drawable.img_59,
    Res.drawable.img_60,
    Res.drawable.img_61,
    Res.drawable.img_62,
    Res.drawable.img_63,
    Res.drawable.img_64,
    Res.drawable.img_65,
    Res.drawable.img_66,
    Res.drawable.img_67,
    Res.drawable.img_68,
    Res.drawable.img_69,
    Res.drawable.img_70,
    Res.drawable.img_71,
    Res.drawable.img_72,
    Res.drawable.img_73,
    Res.drawable.img_74,
    Res.drawable.img_75,
    Res.drawable.img_76,
    Res.drawable.img_77,
    Res.drawable.img_78,
    Res.drawable.img_79,
    Res.drawable.img_80,
    Res.drawable.img_81,
    Res.drawable.img_82,
    Res.drawable.img_83,
    Res.drawable.img_84,
    Res.drawable.img_85,
    Res.drawable.img_86,
    Res.drawable.img_87,
    Res.drawable.img_88,
    Res.drawable.img_89,
    Res.drawable.img_90
)

@Composable
fun DrawingScreen(handleSource: () -> Source) {

    val digitClassifier by remember { mutableStateOf(ADigitClassifier(handleSource)) }

    /* Screen mode controls*/
    var isModelLoaded by remember { mutableStateOf(false) }
    var isChooseImageMode by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(-1) }
    var classificationResult by remember { mutableStateOf<String?>(null) }

    /* Drawing controls */
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPathRef by remember { mutableStateOf(1) }
    var lastOffset by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Instruction Text
        Text(
            text = "Draw a digit (0-9) in the canvas below.\nPress 'Classify' to recognize the digit or 'Clear' to start over.",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (isChooseImageMode) {
            ChooseImagePanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                imageResources = IMAGE_RESOURCES,
                selectedImageIndex = selectedImageIndex,
                onImageSelected = { selectedImageIndex = it }
            )

        } else {
            DrawingPanel(
                onDragStart = { offset ->
                    currentPath = Path()
                    currentPath?.moveTo(offset.x, offset.y)
                    currentPathRef += 1
                    lastOffset = offset
                },
                onDrag = { _, offset ->
                    if (lastOffset != null) {
                        val newOffset = Offset(
                            lastOffset!!.x + offset.x,
                            lastOffset!!.y + offset.y
                        )
                        currentPath?.lineTo(newOffset.x, newOffset.y)
                        currentPathRef += 1
                        lastOffset = newOffset
                    }
                },
                onDragEnd = {
                    currentPath.let { value ->
                        if (value != null) {
                            paths = paths + value
                            currentPath = null
                            currentPathRef = 0
                        }
                    }
                },
                onDraw = {
                    paths.forEach { drawPath(path = it, color = Color.Black, style = Stroke(10f)) }

                    if (currentPath != null && currentPathRef > 0) {
                        drawPath(path = currentPath!!, color = Color.Black, style = Stroke(10f))
                    }
                },
            )
        }

        val coroutineScope = rememberCoroutineScope()

        ButtonsPanel(
            isChooseImage = isChooseImageMode,
            isModelLoaded = isModelLoaded,
            classificationResult = classificationResult,
            onLoadModel = {
                isModelLoaded = true

                coroutineScope.launch {
                    digitClassifier.loadModel()
                }
            },
            onSwitchMode = {
                isChooseImageMode = !isChooseImageMode

                if (isChooseImageMode) {
                    selectedImageIndex = -1

                } else {
                    paths = emptyList()
                    currentPath = null
                }
            },
            onClearCanvas = {
                paths = emptyList()
                currentPath = null
                selectedImageIndex = -1
                classificationResult = null
            },
            onClassify = {
                val bitmap = drawPathsToImageBitmap(paths)
                val result = digitClassifier.classify(bitmap.toGrayScale28To28Image())
                classificationResult = "Predicted digit: $result"
            },
        )
    }
}

@Composable
fun ChooseImagePanel(
    modifier: Modifier,
    imageResources: List<DrawableResource>,
    selectedImageIndex: Int,
    onImageSelected: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5), // 5 columns in the grid
        modifier = modifier,
        content = {
            items(imageResources.size) { index ->
                Image(
                    painter = painterResource(imageResources[index]),
                    contentDescription = "Image",
                    modifier = Modifier
                        .padding(4.dp)
                        .size(48.dp)
                        .clickable {
                            onImageSelected.invoke(index)
                        }
                        .border(
                            width = if (selectedImageIndex == index) 2.dp else 0.dp,
                            color = if (selectedImageIndex == index) Color.Blue else Color.Transparent // Highlight selected image
                        )
                )
            }
        }
    )
}

@Composable
fun DrawingPanel(
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDraw: DrawScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp),

        ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd
                    )
                },
            onDraw = onDraw
        )
    }
}

@Composable
fun ButtonsPanel(
    isChooseImage: Boolean,
    isModelLoaded: Boolean,
    classificationResult: String?,
    onLoadModel: () -> Unit,
    onSwitchMode: () -> Unit,
    onClearCanvas: () -> Unit,
    onClassify: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                // Switch Button
                Button(
                    onClick = onSwitchMode,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isChooseImage) Color.Green else Color.Cyan
                    )
                ) {
                    if (isChooseImage) {
                        Text("Draw image")
                    } else {
                        Text("Choose image")
                    }
                }
            }

            if (!isModelLoaded) {
                Button(onClick = onLoadModel) {
                    Text("Load Model")
                }
            } else {
                Button(onClick = onClassify) {
                    Text("Classify")
                }
            }

            Button(onClick = onClearCanvas) {
                Text("Clear")
            }
        }

        classificationResult?.let { result ->
            Text(
                text = result,
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

fun drawPathsToImageBitmap(
    paths: List<Path>,
    bitmapSize: IntSize = IntSize(500, 500),
    pathColor: Color = Color.Black,
    strokeWidth: Float = 5f
): ImageBitmap {
    // Create an ImageBitmap with given dimensions
    val imageBitmap = ImageBitmap(bitmapSize.width, bitmapSize.height)

    // Create a Canvas to draw onto the ImageBitmap
    val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)

    // Define a Paint object
    val paint = Paint().apply {
        this.color = pathColor
        this.style = PaintingStyle.Stroke
        this.strokeWidth = strokeWidth
    }

    // Draw each path onto the canvas
    paths.forEach { path ->
        canvas.drawPath(path, paint)
    }

    return imageBitmap.also {
        debugInConsolePrinting(it)
    }
}

private fun ImageBitmap.toGrayScale28To28Image(): DigitClassifier.GrayScale28To28Image {
    val image = DigitClassifier.GrayScale28To28Image()

//    val img = ImageIO.read(Files.newInputStream(this.first()))
//    val resizedImg = BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY)
//    val graphics = resizedImg.createGraphics()
//    graphics.drawImage(img, 0, 0, 28, 28, null)
//    graphics.dispose()
//
//    for (y in 0 until 28) {
//        for (x in 0 until 28) {
//            val rgb = resizedImg.getRGB(x, y) and 0xFF
//            image.setPixel(x, y, rgb / 255.0f)
//        }
//    }

    return image
}

fun debugInConsolePrinting(bitmap: ImageBitmap) {
    val pixelMap = bitmap.toPixelMap()
    val width = bitmap.width
    val height = bitmap.height

    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y]
            print(if (color.toArgb() > 0) "@" else "_")
        }
        println()
    }
}
