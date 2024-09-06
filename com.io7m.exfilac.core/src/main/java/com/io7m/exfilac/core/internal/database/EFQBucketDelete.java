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

import java.sql.SQLException;
import java.util.Set;

public final class EFQBucketDelete
  extends EFDatabaseQueryAbstract<Set<EFBucketReferenceName>, DDatabaseUnit>
  implements EFQBucketDeleteType {

  private static final String QUERY = """
    DELETE FROM buckets WHERE bucket_ref_name = ?
    """;

  EFQBucketDelete(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<Set<EFBucketReferenceName>, DDatabaseUnit, EFQBucketDeleteType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQBucketDeleteType.class,
      EFQBucketDelete::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final Set<EFBucketReferenceName> buckets)
    throws SQLException {
    var connection = transaction.connection();

    for (var bucket : buckets) {
      try (var st = connection.prepareStatement(QUERY)) {
        st.setString(1, bucket.getValue());
        st.execute();
      }
    }

    return DDatabaseUnit.UNIT;
  }
}
