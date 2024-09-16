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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class Make
{
  private static final Logger LOG =
    Logger.getLogger(Make.class.getName());

  private static final String XSTRUCTURAL_VERSION =
    "1.9.0";

  private static final String XSTRUCTURAL_CHECKSUM =
    "be85c056bb3eb461a5a1322a86ce540e94dd64a061a60e6c62b7a99a1c649c48";

  private static final String XSTRUCTURAL_SOURCE =
    "https://repo1.maven.org/maven2/com/io7m/xstructural/com.io7m.xstructural.cmdline/%s/com.io7m.xstructural.cmdline-%s-main.jar"
      .formatted(XSTRUCTURAL_VERSION, XSTRUCTURAL_VERSION);

  private static final String SCANDO_VERSION =
    "1.0.0";

  private static final String SCANDO_CHECKSUM =
    "08fba5fc4bc3b5a49d205a4c38356dc8c7e01f4963adb661b67f9d2ed23751ae";

  private static final String SCANDO_SOURCE =
    "https://repo1.maven.org/maven2/com/io7m/scando/com.io7m.scando.cmdline/%s/com.io7m.scando.cmdline-%s-main.jar"
      .formatted(SCANDO_VERSION, SCANDO_VERSION);

  private static final String KTLINT_VERSION =
    "0.50.0";

  private static final String KTLINT_CHECKSUM =
    "c704fbc28305bb472511a1e98a7e0b014aa13378a571b716bbcf9d99d59a5092";

  private static final String KTLINT_SOURCE =
    "https://repo1.maven.org/maven2/com/pinterest/ktlint/%s/ktlint-%s-all.jar"
      .formatted(KTLINT_VERSION, KTLINT_VERSION);

  private static final List<String> KTLINT_ARGUMENTS =
    List.of(
      "*/src/**/*.kt",
      "*/build.gradle.kts",
      "build.gradle.kts",
      "!*/src/test/**"
    );

  private static final Properties PROJECT_PROPERTIES =
    new Properties();

  private static final Path DOCUMENTATION_SOURCES =
    Paths.get("com.io7m.exfilac.documentation")
      .resolve("src")
      .resolve("main")
      .resolve("resources")
      .resolve("com")
      .resolve("io7m")
      .resolve("exfilac")
      .resolve("documentation");

  private static final Path DOCUMENTATION_ASSETS_OUTPUT =
    Paths.get("com.io7m.exfilac.main")
      .resolve("src")
      .resolve("main")
      .resolve("assets")
      .resolve("manual");

  public static void main(
    final String[] ignored)
    throws Exception
  {
    openProperties();
    downloadScando();
    downloadKtlint();
    downloadXStructural();
    executeKtlint();

    copyDirectory(DOCUMENTATION_SOURCES, DOCUMENTATION_ASSETS_OUTPUT);
    executeXStructural();
    executeExtractPrivacyPolicy();

    if (!Objects.equals(System.getProperty("make.gradle"), "false")) {
      executeGradle();
    }
  }

  private static void executeExtractPrivacyPolicy()
    throws Exception
  {
    LOG.info("Extracting privacy policy link…");

    final var indexFile =
      DOCUMENTATION_ASSETS_OUTPUT.resolve("xstructural-index.xml");

    final var factory =
      DocumentBuilderFactory.newInstance();
    final var builder =
      factory.newDocumentBuilder();
    final var doc =
      builder.parse(indexFile.toString());
    final var xPathfactory =
      XPathFactory.newInstance();
    final var xpath =
      xPathfactory.newXPath();
    final var expr =
      xpath.compile("/Index/Item[@ID='6f66cea7-9131-42ad-9bfc-98a2d9676bc2']");
    final var nodes =
      (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

    if (nodes.getLength() != 1) {
      throw errorNoPrivacyPolicy();
    }

    final var element =
      (Element) nodes.item(0);
    final var file =
      element.getAttribute("File");

    LOG.info("Privacy policy: %s".formatted(file));

    final var privacyLink =
      DOCUMENTATION_ASSETS_OUTPUT.resolve("privacy-link.txt");

    Files.writeString(
      privacyLink,
      file.trim(),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }

  private static Exception errorNoPrivacyPolicy()
  {
    LOG.severe("Failed to locate the privacy policy.");
    return new IOException();
  }

  private static void executeGradle()
    throws Exception
  {
    LOG.info("Executing gradle…");

    final var args = new ArrayList<String>();
    args.add("./gradlew");
    args.add("clean");
    args.add("assemble");
    args.add("test");
    execute(args);
  }

  private static void copyDirectory(
    final Path source,
    final Path target)
    throws Exception
  {
    LOG.info("Copying %s to %s…".formatted(source, target));

    Files.createDirectories(target);

    final List<Path> sourceFiles;
    try (final var sourceStream = Files.walk(source)) {
      sourceFiles = sourceStream
        .map(source::relativize)
        .toList();
    }

    for (final var sourceName : sourceFiles) {
      final var targetFile = target.resolve(sourceName);
      final var sourceFile = source.resolve(sourceName);

      Files.createDirectories(targetFile.getParent());
      if (!Files.isRegularFile(sourceFile)) {
        continue;
      }

      LOG.info("Copy %s → %s".formatted(sourceFile, targetFile));
      Files.copy(sourceFile, targetFile, REPLACE_EXISTING);
    }
  }

  private static void downloadXStructural()
    throws Exception
  {
    LOG.info("Downloading xstructural…");

    downloadJar(
      "xstructural.jar",
      XSTRUCTURAL_CHECKSUM,
      XSTRUCTURAL_SOURCE
    );
  }

  private static void executeKtlint()
    throws Exception
  {
    LOG.info("Executing ktlint…");

    final var args = new ArrayList<String>();
    args.add("java");
    args.add("-jar");
    args.add("ktlint.jar");
    args.addAll(KTLINT_ARGUMENTS);
    execute(args);
  }

  private static void executeXStructural()
    throws Exception
  {
    LOG.info("Executing xstructural…");

    {
      final var args = new ArrayList<String>();
      args.add("java");
      args.add("-jar");
      args.add("xstructural.jar");
      args.add("xhtml");
      args.add("--outputDirectory");
      args.add(DOCUMENTATION_ASSETS_OUTPUT.toAbsolutePath().toString());
      args.add("--sourceFile");
      args.add(DOCUMENTATION_ASSETS_OUTPUT.resolve("documentation.xml").toString());
      args.add("--stylesheet");
      args.add("MULTIPLE_FILE");
      execute(args);
    }

    {
      final var args = new ArrayList<String>();
      args.add("java");
      args.add("-jar");
      args.add("xstructural.jar");
      args.add("xhtml");
      args.add("--outputDirectory");
      args.add(DOCUMENTATION_ASSETS_OUTPUT.toAbsolutePath().toString());
      args.add("--sourceFile");
      args.add(DOCUMENTATION_ASSETS_OUTPUT.resolve("documentation.xml").toString());
      args.add("--stylesheet");
      args.add("MULTIPLE_FILE_INDEX_ONLY");
      execute(args);
    }
  }

  private static void openProperties()
    throws Exception
  {
    try (final var stream =
           Files.newInputStream(Paths.get("gradle.properties"))) {
      PROJECT_PROPERTIES.load(stream);
    }
  }

  private static void execute(
    final List<String> args)
    throws Exception
  {
    final var process =
      new ProcessBuilder()
        .command(args)
        .inheritIO()
        .start();

    final var status = process.waitFor();
    if (status != 0) {
      throw errorCommand(args);
    }
  }

  private static void downloadKtlint()
    throws Exception
  {
    LOG.info("Downloading ktlint…");

    downloadJar(
      "ktlint.jar",
      KTLINT_CHECKSUM,
      KTLINT_SOURCE
    );
  }

  private static void downloadScando()
    throws Exception
  {
    LOG.info("Downloading scando…");

    downloadJar(
      "scando.jar",
      SCANDO_CHECKSUM,
      SCANDO_SOURCE
    );
  }

  private static void downloadJar(
    final String outputName,
    final String checksum,
    final String source)
    throws Exception
  {
    final var outputFile =
      Paths.get(outputName);

    if (Files.exists(outputFile)) {
      final var receivedHash = sha256Of(outputFile);
      if (Objects.equals(receivedHash, checksum)) {
        LOG.info("Local file %s already exists with the right checksum.".formatted(
          outputFile));
        return;
      }
    }

    LOG.info("Downloading %s".formatted(source));
    try (final var client = HttpClient.newHttpClient()) {
      final var request =
        HttpRequest.newBuilder(URI.create(source))
          .build();

      final var response =
        client.send(
          request,
          HttpResponse.BodyHandlers.ofFile(Paths.get(outputName))
        );

      if (response.statusCode() >= 400) {
        throw errorServerError(source, response.statusCode());
      }

      final var received = sha256Of(outputFile);
      if (!Objects.equals(received, checksum)) {
        throw errorUnexpectedSHA256(outputFile, checksum, received);
      }
    }
  }

  private static Exception errorCommand(
    final List<String> command)
  {
    LOG.severe("External command exited with a non-zero status code.");
    LOG.severe("  Command: %s".formatted(command));
    return new IOException();
  }

  private static Exception errorServerError(
    final String source,
    final int code)
  {
    LOG.severe("Server returned an unexpected HTTP status code.");
    LOG.severe("  URI:    %s".formatted(source));
    LOG.severe("  Status: %d".formatted(code));
    return new IOException();
  }

  private static Exception errorUnexpectedSHA256(
    final Path outputFile,
    final String expected,
    final String received)
  {
    LOG.severe("Unexpected SHA-256 checksum");
    LOG.severe("  File:     %s".formatted(outputFile));
    LOG.severe("  Expected: %s".formatted(expected));
    LOG.severe("  Received: %s".formatted(received));
    return new IOException();
  }

  private static String sha256Of(
    final Path file)
    throws Exception
  {
    final var digest =
      MessageDigest.getInstance("SHA-256");

    try (final var stream = Files.newInputStream(file)) {
      final var buffer = new byte[65536];
      while (true) {
        final var r = stream.read(buffer);
        if (r == -1) {
          break;
        }
        digest.update(buffer, 0, r);
      }
    }

    return HexFormat.of().formatHex(digest.digest());
  }
}
