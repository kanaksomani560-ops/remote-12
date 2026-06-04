package com.controller

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private var outputStream: java.io.OutputStream? = null
    private var connected = false
    private val PORT = 5010

    // Joystick state
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private val joystickRadius = 150f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipInput = findViewById<EditText>(R.id.ipInput)
        val connectBtn = findViewById<Button>(R.id.connectBtn)
        val statusText = findViewById<TextView>(R.id.statusText)
        val joystickBase = findViewById<View>(R.id.joystickBase)
        val joystickKnob = findViewById<View>(R.id.joystickKnob)

        connectBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            if (ip.isEmpty()) { statusText.text = "Enter IP!"; return@setOnClickListener }
            if (!connected) {
                connectBtn.text = "Disconnect"
                statusText.text = "Connecting..."
                thread {
                    try {
                        val socket = java.net.Socket(ip, PORT)
                        outputStream = socket.getOutputStream()
                        connected = true
                        runOnUiThread { statusText.text = "Connected!" }
                    } catch (e: Exception) {
                        runOnUiThread {
                            statusText.text = "Error: ${e.message}"
                            connectBtn.text = "Connect"
                        }
                    }
                }
            } else {
                connected = false
                outputStream = null
                connectBtn.text = "Connect"
                statusText.text = "Disconnected"
            }
        }

        // Joystick touch
        joystickBase.post {
            joystickCenterX = joystickBase.width / 2f
            joystickCenterY = joystickBase.height / 2f
        }

        joystickBase.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - joystickBase.width / 2f
                    val dy = event.y - joystickBase.height / 2f
                    val dist = sqrt(dx * dx + dy * dy)
                    val maxDist = joystickRadius
                    val nx = if (dist > maxDist) dx / dist else dx / maxDist
                    val ny = if (dist > maxDist) dy / dist else dy / maxDist

                    joystickKnob.translationX = nx * maxDist
                    joystickKnob.translationY = ny * maxDist

                    sendJoystick(nx, ny)
                }
                MotionEvent.ACTION_UP -> {
                    joystickKnob.translationX = 0f
                    joystickKnob.translationY = 0f
                    sendJoystick(0f, 0f)
                }
            }
            true
        }

        // Setup all buttons
        setupButton(R.id.btnShoot, "shoot")
        setupButton(R.id.btnAim, "aim")
        setupButton(R.id.btnJump, "jump")
        setupButton(R.id.btnCrouch, "crouch")
        setupButton(R.id.btnProne, "prone")
        setupButton(R.id.btnReload, "reload")
        setupButton(R.id.btnPickup, "pickup")
        setupButton(R.id.btnMap, "map")
        setupButton(R.id.btnInventory, "inventory")
        setupButton(R.id.btnGrenade, "grenade")
        setupButton(R.id.btnSmoke, "smoke")
        setupButton(R.id.btnFlash, "flash")
        setupButton(R.id.btnBoost, "boost")
        setupButton(R.id.btnVehicle, "vehicle")
        setupButton(R.id.btnChat, "chat")
        setupButton(R.id.btnShift, "shift")
        setupButton(R.id.btnAlt, "alt")
        setupButton(R.id.btnSlot3, "slot3")
        setupButton(R.id.btnMelee, "melee")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButton(id: Int, key: String) {
        val btn = findViewById<View>(id)
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> sendKey(key, "down")
                MotionEvent.ACTION_UP -> sendKey(key, "up")
            }
            true
        }
    }

    private fun sendKey(key: String, event: String) {
        if (!connected) return
        thread {
            try {
                val msg = JSONObject()
                msg.put("action", "key")
                msg.put("key", key)
                msg.put("event", event)
                outputStream?.write((msg.toString() + "\n").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) { }
        }
    }

    private fun sendJoystick(x: Float, y: Float) {
        if (!connected) return
        thread {
            try {
                val msg = JSONObject()
                msg.put("action", "joystick")
                msg.put("x", x.toDouble())
                msg.put("y", y.toDouble())
                outputStream?.write((msg.toString() + "\n").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) { }
        }
    }
}
