/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.exfilac.main

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import com.io7m.exfilac.core.EFNetworkStatus.NETWORK_STATUS_CELLULAR
import com.io7m.exfilac.core.EFNetworkStatus.NETWORK_STATUS_UNAVAILABLE
import com.io7m.exfilac.core.EFNetworkStatus.NETWORK_STATUS_WIFI
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class EFNetworkConnectivityService : Service() {

  private val logger =
    LoggerFactory.getLogger(EFNetworkConnectivityService::class.java)

  private val started =
    AtomicBoolean(false)

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    if (this.started.compareAndSet(false, true)) {
      this.logger.debug("Starting network service…")

      val networkRequest =
        NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
          .build()

      val networkCallback =
        object : NetworkCallback() {
          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
          ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
              EFApplication.application.exfilac.networkStatusSet(NETWORK_STATUS_WIFI)
              return
            }
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
              EFApplication.application.exfilac.networkStatusSet(NETWORK_STATUS_CELLULAR)
              return
            }
          }

          override fun onLost(network: Network) {
            super.onLost(network)
            EFApplication.application.exfilac.networkStatusSet(NETWORK_STATUS_UNAVAILABLE)
          }

          override fun onUnavailable() {
            super.onUnavailable()
            EFApplication.application.exfilac.networkStatusSet(NETWORK_STATUS_UNAVAILABLE)
          }
        }

      val connectivityManager =
        this.getSystemService(ConnectivityManager::class.java) as ConnectivityManager

      connectivityManager.requestNetwork(networkRequest, networkCallback)
    } else {
      this.logger.debug("Ignoring redundant request to start network service.")
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}
