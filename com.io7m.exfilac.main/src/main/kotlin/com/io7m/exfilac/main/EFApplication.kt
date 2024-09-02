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

import android.app.Application
import com.io7m.exfilac.core.ExfilacFactory
import com.io7m.exfilac.core.ExfilacType

class EFApplication : Application() {

  private lateinit var exfilacField: ExfilacType

  val exfilac: ExfilacType
    get() = this.exfilacField

  companion object {
    private lateinit var INSTANCE: EFApplication

    @JvmStatic
    val application: EFApplication
      get() = this.INSTANCE
  }

  override fun onCreate() {
    super.onCreate()
    INSTANCE = this
    this.exfilacField = ExfilacOnUI(ExfilacFactory.open(application.dataDir.toPath()))
  }
}
