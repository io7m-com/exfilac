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

import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.exfilac.core.EFSettings;

import java.io.IOException;
import java.sql.SQLException;

public final class EFQSettingsGet
  extends EFDatabaseQueryAbstract<DDatabaseUnit, EFSettings>
  implements EFQSettingsGetType {

  private static final String QUERY = """
    SELECT
      settings_text
    FROM
      settings
    """;

  EFQSettingsGet(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<DDatabaseUnit, EFSettings, EFQSettingsGetType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQSettingsGetType.class,
      EFQSettingsGet::new
    );
  }

  @Override
  protected EFSettings onExecute(
    final EFDatabaseTransactionType transaction,
    final DDatabaseUnit p)
    throws SQLException {
    var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      try (var rs = st.executeQuery()) {
        if (rs.next()) {
          return EFSettingsTexts.INSTANCE.deserializeFromBytes(
            rs.getBytes("settings_text")
          );
        } else {
          return EFSettings.Companion.defaults();
        }
      }
    } catch (final IOException e) {
      throw new SQLException(e);
    }
  }
}
