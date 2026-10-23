package com.gestor_ventures.front.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Avatar con las iniciales de un usuario o negocio ("Dulce Antojo" → "DA"). El tamaño de la
 * letra sale del tamaño del recuadro, para que se vea igual en cualquier medida.
 */
@Composable
fun GvAvatar(
    iniciales: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: RoundedCornerShape = RoundedCornerShape(size / 3.6f),
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    foto: String? = null,
) {
    val imagen = rememberFoto(foto, with(LocalDensity.current) { size.roundToPx() })

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }
        Text(
            text = iniciales,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.39f).sp,
            ),
            color = contentColor,
        )
    }
}

@Composable
private fun rememberFoto(foto: String?, ladoPx: Int): ImageBitmap? {
    val context = LocalContext.current
    val imagen by produceState<ImageBitmap?>(initialValue = null, foto, ladoPx) {
        value = foto?.let { withContext(Dispatchers.IO) { decodificar(context, it, ladoPx) } }
    }
    return imagen
}

private fun decodificar(context: Context, foto: String, ladoPx: Int): ImageBitmap? = runCatching {
    val uri = foto.toUri()
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }

    var muestreo = 1
    while (minOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= ladoPx) muestreo *= 2

    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    context.contentResolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, opciones) }
        ?.asImageBitmap()
}.getOrNull()
