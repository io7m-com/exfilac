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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EFQUploadRecordList
  extends EFDatabaseQueryAbstract<EFQUploadRecordListParameters, List<EFUploadRecord>>
  implements EFQUploadRecordListType {

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
      upload_record_time_start >= ?
    ORDER BY upload_record_time_start, upload_record_id ASC
    LIMIT ?
    """;

  private static final String QUERY_WITH_NAME_FILTER = """
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
      upload_record_time_start >= ? AND upload_name = ?
    ORDER BY upload_record_time_start, upload_record_id ASC
    LIMIT ?
    """;

  EFQUploadRecordList(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFQUploadRecordListParameters, List<EFUploadRecord>, EFQUploadRecordListType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadRecordListType.class,
      EFQUploadRecordList::new
    );
  }

  @Override
  protected List<EFUploadRecord> onExecute(
    final EFDatabaseTransactionType transaction,
    final EFQUploadRecordListParameters parameters)
    throws SQLException {
    var connection = transaction.connection();
    var results = new ArrayList<EFUploadRecord>();

    if (parameters.getOnlyIncludeForName() != null) {
      try (var st = connection.prepareStatement(QUERY_WITH_NAME_FILTER)) {
        st.setLong(1, parameters.getNewerThan().toInstant().toEpochMilli());
        st.setString(2, parameters.getOnlyIncludeForName().getValue());
        st.setInt(3, parameters.getLimit());
        readResults(st, results);
      }
    } else {
      try (var st = connection.prepareStatement(QUERY)) {
        st.setLong(1, parameters.getNewerThan().toInstant().toEpochMilli());
        st.setInt(2, parameters.getLimit());
        readResults(st, results);
      }
    }

    return List.copyOf(results);
  }

  private static void readResults(
    final PreparedStatement st,
    final ArrayList<EFUploadRecord> results)
    throws SQLException {
    try (var rs = st.executeQuery()) {
      while (rs.next()) {
        results.add(mapRecord(rs));
      }
    }
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
