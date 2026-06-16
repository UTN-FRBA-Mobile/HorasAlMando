package com.horas_al_mando.ham_android.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
fun getLastLocation(context: Context, onResult: (Double?, Double?) -> Unit) {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        onResult(null, null)
        return
    }
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) onResult(location.latitude, location.longitude)
            else onResult(null, null)
        }
        .addOnFailureListener { onResult(null, null) }
}
