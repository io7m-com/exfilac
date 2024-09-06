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

import com.io7m.darco.api.DDatabaseException;
import com.io7m.exfilac.core.internal.EFUploadID;
import com.io7m.exfilac.core.internal.EFUploadRecord;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public final class EFQUploadRecordCreate
  extends EFDatabaseQueryAbstract<EFQUploadRecordCreateParameters, EFUploadRecord>
  implements EFQUploadRecordCreateType {

  private static final String QUERY = """
    INSERT INTO upload_records (
      upload_record_time_start,
      upload_name,
      upload_reason,
      upload_files_required,
      upload_files_skipped,
      upload_files_uploaded,
      upload_files_failed
    ) VALUES (
      ?,
      ?,
      ?,
      0,
      0,
      0,
      0
    ) RETURNING upload_record_id
    """;

  EFQUploadRecordCreate(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFQUploadRecordCreateParameters, EFUploadRecord, EFQUploadRecordCreateType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadRecordCreateType.class,
      EFQUploadRecordCreate::new
    );
  }

  @Override
  protected EFUploadRecord onExecute(
    final EFDatabaseTransactionType transaction,
    final EFQUploadRecordCreateParameters parameters)
    throws SQLException {
    final var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setTimestamp(1, timestampOf(parameters.getTimeStart()));
      st.setString(2, parameters.getUploadName().getValue());
      st.setString(3, parameters.getReason());
      try (var rs = st.executeQuery()) {
        return new EFUploadRecord(
          EFUploadID.Companion.of(rs.getLong(1)),
          parameters.getTimeStart(),
          null,
          parameters.getUploadName(),
          null,
          parameters.getReason(),
          0L,
          0L,
          0L,
          0L,
          null
        );
      }
    }
  }

  private Timestamp timestampOf(
    final OffsetDateTime time) {
    return new Timestamp(time.toInstant().toEpochMilli());
  }
}
