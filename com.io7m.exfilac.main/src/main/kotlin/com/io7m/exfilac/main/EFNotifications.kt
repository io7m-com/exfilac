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

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object EFNotifications {

  private const val NOTIFICATION_CHANNEL_ID =
    "com.io7m.exfilac.main.notifications"

  fun notificationsDisplayDialog(
    context: Activity,
    onDismiss: Runnable
  ) {
    if (Build.VERSION.SDK_INT >= 33) {
      if (!notificationsArePermitted(context)) {
        MaterialAlertDialogBuilder(context)
          .setMessage(R.string.notification_explanation)
          .setOnDismissListener {
            try {
              context.requestPermissions(
                arrayOf("android.permission.POST_NOTIFICATIONS"),
                1000
              )
            } finally {
              onDismiss.run()
            }
          }
          .show()
      }
    }
  }

  fun notificationsArePermitted(
    context: Context
  ): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      "android.permission.POST_NOTIFICATIONS"
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun createNotificationChannel(
    context: Context
  ) {
    val notificationManager =
      context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(
      NOTIFICATION_CHANNEL_ID,
      context.getString(R.string.notification_title),
      NotificationManager.IMPORTANCE_LOW
    )
    notificationManager.createNotificationChannel(channel)
  }

  fun buildNotification(
    context: Context
  ): Notification {
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
      .setContentTitle(context.getString(R.string.notification_title))
      .setContentText(context.getString(R.string.notification_description))
      .setSmallIcon(R.drawable.io7m_inverse_framed_24)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .setContentIntent(
        Intent(context, EFActivity::class.java).let { notificationIntent ->
          PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
      )
      .build()
  }
}
