package com.pixelstudio

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelStudioApp()
        }
    }
}

@Composable
fun PixelStudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                    brightness = 0f
                    contrast = 1f
                    saturation = 1f
                    rotation = 0
                }
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Не удалось открыть изображение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val bg = ComposeColor(0xFF101114)
    val panel = ComposeColor(0xFF1C1D22)
    val accent = ComposeColor(0xFFB68CFF)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = bg,
            surface = panel
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
        ) {

            // Верхняя панель
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "PixelStudio",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White
                    )

                    Text(
                        "Мобильный фоторедактор",
                        color = ComposeColor.LightGray,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        bitmap?.let {
                            saveEdited(
                                context,
                                it,
                                brightness,
                                contrast,
                                saturation,
                                rotation
                            )
                        }
                    },
                    enabled = bitmap != null
                ) {
                    Text("Сохранить")
                }
            }

            // Рабочее поле
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(
                        ComposeColor(0xFF08090B),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let { source ->

                    val cm = ColorMatrix().apply {
                        setToSaturation(saturation)

                        val c = contrast
                        val b = brightness * 255f

                        val contrastMatrix = ColorMatrix(
                            floatArrayOf(
                                c, 0f, 0f, 0f, b,
                                0f, c, 0f, 0f, b,
                                0f, 0f, c, 0f, b,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )

                        postConcat(contrastMatrix)
                    }

                    Image(
                        bitmap = source.asImageBitmap(),
                        contentDescription = "Фото для редактирования",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colorFilter = ColorMatrixColorFilter(cm)
                    )

                } ?: Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Твоё рабочее поле",
                        fontSize = 20.sp,
                        color = ComposeColor.White
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Открой фотографию, чтобы начать",
                        color = ComposeColor.Gray
                    )
                }
            }

            // Панель инструментов
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(panel)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                Button(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Открыть фотографию")
                }

                Spacer(Modifier.height(10.dp))

                SliderRow(
                    "Яркость",
                    brightness,
                    -1f,
                    1f
                ) {
                    brightness = it
                }

                SliderRow(
                    "Контраст",
                    contrast,
                    0f,
                    2f
                ) {
                    contrast = it
                }

                SliderRow(
                    "Насыщенность",
                    saturation,
                    0f,
                    2f
                ) {
                    saturation = it
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    OutlinedButton(
                        onClick = {
                            rotation = (rotation - 90) % 360
                        }
                    ) {
                        Text("↶ Поворот")
                    }

                    OutlinedButton(
                        onClick = {
                            brightness = 0f
                            contrast = 1f
                            saturation = 1f
                            rotation = 0
                        }
                    ) {
                        Text("Сброс")
                    }

                    OutlinedButton(
                        onClick = {
                            rotation = (rotation + 90) % 360
                        }
                    ) {
                        Text("↷ Поворот")
                    }
                }
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Text(
        label,
        color = ComposeColor.White,
        fontWeight = FontWeight.Medium
    )

    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = min..max
    )
}

fun saveEdited(
    context: android.content.Context,
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Int
) {
    try {
        val out = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )

        val paint = Paint().apply {

            val cm = android.graphics.ColorMatrix().apply {
                setSaturation(saturation)
            }

            val c = contrast
            val b = brightness * 255f

            val m = android.graphics.ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, b,
                    0f, c, 0f, 0f, b,
                    0f, 0f, c, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            cm.postConcat(m)

            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }

        android.graphics.Canvas(out).drawBitmap(
            source,
            0f,
            0f,
            paint
        )

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "PixelStudio_${System.currentTimeMillis()}.jpg"
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/PixelStudio"
            )
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                out.compress(
                    Bitmap.CompressFormat.JPEG,
                    95,
                    stream
                )
            }
        }

        Toast.makeText(
            context,
            "Фото сохранено в Pictures/PixelStudio",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Ошибка сохранения: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}
