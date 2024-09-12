import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public final class UnpackSQLite {

  private static final Logger LOG =
    Logger.getLogger(UnpackSQLite.class.getName());

  public static void main(
    final String[] args)
    throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: version");
      throw new IllegalArgumentException();
    }

    final var version =
      args[0];
    final var source =
      "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/%s/sqlite-jdbc-%s.jar"
        .formatted(version, version);

    LOG.info("Retrieving: %s".formatted(source));

    final var file =
      Paths.get("sqlite.jar")
        .toAbsolutePath();

    final var http =
      HttpClient.newHttpClient();
    final var request =
      HttpRequest.newBuilder(URI.create(source))
        .GET()
        .build();

    final var response =
      http.send(request, HttpResponse.BodyHandlers.ofFile(file));

    if (response.statusCode() >= 400) {
      throw new IOException("Server: " + response.statusCode());
    }

    final var outBase =
      Paths.get("com.io7m.exfilac.main")
        .toAbsolutePath();
    final var jniBase =
      outBase.resolve("src").resolve("main").resolve("jniLibs");
    final var x64 =
      jniBase.resolve("x86_64");
    final var x86 =
      jniBase.resolve("x86");
    final var armeabi =
      jniBase.resolve("armeabi");
    final var arm64_v8a =
      jniBase.resolve("arm64-v8a");

    Files.createDirectories(x64);
    Files.createDirectories(x86);
    Files.createDirectories(armeabi);
    Files.createDirectories(arm64_v8a);

    try (var zip = new ZipFile(file.toFile())) {
      copy(
        zip,
        "org/sqlite/native/Linux-Android/x86_64/libsqlitejdbc.so",
        x64.resolve("libsqlitejdbc.so")
      );
      copy(
        zip,
        "org/sqlite/native/Linux-Android/x86/libsqlitejdbc.so",
        x86.resolve("libsqlitejdbc.so")
      );
      copy(
        zip,
        "org/sqlite/native/Linux-Android/arm/libsqlitejdbc.so",
        armeabi.resolve("libsqlitejdbc.so")
      );
      copy(
        zip,
        "org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so",
        arm64_v8a.resolve("libsqlitejdbc.so")
      );
    }
  }

  private static void copy(
    ZipFile zip,
    String zipName,
    Path output)
    throws IOException {
    LOG.info("Copying %s to %s".formatted(zipName, output));

    final var entry = zip.getEntry(zipName);
    if (entry == null) {
      throw new IOException("Zip has no entry named '%s'".formatted(zipName));
    }
    try (var stream = zip.getInputStream(entry)) {
      Files.copy(stream, output, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
