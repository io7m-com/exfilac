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

import android.os.Handler
import android.os.Looper

/**
 * Utility functions to execute code on the Android UI thread.
 */

object EFUIThread {

  /**
   * Check that the current thread is the UI thread and raise {@link IllegalStateException}
   * if it isn't.
   */

  fun checkIsUIThread() {
    if (isUIThread() == false) {
      throw IllegalStateException(
        String.format(
          "Current thread '%s' is not the Android UI thread",
          Thread.currentThread(),
        ),
      )
    }
  }

  /**
   * @return `true` iff the current thread is the UI thread.
   */

  fun isUIThread(): Boolean {
    return Looper.getMainLooper().thread === Thread.currentThread()
  }

  /**
   * Run the given Runnable on the UI thread.
   *
   * @param r The runnable
   */

  fun runOnUIThread(r: Runnable) {
    if (isUIThread()) {
      return r.run()
    }

    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post(r)
  }

//  /**
//   * Observe the given attribute on the UI thread.
//   */
//
//  fun <T> observeOnUIThread(
//    attribute: AttributeReadableType<T>,
//    observer: (T, T) -> Unit
//  ): AttributeSubscriptionType {
//    return attribute.subscribe { oldValue, newValue ->
//      runOnUIThread {
//        try {
//          observer.invoke(oldValue, newValue)
//        } catch (e: Throwable) {
//          // Nothing we can do about this.
//        }
//      }
//    }
//  }
}
