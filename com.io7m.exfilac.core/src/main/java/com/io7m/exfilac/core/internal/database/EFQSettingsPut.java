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

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.sql.SQLException;

public final class EFQSettingsPut
  extends EFDatabaseQueryAbstract<EFSettings, DDatabaseUnit>
  implements EFQSettingsPutType {

  private static final String QUERY_INSERT = """
    INSERT OR REPLACE INTO settings (settings_text) VALUES (?)
    """;

  private static final String QUERY_UPDATE = """
    UPDATE settings SET settings_text = ?
    """;

  EFQSettingsPut(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFSettings, DDatabaseUnit, EFQSettingsPutType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQSettingsPutType.class,
      EFQSettingsPut::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final EFSettings settings)
    throws SQLException {
    var connection = transaction.connection();

    byte[] settingsText;
    try {
      settingsText = EFSettingsTexts.INSTANCE.serializeToBytes(settings);
    } catch (IOException e) {
      throw new SQLException(e);
    }

    try (var sti = connection.prepareStatement(QUERY_INSERT)) {
      sti.setBytes(1, settingsText);
      try {
        sti.execute();
      } catch (SQLiteException e) {
        if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK) {
          try (var stu = connection.prepareStatement(QUERY_UPDATE)) {
            stu.setBytes(1, settingsText);
            stu.execute();
          }
        } else {
          throw e;
        }
      }
    }
    return DDatabaseUnit.UNIT;
  }
}
