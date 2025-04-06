package com.example.bluetoothsnake

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSIONS_NEEDED,
    BLUETOOTH_DISABLED,
    BLUETOOTH_UNSUPPORTED,
    DEVICE_NOT_FOUND,
    DEVICE_SELECTION
}

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val device: BluetoothDevice
)

class MainActivity : ComponentActivity() {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var selectedDevice: BluetoothDeviceInfo? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter
    }
    private var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    private var monitorConnectionJob: Job? = null
    private val pairedDevices = mutableStateListOf<BluetoothDeviceInfo>()
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                if (bluetoothAdapter?.isEnabled == true) {
                    connectionState = ConnectionState.DISCONNECTED
                    showToast("Permissions granted. Ready to connect.")
                    loadPairedDevices()
                } else {
                    connectionState = ConnectionState.BLUETOOTH_DISABLED
                    showToast("Permissions granted, but Bluetooth is disabled.")
                }
            } else {
                showToast("Bluetooth permissions are required to use this app.")
                connectionState = ConnectionState.PERMISSIONS_NEEDED
            }
        }
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                        if (connectionState != ConnectionState.BLUETOOTH_UNSUPPORTED) {
                            showToast("Bluetooth was turned off.")
                            handleDisconnect(ConnectionState.BLUETOOTH_DISABLED)
                        }
                    }

                    BluetoothAdapter.STATE_ON -> {
                        if (connectionState == ConnectionState.BLUETOOTH_DISABLED) {
                            showToast("Bluetooth turned on.")
                            connectionState = ConnectionState.DISCONNECTED
                            checkBluetoothState()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (bluetoothAdapter == null) {
            connectionState = ConnectionState.BLUETOOTH_UNSUPPORTED
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SnakeControllerUI(
                        state = connectionState,
                        pairedDevices = pairedDevices,
                        selectedDevice = selectedDevice,
                        onDeviceSelect = { device ->
                            selectedDevice = device
                            connectionState = ConnectionState.DISCONNECTED
                        },
                        onShowDevices = { showDeviceSelection() },
                        onDirectionClick = { direction -> sendCommand(direction) },
                        onConnectClick = { tryConnect() },
                        onRequestPermissions = {
                            requestMultiplePermissionsLauncher.launch(requiredPermissions)
                        },
                        onDisconnectClick = { closeConnection() }
                    )
                }
            }
        }
        checkBluetoothState()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothStateReceiver, filter)
        }
        checkBluetoothState()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothStateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
    }

    private fun checkBluetoothState() {
        if (connectionState == ConnectionState.BLUETOOTH_UNSUPPORTED) {
            return
        }
        if (!hasRequiredPermissions()) {
            connectionState = ConnectionState.PERMISSIONS_NEEDED
            return
        }
        if (bluetoothAdapter?.isEnabled == false) {
            connectionState = ConnectionState.BLUETOOTH_DISABLED
            return
        }
        if (connectionState != ConnectionState.CONNECTED &&
            connectionState != ConnectionState.CONNECTING
        ) {
            loadPairedDevices()
            if (connectionState != ConnectionState.DEVICE_SELECTION) {
                connectionState = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val adapter = bluetoothAdapter ?: run {
            connectionState = ConnectionState.BLUETOOTH_UNSUPPORTED
            return
        }
        if (!adapter.isEnabled) {
            connectionState = ConnectionState.BLUETOOTH_DISABLED
            return
        }
        try {
            val currentBondedDevices = adapter.bondedDevices
            pairedDevices.clear()
            currentBondedDevices?.forEach { device ->
                pairedDevices.add(
                    BluetoothDeviceInfo(
                        name = device.name,
                        address = device.address,
                        device = device
                    )
                )
            }
            if (pairedDevices.isEmpty() && connectionState == ConnectionState.DEVICE_SELECTION) {
                showToast("No paired devices found. Pair in system settings.")
            }
        } catch (e: Exception) {
            showToast("Error loading paired devices: ${e.message}")
        }
    }

    private fun showDeviceSelection() {
        if (!hasRequiredPermissions()) {
            connectionState = ConnectionState.PERMISSIONS_NEEDED
            showToast("Permissions required to view devices.")
            return
        }
        if (bluetoothAdapter?.isEnabled == false) {
            connectionState = ConnectionState.BLUETOOTH_DISABLED
            showToast("Enable Bluetooth to view devices.")
            return
        }
        loadPairedDevices()
        connectionState = ConnectionState.DEVICE_SELECTION
    }

    @Composable
    fun SnakeControllerUI(
        state: ConnectionState,
        pairedDevices: List<BluetoothDeviceInfo>,
        selectedDevice: BluetoothDeviceInfo?,
        onDeviceSelect: (BluetoothDeviceInfo) -> Unit,
        onShowDevices: () -> Unit,
        onDirectionClick: (String) -> Unit,
        onConnectClick: () -> Unit,
        onRequestPermissions: () -> Unit,
        onDisconnectClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusText(state)
            Spacer(modifier = Modifier.height(10.dp))
            if (selectedDevice != null && state != ConnectionState.DEVICE_SELECTION) {
                Text(
                    text = "Device: ${selectedDevice.name ?: "Unknown"} (${selectedDevice.address})",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (state == ConnectionState.CONNECTED) {
                Button(onClick = onDisconnectClick) {
                    Text("Disconnect")
                }
            }
            when (state) {
                ConnectionState.CONNECTING -> {
                    CircularProgressIndicator()
                }

                ConnectionState.DEVICE_SELECTION -> {
                    DeviceSelectionList(
                        devices = pairedDevices,
                        onDeviceSelect = onDeviceSelect
                    )
                }

                ConnectionState.DISCONNECTED,
                ConnectionState.DEVICE_NOT_FOUND,
                ConnectionState.BLUETOOTH_DISABLED -> {
                    ConnectionActionButtons(
                        state = state,
                        hasSelectedDevice = selectedDevice != null,
                        onShowDevices = onShowDevices,
                        onConnectClick = onConnectClick
                    )
                }

                ConnectionState.PERMISSIONS_NEEDED -> {
                    Button(onClick = onRequestPermissions) {
                        Text("Grant Permissions")
                    }
                }

                ConnectionState.BLUETOOTH_UNSUPPORTED -> {
                    Text(
                        text = "This device does not support Bluetooth.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ConnectionState.CONNECTED -> Unit
            }
            Spacer(modifier = Modifier.height(if (state == ConnectionState.DEVICE_SELECTION) 16.dp else 64.dp))
            if (state != ConnectionState.DEVICE_SELECTION) {
                ControlButtons(
                    enabled = (state == ConnectionState.CONNECTED),
                    onDirectionClick = onDirectionClick
                )
            }
        }
    }

    @Composable
    private fun StatusText(state: ConnectionState) {
        val statusText = when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected ❌"
            ConnectionState.CONNECTING -> "Connecting..."
            ConnectionState.CONNECTED -> "Connected ✅"
            ConnectionState.PERMISSIONS_NEEDED -> "Permissions Required"
            ConnectionState.BLUETOOTH_DISABLED -> "Bluetooth Disabled"
            ConnectionState.BLUETOOTH_UNSUPPORTED -> "Bluetooth Not Supported"
            ConnectionState.DEVICE_NOT_FOUND -> "Device Not Found"
            ConnectionState.DEVICE_SELECTION -> "Select a Device"
        }
        Text(
            text = "Status: $statusText",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun ConnectionActionButtons(
        state: ConnectionState,
        hasSelectedDevice: Boolean,
        onShowDevices: () -> Unit,
        onConnectClick: () -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onShowDevices,
                enabled = state != ConnectionState.BLUETOOTH_DISABLED
            ) {
                Text("Select Device")
            }
            Button(
                onClick = onConnectClick,
                enabled = state != ConnectionState.BLUETOOTH_DISABLED && hasSelectedDevice
            ) {
                Text("Connect")
            }
        }
    }

    @Composable
    private fun DeviceSelectionList(
        devices: List<BluetoothDeviceInfo>,
        onDeviceSelect: (BluetoothDeviceInfo) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (devices.isEmpty()) {
                EmptyDeviceList()
            } else {
                PopulatedDeviceList(devices, onDeviceSelect)
            }
        }
    }

    @Composable
    private fun EmptyDeviceList() {
        Text(
            text = "No paired devices found",
            modifier = Modifier.padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Pair your target device in Android Bluetooth settings first.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }

    @Composable
    private fun PopulatedDeviceList(
        devices: List<BluetoothDeviceInfo>,
        onDeviceSelect: (BluetoothDeviceInfo) -> Unit
    ) {
        Text(
            text = "Select a paired device:",
            modifier = Modifier.padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
        LazyColumn(
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(devices) { device ->
                DeviceCard(device, onDeviceSelect)
            }
        }
    }

    @Composable
    private fun DeviceCard(
        device: BluetoothDeviceInfo,
        onDeviceSelect: (BluetoothDeviceInfo) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onDeviceSelect(device) }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun ControlButtons(enabled: Boolean, onDirectionClick: (String) -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onDirectionClick("up") },
                modifier = Modifier.size(100.dp, 50.dp),
                enabled = enabled
            ) { Text("UP") }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onDirectionClick("left") },
                    modifier = Modifier.size(100.dp, 50.dp),
                    enabled = enabled
                ) { Text("LEFT") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { onDirectionClick("right") },
                    modifier = Modifier.size(100.dp, 50.dp),
                    enabled = enabled
                ) { Text("RIGHT") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onDirectionClick("down") },
                modifier = Modifier.size(100.dp, 50.dp),
                enabled = enabled
            ) { Text("DOWN") }
        }
    }

    private fun tryConnect() {
        if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.CONNECTED) {
            return
        }

        if (selectedDevice == null) {
            showToast("Please select a device first")
            showDeviceSelection()
            return
        }

        if (!hasRequiredPermissions()) {
            connectionState = ConnectionState.PERMISSIONS_NEEDED
            showToast("Permissions required to connect.")
            requestMultiplePermissionsLauncher.launch(requiredPermissions)
            return
        }

        val adapter = bluetoothAdapter ?: run {
            connectionState = ConnectionState.BLUETOOTH_UNSUPPORTED
            showToast("Bluetooth not supported on this device.")
            return
        }

        if (!adapter.isEnabled) {
            connectionState = ConnectionState.BLUETOOTH_DISABLED
            showToast("Bluetooth is disabled. Please enable it.")
            return
        }

        connectionState = ConnectionState.CONNECTING
        showToast("Connecting to ${selectedDevice?.name ?: selectedDevice?.address}...")

        lifecycleScope.launch(Dispatchers.IO) {
            connectToDevice()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectToDevice() {
        closeConnectionResources()
        val deviceToConnect = selectedDevice?.device ?: run {
            withContext(Dispatchers.Main) {
                connectionState = ConnectionState.DEVICE_NOT_FOUND
                showToast("Error: No device selected for connection.")
            }
            return
        }
        try {
            bluetoothSocket = deviceToConnect.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream

            monitorConnectionJob = lifecycleScope.launch(Dispatchers.IO) {
                monitorConnection()
            }

            withContext(Dispatchers.Main) {
                connectionState = ConnectionState.CONNECTED
                showToast("Connected to ${selectedDevice?.name ?: deviceToConnect.address}")
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                val deviceName = selectedDevice?.name ?: deviceToConnect.address
                showToast("Connection failed to $deviceName.")
                handleDisconnect(ConnectionState.DISCONNECTED)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Connection failed: ${e.message ?: "Unknown error."}")
                handleDisconnect(ConnectionState.DISCONNECTED)
            }
        }
    }

    private suspend fun monitorConnection() {
        val stream = inputStream ?: return
        val buffer = ByteArray(1024)
        try {
            while (coroutineContext.isActive && bluetoothSocket?.isConnected == true) {
                val bytesRead = withContext(Dispatchers.IO) {
                    stream.read(buffer)
                }
                if (bytesRead == -1) break
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            withContext(Dispatchers.Main) {
                if (connectionState == ConnectionState.CONNECTED) {
                    showToast("Connection lost.")
                    handleDisconnect(ConnectionState.DISCONNECTED)
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        if (connectionState != ConnectionState.CONNECTED) {
            showToast("Cannot send: Not connected.")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write("$command\n".toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showToast("Send failed: Connection lost.")
                    handleDisconnect(ConnectionState.DISCONNECTED)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Send failed: ${e.message}")
                    handleDisconnect(ConnectionState.DISCONNECTED)
                }
            }
        }
    }

    private fun handleDisconnect(newState: ConnectionState = ConnectionState.DISCONNECTED) {
        monitorConnectionJob?.cancel()
        monitorConnectionJob = null

        lifecycleScope.launch(Dispatchers.IO) {
            closeConnectionResources()
            withContext(Dispatchers.Main) {
                connectionState = newState
            }
        }
    }

    private fun closeConnectionResources() {
        try {
            inputStream?.close()
        } catch (_: IOException) {
        }

        try {
            outputStream?.close()
        } catch (_: IOException) {
        }

        try {
            bluetoothSocket?.close()
        } catch (_: IOException) {
        }

        inputStream = null
        outputStream = null
        bluetoothSocket = null
    }

    private fun closeConnection() {
        showToast("Disconnecting...")
        handleDisconnect(ConnectionState.DISCONNECTED)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}