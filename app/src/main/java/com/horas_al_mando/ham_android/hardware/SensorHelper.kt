package com.horas_al_mando.ham_android.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow

class SensorHelper(
    context: Context,
    private val onSensorUpdate: (altitude: Double, pressure: Double) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    fun start() {
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0].toDouble()
            val altitude = 44330.0 * (1.0 - (pressure / 1013.25).pow(1.0 / 5.25588))
            onSensorUpdate(altitude, pressure)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
