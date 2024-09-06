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

import static java.time.ZoneOffset.UTC;

import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.exfilac.core.internal.EFUploadEventID;
import com.io7m.exfilac.core.internal.EFUploadEventRecord;
import com.io7m.exfilac.core.internal.EFUploadID;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EFQUploadEventRecordList
  extends EFDatabaseQueryAbstract<EFQUploadEventRecordListParameters, List<EFUploadEventRecord>>
  implements EFQUploadEventRecordListType {

  private static final String QUERY = """
    SELECT
      upload_event_id,
      upload_event_record_id,
      upload_event_time,
      upload_event_message,
      upload_event_file,
      upload_event_exception,
      upload_event_failed
    FROM
      upload_events
    WHERE
      upload_event_record_id = ? AND upload_event_time >= ?
    ORDER BY upload_event_time, upload_event_id ASC
    LIMIT ?
    """;

  EFQUploadEventRecordList(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFQUploadEventRecordListParameters, List<EFUploadEventRecord>, EFQUploadEventRecordListType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadEventRecordListType.class,
      EFQUploadEventRecordList::new
    );
  }

  @Override
  protected List<EFUploadEventRecord> onExecute(
    final EFDatabaseTransactionType transaction,
    final EFQUploadEventRecordListParameters parameters)
    throws SQLException {
    final var connection = transaction.connection();

    var results = new ArrayList<EFUploadEventRecord>();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setLong(1, parameters.getUpload().toLong());
      st.setTimestamp(2, timestampOf(parameters.getTimeStart()));
      st.setInt(3, parameters.getLimit());
      try (var rs = st.executeQuery()) {
        while (rs.next()) {
          results.add(
            new EFUploadEventRecord(
              EFUploadEventID.Companion.of(rs.getLong("upload_event_id")),
              EFUploadID.Companion.of(rs.getLong("upload_event_record_id")),
              timeOf(rs.getLong("upload_event_time")),
              rs.getString("upload_event_message"),
              rs.getString("upload_event_file"),
              rs.getString("upload_event_exception"),
              rs.getBoolean("upload_event_failed")
            )
          );
        }
      }
    }
    return results;
  }

  private Timestamp timestampOf(
    final OffsetDateTime time) {
    return new Timestamp(time.toInstant().toEpochMilli());
  }

  private OffsetDateTime timeOf(
    long time) {
    return OffsetDateTime.ofInstant(
      Instant.ofEpochMilli(time),
      UTC
    );
  }
}
