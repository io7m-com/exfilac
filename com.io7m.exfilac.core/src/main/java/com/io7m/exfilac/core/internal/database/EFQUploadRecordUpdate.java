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
import com.io7m.exfilac.core.EFBucketReferenceName;
import com.io7m.exfilac.core.EFUploadResult;
import com.io7m.exfilac.core.internal.EFUploadID;
import com.io7m.exfilac.core.internal.EFUploadRecord;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

public final class EFQUploadRecordUpdate
  extends EFDatabaseQueryAbstract<EFUploadRecord, DDatabaseUnit>
  implements EFQUploadRecordUpdateType {

  private static final String QUERY = """
    UPDATE upload_records
      SET upload_record_time_end = ?,
          upload_bucket          = ?,
          upload_files_required  = ?,
          upload_files_skipped   = ?,
          upload_files_uploaded  = ?,
          upload_files_failed    = ?,
          upload_result          = ?
      WHERE upload_record_id = ?
    """;

  EFQUploadRecordUpdate(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFUploadRecord, DDatabaseUnit, EFQUploadRecordUpdateType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadRecordUpdateType.class,
      EFQUploadRecordUpdate::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final EFUploadRecord parameters)
    throws SQLException {
    final var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setTimestamp(1, timestampOf(parameters.getTimeEnd()));
      st.setString(2, bucketOf(parameters.getBucket()));
      st.setLong(3, parameters.getFilesRequired());
      st.setLong(4, parameters.getFilesSkipped());
      st.setLong(5, parameters.getFilesUploaded());
      st.setLong(6, parameters.getFilesFailed());
      st.setString(7, resultOf(parameters.getResult()));
      st.setLong(8, parameters.getId().toLong());
      st.execute();
    }
    return DDatabaseUnit.UNIT;
  }

  private String resultOf(
    final EFUploadResult result) {
    if (result == null) {
      return null;
    }
    return result.name();
  }

  private String bucketOf(
    final EFBucketReferenceName bucket) {
    if (bucket == null) {
      return null;
    }
    return bucket.getValue();
  }

  private Timestamp timestampOf(
    final OffsetDateTime time) {
    if (time == null) {
      return null;
    }
    return new Timestamp(time.toInstant().toEpochMilli());
  }
}
