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
import com.io7m.exfilac.core.EFUploadConfiguration;

import java.io.IOException;
import java.sql.SQLException;

public final class EFQUploadConfigurationPut
  extends EFDatabaseQueryAbstract<EFUploadConfiguration, DDatabaseUnit>
  implements EFQUploadConfigurationPutType {

  private static final String QUERY = """
    INSERT INTO upload_configurations (
      upload_name,
      upload_device_source,
      upload_bucket_id,
      upload_policy
    ) VALUES (
      ?,
      ?,
      (SELECT bucket_id FROM buckets WHERE bucket_ref_name = ?),
      ?
    ) ON CONFLICT DO UPDATE SET
      upload_device_source = ?,
      upload_bucket_id     = (SELECT bucket_id FROM buckets WHERE bucket_ref_name = ?),
      upload_policy        = ?
    """;

  EFQUploadConfigurationPut(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFUploadConfiguration, DDatabaseUnit, EFQUploadConfigurationPutType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadConfigurationPutType.class,
      EFQUploadConfigurationPut::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final EFUploadConfiguration upload)
    throws SQLException {
    var connection =
      transaction.connection();

    byte[] policyText;
    try {
      policyText = EFUploadPolicies.INSTANCE.serializeToBytes(upload.getPolicy());
    } catch (IOException e) {
      throw new SQLException(e);
    }

    try (var st = connection.prepareStatement(QUERY)) {
      st.setString(1, upload.getName().getValue());
      st.setString(2, upload.getSource().toString());
      st.setString(3, upload.getBucket().getValue());
      st.setBytes(4, policyText);

      st.setString(5, upload.getSource().toString());
      st.setString(6, upload.getBucket().getValue());
      st.setBytes(7, policyText);
      st.execute();
    }
    return DDatabaseUnit.UNIT;
  }
}
