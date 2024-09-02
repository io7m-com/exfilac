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


package com.io7m.exfilac.core.internal;

import com.io7m.darco.api.DDatabaseAbstract;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import io.opentelemetry.api.trace.Span;

/**
 * The database.
 */

public final class EFDatabase
  extends DDatabaseAbstract<
  EFDatabaseConfiguration,
  EFDatabaseConnectionType,
  EFDatabaseTransactionType,
  EFDatabaseQueryProviderType<?, ?, ?>>
  implements EFDatabaseType {
  /**
   * The database.
   *
   * @param inDataSource    The data source
   * @param inConfiguration The configuration
   * @param queryProviders  The query providers
   * @param resources       The resources
   */

  public EFDatabase(
    final EFDatabaseConfiguration inConfiguration,
    final SQLiteDataSource inDataSource,
    final Collection<EFDatabaseQueryProviderType<?, ?, ?>> queryProviders,
    final CloseableCollectionType<DDatabaseException> resources) {
    super(inConfiguration, inDataSource, queryProviders, resources);
  }

  private static void setWALMode(
    final Connection connection)
    throws DDatabaseException {
    try (var st = connection.createStatement()) {
      st.execute("PRAGMA journal_mode=WAL;");
    } catch (final SQLException e) {
      throw DDatabaseException.ofException(e);
    }
  }

  @Override
  protected EFDatabaseConnectionType createConnection(
    final Span span,
    final Connection connection,
    final Map<Class<?>, EFDatabaseQueryProviderType<?, ?, ?>> queries)
    throws DDatabaseException {
    setWALMode(connection);
    return new EFDatabaseConnection(
      this,
      span,
      connection,
      queries
    );
  }

  @Override
  public String description() {
    return "Database service.";
  }
}
