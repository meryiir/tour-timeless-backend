package com.tourisme.service;

import com.tourisme.exception.PostgresDumpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PostgresDataDumpService {

    private static final String[] WINDOWS_PG_VERSIONS = {"18", "17", "16", "15", "14", "13", "12"};

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /** Executable name or full path; env PG_DUMP_PATH overrides in application.yml via ${PG_DUMP_PATH:pg_dump} */
    @Value("${app.backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    /**
     * Runs pg_dump --data-only (plain SQL). Tries configured path, then PATH, then common Windows install locations.
     */
    public byte[] dumpDataOnlyPlainSql(boolean useInserts) {
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new PostgresDumpException("PostgreSQL data export only works when spring.datasource.url is a jdbc:postgresql:// URL");
        }

        JdbcTarget target = parsePostgresqlUrl(jdbcUrl);
        List<String> candidates = buildExecutableCandidates(pgDumpPath);

        IOException lastIo = null;
        for (String exe : candidates) {
            try {
                return runPgDumpOnce(exe, target, useInserts);
            } catch (IOException e) {
                lastIo = e;
            }
        }

        String hint = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? " Example: C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe — or add PostgreSQL \"bin\" to your system PATH."
                : " Install postgresql-client or set app.backup.pg-dump-path to the full path of pg_dump.";
        throw new PostgresDumpException(
                "Could not run pg_dump. Tried: " + String.join(", ", candidates) + "." + hint
                        + (lastIo != null && lastIo.getMessage() != null ? " (" + lastIo.getMessage() + ")" : ""),
                lastIo
        );
    }

    private List<String> buildExecutableCandidates(String configured) {
        Set<String> ordered = new LinkedHashSet<>();
        String c = configured != null ? configured.trim() : "pg_dump";
        ordered.add(c);

        if (isWindows()) {
            String pf = System.getenv("ProgramFiles");
            String pf86 = System.getenv("ProgramFiles(x86)");
            for (String ver : WINDOWS_PG_VERSIONS) {
                if (pf != null) {
                    Path exePath = Path.of(pf, "PostgreSQL", ver, "bin", "pg_dump.exe");
                    if (Files.isRegularFile(exePath)) {
                        ordered.add(exePath.toAbsolutePath().normalize().toString());
                    }
                }
                if (pf86 != null) {
                    Path exePath86 = Path.of(pf86, "PostgreSQL", ver, "bin", "pg_dump.exe");
                    if (Files.isRegularFile(exePath86)) {
                        ordered.add(exePath86.toAbsolutePath().normalize().toString());
                    }
                }
            }
        }

        return new ArrayList<>(ordered);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private byte[] runPgDumpOnce(String executable, JdbcTarget target, boolean useInserts) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-h");
        command.add(target.host());
        command.add("-p");
        command.add(Integer.toString(target.port()));
        command.add("-U");
        command.add(username);
        command.add("-d");
        command.add(target.database());
        command.add("--data-only");
        command.add("--no-owner");
        command.add("--no-acl");
        command.add("-F");
        command.add("p");
        if (useInserts) {
            command.add("--inserts");
        }

        Path tmp = null;
        try {
            tmp = Files.createTempFile("tourisme-pg-data-", ".sql");
            Path tmpPath = tmp.toAbsolutePath();
            command.add("-f");
            command.add(tmpPath.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PGPASSWORD", password);

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new IOException("Failed to start '" + executable + "'", e);
            }

            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new PostgresDumpException("pg_dump timed out after 30 minutes");
            }

            int code = process.exitValue();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (code != 0) {
                throw new PostgresDumpException(
                        "pg_dump (" + executable + ") exited with code " + code + ". stderr: " + stderr.trim()
                );
            }

            if (!Files.exists(tmp) || Files.size(tmp) == 0) {
                throw new PostgresDumpException("pg_dump produced an empty file. stderr: " + stderr.trim());
            }

            return Files.readAllBytes(tmp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PostgresDumpException("pg_dump was interrupted", e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private record JdbcTarget(String host, int port, String database) {}

    static JdbcTarget parsePostgresqlUrl(String jdbcUrl) {
        String prefix = "jdbc:postgresql://";
        String rest = jdbcUrl.substring(prefix.length());
        int q = rest.indexOf('?');
        if (q >= 0) {
            rest = rest.substring(0, q);
        }
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) {
            throw new PostgresDumpException("Invalid JDBC URL: missing database name");
        }
        String hostPort = rest.substring(0, slash);
        String database = rest.substring(slash + 1);

        if (hostPort.startsWith("[")) {
            int endBracket = hostPort.indexOf(']');
            if (endBracket < 0) {
                throw new PostgresDumpException("Invalid JDBC URL: malformed IPv6 host");
            }
            String host = hostPort.substring(1, endBracket);
            int port = 5432;
            if (endBracket + 1 < hostPort.length() && hostPort.charAt(endBracket + 1) == ':') {
                port = Integer.parseInt(hostPort.substring(endBracket + 2));
            }
            return new JdbcTarget(host, port, database);
        }

        int colon = hostPort.lastIndexOf(':');
        if (colon > 0) {
            String host = hostPort.substring(0, colon);
            int port = Integer.parseInt(hostPort.substring(colon + 1));
            return new JdbcTarget(host, port, database);
        }

        return new JdbcTarget(hostPort, 5432, database);
    }
}
