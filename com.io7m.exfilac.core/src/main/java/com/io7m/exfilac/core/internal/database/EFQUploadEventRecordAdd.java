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
import com.io7m.exfilac.core.internal.EFUploadEventRecord;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

public final class EFQUploadEventRecordAdd
  extends EFDatabaseQueryAbstract<EFUploadEventRecord, DDatabaseUnit>
  implements EFQUploadEventRecordAddType {

  private static final String QUERY = """
    INSERT INTO upload_events (
      upload_event_record_id,
      upload_event_time,
      upload_event_message,
      upload_event_file,
      upload_event_exception,
      upload_event_failed
    ) VALUES (
      ?,
      ?,
      ?,
      ?,
      ?,
      ?
    ) RETURNING upload_event_id
    """;

  EFQUploadEventRecordAdd(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFUploadEventRecord, DDatabaseUnit, EFQUploadEventRecordAddType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadEventRecordAddType.class,
      EFQUploadEventRecordAdd::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final EFUploadEventRecord parameters)
    throws SQLException {
    final var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setLong(1, parameters.getUploadID().toLong());
      st.setTimestamp(2, timestampOf(parameters.getTime()));
      st.setString(3, parameters.getMessage());
      st.setString(4, parameters.getFile());
      st.setString(5, parameters.getExceptionTrace());
      st.setInt(6, parameters.getFailed() ? 1 : 0);
      st.execute();
    }
    return DDatabaseUnit.UNIT;
  }

  private Timestamp timestampOf(
    final OffsetDateTime time) {
    return new Timestamp(time.toInstant().toEpochMilli());
  }
}