package com.social100.todero.common.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogRedirector {

  private static final Path LOG_DIR = Paths.get(".");
  private static final long MAX_LOG_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
  private static final DateTimeFormatter ROTATION_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  public static void initialize() throws IOException {
    Files.createDirectories(LOG_DIR);

    Path stdoutPath = LOG_DIR.resolve("stdout.log");
    Path stderrPath = LOG_DIR.resolve("stderr.log");

    rotateIfTooBig(stdoutPath);
    rotateIfTooBig(stderrPath);

    System.setOut(new PrintStream(new FileOutputStream(stdoutPath.toFile(), true), true));
    System.setErr(new PrintStream(new FileOutputStream(stderrPath.toFile(), true), true));
  }

  private static void rotateIfTooBig(Path path) throws IOException {
    if (Files.exists(path) && Files.size(path) >= MAX_LOG_SIZE_BYTES) {
      String timestamp = LocalDateTime.now().format(ROTATION_FORMAT);
      Path rotated = Paths.get(path.toString() + "." + timestamp);
      Files.move(path, rotated, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
