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

import com.io7m.darco.api.DDatabaseQueryProviderAbstract;
import com.io7m.darco.api.DDatabaseQueryType;

import java.util.function.Function;

final class EFDatabaseQueryProvider<P, R, Q extends DDatabaseQueryType<P, R>>
  extends DDatabaseQueryProviderAbstract<EFDatabaseTransactionType, P, R, Q>
  implements EFDatabaseQueryProviderType<P, R, Q> {
  private EFDatabaseQueryProvider(
    final Class<? extends Q> inQueryClass,
    final Function<EFDatabaseTransactionType, DDatabaseQueryType<P, R>> inConstructor) {
    super(inQueryClass, inConstructor);
  }

  static <P, R, Q extends DDatabaseQueryType<P, R>>
  EFDatabaseQueryProviderType<P, R, Q>
  provide(
    final Class<? extends Q> inQueryClass,
    final Function<EFDatabaseTransactionType, DDatabaseQueryType<P, R>> inConstructor) {
    return new EFDatabaseQueryProvider<>(inQueryClass, inConstructor);
  }
}
