package com.horas_al_mando.ham_android.model

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.*

object MockData {

    const val PILOT_ID          = 1L
    const val PILOT_NAME        = "Carlos Rodríguez"
    const val PILOT_LICENSE     = "PPA-12345"
    const val PILOT_TOTAL_HOURS = "320 hs"

    data class StoredFlight(
        val id      : Int,
        val label   : String,
        val duration: String,
        val distance: String,
        val payload : FlightSyncPayload,
    )

    val mockFlights: List<StoredFlight> = listOf(
        StoredFlight(
            id       = 1,
            label    = "Buenos Aires – 08/05/2026",
            duration = "28 min",
            distance = "~42 km",
            payload  = buildFlight(
                startLat    = -34.6037,
                startLng    = -58.3816,
                baseHdg     = 45.0,
                cruiseAlt   = 1500.0,
                cruiseSpeed = 95.0,
                points      = 56,
                minutesAgo  = 2880L,
            ),
        ),
        StoredFlight(
            id       = 2,
            label    = "Córdoba – 06/05/2026",
            duration = "22 min",
            distance = "~35 km",
            payload  = buildFlight(
                startLat    = -31.4201,
                startLng    = -64.1888,
                baseHdg     = 90.0,
                cruiseAlt   = 2200.0,
                cruiseSpeed = 110.0,
                points      = 44,
                minutesAgo  = 5760L,
            ),
        ),
        StoredFlight(
            id       = 3,
            label    = "Mendoza – 01/05/2026",
            duration = "34 min",
            distance = "~55 km",
            payload  = buildFlight(
                startLat    = -32.8894,
                startLng    = -68.8458,
                baseHdg     = 0.0,
                cruiseAlt   = 2800.0,
                cruiseSpeed = 118.0,
                points      = 68,
                minutesAgo  = 12960L,
            ),
        ),
    )

    private fun buildFlight(
        startLat   : Double,
        startLng   : Double,
        baseHdg    : Double,
        cruiseAlt  : Double,
        cruiseSpeed: Double,
        points     : Int,
        minutesAgo : Long,
    ): FlightSyncPayload {
        val intervalSec = 30L
        val start = Instant.now().minus(minutesAgo + points * intervalSec / 60L, ChronoUnit.MINUTES)

        val climbPts   = (points * 0.2).toInt().coerceAtLeast(2)
        val descentPts = (points * 0.2).toInt().coerceAtLeast(2)
        val midCruise  = climbPts + (points - climbPts - descentPts) / 2

        val path = mutableListOf<FlightPoint>()
        var lat = startLat
        var lng = startLng
        var hdg = baseHdg

        for (i in 0 until points) {
            val t = i * intervalSec.toDouble()

            val (altitude, vs) = when {
                i < climbPts             -> {
                    val progress = i.toDouble() / climbPts
                    val alt  = 500.0 + (cruiseAlt - 500.0) * progress
                    val rate = (cruiseAlt - 500.0) / (climbPts * intervalSec / 60.0)
                    Pair(alt, rate)
                }
                i >= points - descentPts -> {
                    val progress = (i - (points - descentPts)).toDouble() / descentPts
                    val alt  = cruiseAlt - (cruiseAlt - 400.0) * progress
                    val rate = -(cruiseAlt - 400.0) / (descentPts * intervalSec / 60.0)
                    Pair(alt, rate)
                }
                else -> Pair(cruiseAlt + sin(t * 0.002) * 8.0, sin(t * 0.003) * 4.0)
            }

            val speed = when {
                i < climbPts             -> cruiseSpeed * 0.85 + sin(t * 0.001) * 2.0
                i >= points - descentPts -> cruiseSpeed * 0.90 + sin(t * 0.001) * 2.0
                else                     -> cruiseSpeed + sin(t * 0.001) * 3.0
            }

            hdg = if (i == midCruise) (hdg + 20.0) % 360.0
                  else (hdg + sin(t * 0.005) * 0.3 + 360.0) % 360.0

            val pressure = 1013.25 * (1.0 - altitude / 44330.0).pow(5.2561)

            path.add(FlightPoint(
                lat           = lat,
                lng           = lng,
                timestamp     = start.plus(i * intervalSec, ChronoUnit.SECONDS).toString(),
                altitude      = altitude,
                speed         = speed,
                heading       = hdg,
                verticalSpeed = vs,
                pressure      = pressure,
            ))

            val speedMs = speed * 0.5144
            val distM   = speedMs * intervalSec.toDouble()
            val hdgRad  = Math.toRadians(hdg)
            lat += (distM * cos(hdgRad)) / 111320.0
            lng += (distM * sin(hdgRad)) / (111320.0 * cos(Math.toRadians(lat)))
        }

        return FlightSyncPayload(
            pilotId   = PILOT_ID,
            startTime = start.toString(),
            endTime   = start.plus(points * intervalSec, ChronoUnit.SECONDS).toString(),
            path      = path,
        )
    }
}
