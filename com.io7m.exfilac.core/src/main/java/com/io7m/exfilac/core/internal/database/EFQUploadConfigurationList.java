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
import com.io7m.exfilac.core.EFDeviceSource;
import com.io7m.exfilac.core.EFUploadConfiguration;
import com.io7m.exfilac.core.EFUploadName;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class EFQUploadConfigurationList
  extends EFDatabaseQueryAbstract<DDatabaseUnit, List<EFUploadConfiguration>>
  implements EFQUploadConfigurationListType {

  private static final String QUERY = """
    SELECT
      upload_configuration_id,
      upload_name,
      upload_device_source,
      bucket_ref_name,
      upload_policy
    FROM
      upload_configurations
    JOIN
      buckets
    ON buckets.bucket_id = upload_bucket_id
    ORDER BY upload_name ASC
    """;

  EFQUploadConfigurationList(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<DDatabaseUnit, List<EFUploadConfiguration>, EFQUploadConfigurationListType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadConfigurationListType.class,
      EFQUploadConfigurationList::new
    );
  }

  @Override
  protected List<EFUploadConfiguration> onExecute(
    final EFDatabaseTransactionType transaction,
    final DDatabaseUnit name)
    throws SQLException {
    var connection = transaction.connection();
    try {
      var results = new ArrayList<EFUploadConfiguration>();
      try (var st = connection.prepareStatement(QUERY)) {
        try (var rs = st.executeQuery()) {
          while (rs.next()) {
            results.add(
              new EFUploadConfiguration(
                new EFUploadName(rs.getString(2)),
                new EFDeviceSource(URI.create(rs.getString(3))),
                new EFBucketReferenceName(rs.getString(4)),
                EFUploadPolicies.INSTANCE.deserializeFromBytes(rs.getBytes(5))
              )
            );
          }
        }
      }
      return List.copyOf(results);
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }
}
