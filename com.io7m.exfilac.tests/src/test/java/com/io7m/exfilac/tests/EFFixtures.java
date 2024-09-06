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


package com.io7m.exfilac.tests;

import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.api.EPortAddressType;
import com.io7m.ervilla.api.EPortProtocol;
import com.io7m.ervilla.api.EPortPublish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EFFixtures
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EFFixtures.class);

  private static final Path BASE_DIRECTORY;
  private static final List<EPortPublish> PUBLICATION_PORTS;
  private static EFMinIOFixture MINIO_FIXTURE;

  static {
    try {
      BASE_DIRECTORY = Files.createTempDirectory("ophis");
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }

    PUBLICATION_PORTS =
      List.of(
        new EPortPublish(
          new EPortAddressType.All(),
          minioPort(),
          minioPort(),
          EPortProtocol.TCP
        ),
        new EPortPublish(
          new EPortAddressType.All(),
          minioAdminPort(),
          minioAdminPort(),
          EPortProtocol.TCP
        )
      );
  }

  public static int minioPort()
  {
    return 9000;
  }

  public static int minioAdminPort()
  {
    return 9001;
  }

  private EFFixtures()
  {

  }

  public static EFMinIOFixture minio(
    final EContainerSupervisorType containerFactory)
    throws Exception
  {
    if (MINIO_FIXTURE == null) {
      MINIO_FIXTURE = EFMinIOFixture.create(containerFactory, minioPort(), minioAdminPort());
    } else {
      LOG.info("Reusing MinIO fixture.");
    }
    return MINIO_FIXTURE;
  }
}
