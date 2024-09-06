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

import static com.io7m.ervilla.api.EPortProtocol.TCP;

import com.io7m.ervilla.api.EContainerSpec;
import com.io7m.ervilla.api.EContainerSupervisorType;
import com.io7m.ervilla.api.EContainerType;
import com.io7m.ervilla.api.EPortAddressType;
import com.io7m.ervilla.api.EPortPublish;
import com.io7m.exfilac.core.EFAccessKey;
import com.io7m.exfilac.core.EFBucketName;
import com.io7m.exfilac.core.EFSecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class EFMinIOFixture {
  private static final Logger LOG =
    LoggerFactory.getLogger(EFMinIOFixture.class);

  private final int port;
  private final int portAdmin;
  private final EContainerType container;

  private EFMinIOFixture(
    final EContainerType inContainer,
    final int inPort,
    final int inPortAdmin) {
    this.container =
      Objects.requireNonNull(inContainer, "container");

    this.port = inPort;
    this.portAdmin = inPortAdmin;
  }

  public static String rootUser() {
    return "minio";
  }

  public static String rootPassword() {
    return "12345678";
  }

  public static String defaultBucket() {
    return "bucket-0";
  }

  public static EFMinIOFixture create(
    final EContainerSupervisorType supervisor,
    final int port,
    final int portAdmin)
    throws Exception {
    final var spec =
      EContainerSpec.builder(
          "quay.io",
          "minio/minio",
          HTestProperties.MINIO_VERSION
        )
        .addPublishPort(new EPortPublish(
          new EPortAddressType.All(),
          port,
          port,
          TCP
        ))
        .addPublishPort(new EPortPublish(
          new EPortAddressType.All(),
          portAdmin,
          portAdmin,
          TCP
        ))
        .addArgument("server")
        .addArgument("/data")
        .addArgument("--console-address")
        .addArgument(":" + portAdmin)
        .addEnvironmentVariable("MINIO_DOMAIN", "localhost")
        .addEnvironmentVariable("MINIO_ROOT_USER", rootUser())
        .addEnvironmentVariable("MINIO_ROOT_PASSWORD", rootPassword())
        .setReadyCheck(new EFMinIOReadyCheck(new EPortAddressType.Address4("localhost"), port));

    return new EFMinIOFixture(
      supervisor.start(spec.build()),
      port,
      portAdmin
    );
  }

  public int port() {
    return this.port;
  }

  public void createBucket(
    final EFBucketName name)
    throws Exception {
    LOG.debug("Creating bucket {}.", name);

    this.container.executeAndWait(
      List.of(
        "mc",
        "mb",
        "test/" + name
      ),
      10L,
      TimeUnit.SECONDS
    );

    LOG.debug("Created bucket {}.", name);
  }

  public void createUser(
    final String username,
    final String password,
    final EFAccessKey accessKey,
    final EFSecretKey secretKey)
    throws Exception {
    LOG.debug("Creating user {}.", username);

    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "user",
        "add",
        "test",
        username,
        password
      ),
      10L,
      TimeUnit.SECONDS
    );

    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "user",
        "svcacct",
        "add",
        "--access-key",
        accessKey.getValue(),
        "--secret-key",
        secretKey.getValue(),
        "test",
        username
      ),
      10L,
      TimeUnit.SECONDS
    );

    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "policy",
        "attach",
        "test",
        "readwrite",
        "--user",
        username
      ),
      10L,
      TimeUnit.SECONDS
    );

    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "user",
        "info",
        "test",
        username
      ),
      10L,
      TimeUnit.SECONDS
    );

    LOG.debug("Created user {}.", username);
  }

  public void reset()
    throws Exception {
    LOG.debug("Resetting fixture.");

    this.container.executeAndWait(
      List.of(
        "mc",
        "alias",
        "set",
        "test",
        "http://localhost:9000",
        rootUser(),
        rootPassword()
      ),
      10L,
      TimeUnit.SECONDS
    );
  }

  public void attachUserPolicy(
    final String username,
    final String policy)
    throws Exception {
    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "policy",
        "attach",
        "test",
        policy,
        "--user",
        username
      ),
      10L,
      TimeUnit.SECONDS
    );
  }

  public void detachUserPolicy(
    final String username,
    final String policy)
    throws Exception {
    this.container.executeAndWait(
      List.of(
        "mc",
        "admin",
        "policy",
        "detach",
        "test",
        policy,
        "--user",
        username
      ),
      10L,
      TimeUnit.SECONDS
    );
  }

  public void deleteBucket(
    final EFBucketName name)
    throws Exception {
    LOG.debug("Deleting bucket {}.", name);

    this.container.executeAndWait(
      List.of(
        "mc",
        "rb",
        "test/" + name
      ),
      10L,
      TimeUnit.SECONDS
    );

    LOG.debug("Deleted bucket {}.", name);
  }
}
