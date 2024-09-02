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

package com.io7m.exfilac.core.internal;

import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.exfilac.core.EFBucketConfiguration;

import java.sql.SQLException;

public final class EFQBucketPut
  extends EFDatabaseQueryAbstract<EFBucketConfiguration, DDatabaseUnit>
  implements EFQBucketPutType {

  private static final String QUERY = """
    INSERT INTO buckets (
      bucket_name,
      bucket_region,
      bucket_access_key,
      bucket_secret,
      bucket_access_style,
      bucket_endpoint
    ) VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT DO UPDATE SET
        bucket_region       = ?,
        bucket_access_key   = ?,
        bucket_secret       = ?,
        bucket_access_style = ?,
        bucket_endpoint     = ?
    """;

  EFQBucketPut(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<EFBucketConfiguration, DDatabaseUnit, EFQBucketPutType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQBucketPutType.class,
      EFQBucketPut::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final EFBucketConfiguration bucket)
    throws SQLException {
    var connection = transaction.connection();
    try (var st = connection.prepareStatement(QUERY)) {
      st.setString(1, bucket.getName().getValue());
      st.setString(2, bucket.getRegion().getValue());
      st.setString(3, bucket.getAccessKey().getValue());
      st.setString(4, bucket.getSecret().getValue());
      st.setString(5, bucket.getAccessStyle().name());
      st.setString(6, bucket.getEndpoint().toString());

      st.setString(7, bucket.getRegion().getValue());
      st.setString(8, bucket.getAccessKey().getValue());
      st.setString(9, bucket.getSecret().getValue());
      st.setString(10, bucket.getAccessStyle().name());
      st.setString(11, bucket.getEndpoint().toString());
      st.execute();
    }
    return DDatabaseUnit.UNIT;
  }
}
