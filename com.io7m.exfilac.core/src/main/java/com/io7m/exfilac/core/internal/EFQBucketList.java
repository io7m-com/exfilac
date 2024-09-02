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
import com.io7m.exfilac.core.EFAccessKey;
import com.io7m.exfilac.core.EFBucketAccessStyle;
import com.io7m.exfilac.core.EFBucketConfiguration;
import com.io7m.exfilac.core.EFBucketName;
import com.io7m.exfilac.core.EFRegion;
import com.io7m.exfilac.core.EFSecretKey;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class EFQBucketList
  extends EFDatabaseQueryAbstract<DDatabaseUnit, List<EFBucketConfiguration>>
  implements EFQBucketListType {

  private static final String QUERY = """
    SELECT
      bucket_id,
      bucket_name,
      bucket_region,
      bucket_access_key,
      bucket_secret,
      bucket_access_style,
      bucket_endpoint
    FROM
      buckets
    ORDER BY bucket_name ASC
    """;

  EFQBucketList(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<DDatabaseUnit, List<EFBucketConfiguration>, EFQBucketListType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQBucketListType.class,
      EFQBucketList::new
    );
  }

  @Override
  protected List<EFBucketConfiguration> onExecute(
    final EFDatabaseTransactionType transaction,
    final DDatabaseUnit name)
    throws SQLException {
    var connection = transaction.connection();
    var results = new ArrayList<EFBucketConfiguration>();
    try (var st = connection.prepareStatement(QUERY)) {
      try (var rs = st.executeQuery()) {
        while (rs.next()) {
          results.add(
            new EFBucketConfiguration(
              new EFBucketName(rs.getString("bucket_name")),
              new EFRegion(rs.getString("bucket_region")),
              new EFAccessKey(rs.getString("bucket_access_key")),
              new EFSecretKey(rs.getString("bucket_secret")),
              EFBucketAccessStyle.valueOf(rs.getString("bucket_access_style")),
              URI.create(rs.getString("bucket_endpoint"))
            )
          );
        }
      }
    }
    return List.copyOf(results);
  }
}
