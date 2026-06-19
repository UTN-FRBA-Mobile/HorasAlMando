package com.horas_al_mando.ham_android.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun PlaneMarker(
    markerState: MarkerState,
    headingDegrees: Float,
    tint: Color,
    title: String? = null,
    snippet: String? = null,
) {
    MarkerComposable(
        title ?: "",
        tint.value,
        state = markerState,
        title = title,
        snippet = snippet,
        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
        rotation = headingDegrees,
        flat = true,
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(40.dp),
        )
    }
}
