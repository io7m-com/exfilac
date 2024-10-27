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


package com.io7m.exfilac.core.internal.database;

import com.io7m.darco.api.DDatabaseConnectionAbstract;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.api.DDatabaseTransactionCloseBehavior;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import io.opentelemetry.api.trace.Span;

final class EFDatabaseConnection
  extends DDatabaseConnectionAbstract<
  EFDatabaseConfiguration,
  EFDatabaseTransactionType,
  EFDatabaseQueryProviderType<?, ?, ?>>
  implements EFDatabaseConnectionType {

  private final Semaphore transactionSemaphore;

  EFDatabaseConnection(
    final EFDatabase database,
    final Span span,
    final Connection connection,
    final Map<Class<?>, EFDatabaseQueryProviderType<?, ?, ?>> queries) {
    super(database.configuration(), span, connection, queries);
    this.transactionSemaphore = new Semaphore(configuration().concurrency());
  }

  @Override
  protected EFDatabaseTransactionType createTransaction(
    final DDatabaseTransactionCloseBehavior closeBehavior,
    final Span transactionSpan,
    final Map<Class<?>, EFDatabaseQueryProviderType<?, ?, ?>> queries) {

    try {
      this.transactionSemaphore.acquire();
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }

    return new EFDatabaseTransaction(
      closeBehavior,
      this.configuration(),
      this,
      transactionSpan,
      queries
    );
  }
}
