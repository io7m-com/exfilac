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

import com.io7m.exfilac.core.EFBucketReferenceName;
import com.io7m.exfilac.core.EFUploadName;
import com.io7m.exfilac.core.EFUploadResult;
import com.io7m.exfilac.core.internal.EFUploadID;
import com.io7m.exfilac.core.internal.EFUploadRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

public final class EFQUploadRecordGet
  extends EFDatabaseQueryAbstract<EFUploadID, Optional<EFUploadRecord>>
  implements EFQUploadRecordGetType {

  private static final String QUERY = """
    SELECT
      upload_record_id,
      upload_record_time_start,
      upload_record_time_end,
      upload_name,
      upload_bucket,
      upload_reason,
      upload_files_required,
      upload_files_skipped,
      upload_files_uploaded,
      upload_files_failed,
      upload_result
    FROM
      upload_records
    WHERE
      upload_record_id = ?
    LIMIT 1
    """;

  EFQUploadRecordGet(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFUploadID, Optional<EFUploadRecord>, EFQUploadRecordGetType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadRecordGetType.class,
      EFQUploadRecordGet::new
    );
  }

  @Override
  protected Optional<EFUploadRecord> onExecute(
    final EFDatabaseTransactionType transaction,
    final EFUploadID parameters)
    throws SQLException {
    var connection = transaction.connection();

    try (var st = connection.prepareStatement(QUERY)) {
      st.setLong(1, parameters.toLong());
      try (var rs = st.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRecord(rs));
        }
      }
    }

    return Optional.empty();
  }

  private static EFUploadRecord mapRecord(ResultSet rs) throws SQLException {
    return new EFUploadRecord(
      EFUploadID.Companion.of(rs.getLong("upload_record_id")),
      timeOf(rs.getLong("upload_record_time_start")),
      timeOfNullable(rs.getLong("upload_record_time_end")),
      new EFUploadName(rs.getString("upload_name")),
      new EFBucketReferenceName(rs.getString("upload_bucket")),
      rs.getString("upload_reason"),
      rs.getLong("upload_files_required"),
      rs.getLong("upload_files_skipped"),
      rs.getLong("upload_files_uploaded"),
      rs.getLong("upload_files_failed"),
      resultOf(rs.getString("upload_result"))
    );
  }

  private static EFUploadResult resultOf(
    String string) {
    if (string == null) {
      return null;
    }
    return EFUploadResult.valueOf(string);
  }

  private static OffsetDateTime timeOfNullable(
    long time) {
    if (time == 0L) {
      return null;
    }
    return timeOf(time);
  }

  private static OffsetDateTime timeOf(
    long time) {
    return OffsetDateTime.ofInstant(
      Instant.ofEpochMilli(time),
      UTC
    );
  }
}
