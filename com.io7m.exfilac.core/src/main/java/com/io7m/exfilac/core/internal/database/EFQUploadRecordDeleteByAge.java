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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

public final class EFQUploadRecordDeleteByAge
  extends EFDatabaseQueryAbstract<OffsetDateTime, DDatabaseUnit>
  implements EFQUploadRecordDeleteByAgeType {

  private static final String QUERY = """
    DELETE FROM upload_records WHERE
      upload_record_time_start < ?
    """;

  EFQUploadRecordDeleteByAge(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<OffsetDateTime, DDatabaseUnit, EFQUploadRecordDeleteByAgeType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadRecordDeleteByAgeType.class,
      EFQUploadRecordDeleteByAge::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final OffsetDateTime parameters)
    throws SQLException {
    final var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setTimestamp(1, timestampOf(parameters));
      st.execute();
    }
    return DDatabaseUnit.UNIT;
  }

  private Timestamp timestampOf(
    final OffsetDateTime time) {
    return new Timestamp(time.toInstant().toEpochMilli());
  }
}
