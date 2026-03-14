package com.synapse.social.studioasinc.shared.data.datasource

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshMessage
import com.synapse.social.studioasinc.shared.domain.model.mesh.MeshPeer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidMeshDataSource(private val context: Context) : MeshDataSource {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "com.synapse.social.mesh"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _nearbyPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
    override val nearbyPeers = _nearbyPeers.asStateFlow()

    private val _meshMessages = MutableSharedFlow<MeshMessage>()
    override val meshMessages = _meshMessages.asSharedFlow()

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val peer = MeshPeer(endpointId, info.endpointName, System.currentTimeMillis())
            _nearbyPeers.value = _nearbyPeers.value + peer
            // Automatically attempt connection for mesh
            connectionsClient.requestConnection(context.packageName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            _nearbyPeers.value = _nearbyPeers.value.filter { it.id != endpointId }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            // Handle connection result
        }

        override fun onDisconnected(endpointId: String) {
            _nearbyPeers.value = _nearbyPeers.value.filter { it.id != endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                try {
                    val message = Json.decodeFromString<MeshMessage>(bytes.decodeToString())
                    scope.launch {
                        _meshMessages.emit(message)
                    }
                } catch (e: Exception) {
                    // Log error
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    override suspend fun startDiscovery() {
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, DiscoveryOptions.Builder().setStrategy(strategy).build())
    }

    override suspend fun stopDiscovery() {
        connectionsClient.stopDiscovery()
    }

    override suspend fun startAdvertising() {
        connectionsClient.startAdvertising(context.packageName, serviceId, connectionLifecycleCallback, AdvertisingOptions.Builder().setStrategy(strategy).build())
    }

    override suspend fun stopAdvertising() {
        connectionsClient.stopAdvertising()
    }

    override suspend fun sendPayload(peerId: String, payload: String): Result<Unit> {
        connectionsClient.sendPayload(peerId, Payload.fromBytes(payload.encodeToByteArray()))
        return Result.success(Unit)
    }

    override suspend fun broadcastPayload(payload: String): Result<Unit> {
        val endpointIds = _nearbyPeers.value.map { it.id }
        if (endpointIds.isNotEmpty()) {
            connectionsClient.sendPayload(endpointIds, Payload.fromBytes(payload.encodeToByteArray()))
        }
        return Result.success(Unit)
    }
}
