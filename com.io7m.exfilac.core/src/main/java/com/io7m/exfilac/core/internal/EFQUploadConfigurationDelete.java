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
import com.io7m.exfilac.core.EFUploadName;

import java.sql.SQLException;
import java.util.Set;

public final class EFQUploadConfigurationDelete
  extends EFDatabaseQueryAbstract<Set<EFUploadName>, DDatabaseUnit>
  implements EFQUploadConfigurationDeleteType {

  private static final String QUERY = """
    DELETE FROM upload_configurations WHERE upload_name = ?
    """;

  EFQUploadConfigurationDelete(
    final EFDatabaseTransactionType t) {
    super(t);
  }

  /**
   * @return The query provider
   */

  public static EFDatabaseQueryProviderType<Set<EFUploadName>, DDatabaseUnit, EFQUploadConfigurationDeleteType>
  provider() {
    return EFDatabaseQueryProvider.provide(
      EFQUploadConfigurationDeleteType.class,
      EFQUploadConfigurationDelete::new
    );
  }

  @Override
  protected DDatabaseUnit onExecute(
    final EFDatabaseTransactionType transaction,
    final Set<EFUploadName> uploads)
    throws SQLException {
    var connection = transaction.connection();

    for (var upload : uploads) {
      try (var st = connection.prepareStatement(QUERY)) {
        st.setString(1, upload.getValue());
        st.execute();
      }
    }

    return DDatabaseUnit.UNIT;
  }
}
