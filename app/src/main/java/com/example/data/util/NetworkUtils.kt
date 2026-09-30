package com.example.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkUtils {

  private val _isOnline = MutableStateFlow(value = true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var isInitialized = false

  fun init(context: Context) {
    if (isInitialized) return
    isInitialized = true

    val connectivityManager = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
      ?: return

    // Comprobación inicial síncrona pasiva
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
    val initialOnline = (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) &&
      (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    _isOnline.value = initialOnline

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      .build()

    connectivityManager.registerNetworkCallback(
      request,
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          _isOnline.value = true
        }

        override fun onLost(network: Network) {
          _isOnline.value = false
        }

        override fun onCapabilitiesChanged(
          network: Network,
          networkCapabilities: NetworkCapabilities,
        ) {
          val valid = (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) &&
            (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
          _isOnline.value = valid
        }
      },
    )
  }
}
