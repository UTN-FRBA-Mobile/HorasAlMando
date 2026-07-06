package com.horas_al_mando.ham_android.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.horas_al_mando.ham_android.MainActivity
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.db.AppDatabase
import com.horas_al_mando.ham_android.db.InProgressFlightPointEntity
import com.horas_al_mando.ham_android.hardware.LocationHelper
import com.horas_al_mando.ham_android.hardware.SensorHelper
import com.horas_al_mando.ham_android.model.FlightPoint
import com.horas_al_mando.ham_android.network.FlightSyncSocketRepository
import com.horas_al_mando.ham_android.network.SessionManager
import com.horas_al_mando.ham_android.network.SocialRadarRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

object FlightRepository {
    private val _currentFlightPath = MutableStateFlow<List<FlightPoint>>(emptyList())
    val currentFlightPath: StateFlow<List<FlightPoint>> = _currentFlightPath.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var startTime: String = ""
    private var currentPilotId: Long = 0L
    private var currentClientFlightId: String = ""

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    fun updateFlight(point: FlightPoint) {
        _currentFlightPath.value = _currentFlightPath.value + point
    }

    fun updateElapsed(seconds: Long) {
        _elapsedSeconds.value = seconds
    }

    fun setPilotId(id: Long) {
        currentPilotId = id
    }

    fun getPilotId(): Long = currentPilotId

    fun getClientFlightId(): String = currentClientFlightId

    fun setClientFlightId(id: String) {
        currentClientFlightId = id
    }

    fun getStartTime(): String = startTime

    fun restorePoints(points: List<FlightPoint>) {
        if (points.isNotEmpty()) {
            _currentFlightPath.value = points
            startTime = points.first().timestamp
            _elapsedSeconds.value = 0L
        }
    }

    fun getSyncPayload(): com.horas_al_mando.ham_android.model.FlightSyncPayload {
        return com.horas_al_mando.ham_android.model.FlightSyncPayload(
            clientFlightId = currentClientFlightId,
            startTime = startTime,
            endTime = Instant.now().toString(),
            path = _currentFlightPath.value
        )
    }

    fun setTracking(tracking: Boolean) {
        _isTracking.value = tracking
        if (tracking) {
            startTime = Instant.now().toString()
            if (currentClientFlightId.isBlank()) {
                currentClientFlightId = UUID.randomUUID().toString()
            }
        } else {
            _currentFlightPath.value = emptyList()
            _elapsedSeconds.value = 0
        }
    }

    fun clearFlight() {
        currentClientFlightId = ""
        startTime = ""
    }
}

class FlightTrackingService : Service() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    private var timerJob: Job? = null
    private var streamJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentAltitude = 0.0
    private var currentPressure = 1013.25
    private var secondsTracked = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        locationHelper = LocationHelper(this) { location ->
            val point = FlightPoint(
                lat = location.latitude,
                lng = location.longitude,
                timestamp = Instant.now().toString(),
                altitude = currentAltitude,
                speed = location.speed * 1.94384,
                heading = location.bearing.toDouble(),
                verticalSpeed = 0.0,
                pressure = currentPressure
            )
            FlightRepository.updateFlight(point)
            ActiveCircuitRepository.onLocation(point.lat, point.lng)

            serviceScope.launch {
                AppDatabase.getInstance(applicationContext).inProgressFlightDao().insert(
                    InProgressFlightPointEntity(
                        lat = point.lat,
                        lng = point.lng,
                        timestamp = point.timestamp,
                        altitude = point.altitude ?: 0.0,
                        speed = point.speed ?: 0.0,
                        heading = point.heading ?: 0.0,
                        verticalSpeed = point.verticalSpeed ?: 0.0,
                        pressure = point.pressure ?: 1013.25
                    )
                )
            }
        }

        sensorHelper = SensorHelper(this) { altitude, pressure ->
            currentAltitude = altitude
            currentPressure = pressure
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(getString(R.string.notification_tracking_active))
        startForeground(NOTIFICATION_ID, notification)

        locationHelper.startLocationUpdates()
        sensorHelper.start()

        val sessionManager = SessionManager(applicationContext)
        sessionManager.getClientFlightId()?.takeIf { it.isNotBlank() }?.let {
            FlightRepository.setClientFlightId(it)
        }
        FlightRepository.setTracking(true)
        sessionManager.saveClientFlightId(FlightRepository.getClientFlightId())

        if (ActiveCircuitRepository.activeCircuit.value != null) {
            ActiveCircuitRepository.beginRun()
        }

        serviceScope.launch {
            val existing = AppDatabase.getInstance(applicationContext).inProgressFlightDao().getAll()
            if (existing.isNotEmpty()) {
                val points = existing.map {
                    FlightPoint(
                        lat = it.lat,
                        lng = it.lng,
                        timestamp = it.timestamp,
                        altitude = it.altitude,
                        speed = it.speed,
                        heading = it.heading,
                        verticalSpeed = it.verticalSpeed,
                        pressure = it.pressure
                    )
                }
                FlightRepository.restorePoints(points)
                secondsTracked = existing.size * 2L
            }
        }

        FlightSyncSocketRepository.connect(
            FlightRepository.getClientFlightId(),
            FlightRepository.getStartTime()
        )
        SocialRadarRepository.connect()

        streamJob = serviceScope.launch {
            while (isActive) {
                delay(3000)
                val path = FlightRepository.currentFlightPath.value
                FlightSyncSocketRepository.pushPoints(path)
                path.lastOrNull()?.let { SocialRadarRepository.sendLocation(it) }
            }
        }

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                secondsTracked++
                FlightRepository.updateElapsed(secondsTracked)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        streamJob?.cancel()
        FlightSyncSocketRepository.disconnect()
        SocialRadarRepository.disconnect()
        serviceScope.cancel()
        locationHelper.stopLocationUpdates()
        sensorHelper.stop()
        FlightRepository.setTracking(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "flight_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
