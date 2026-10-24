package id.kalanusantara.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class BackupSha256VerificationTest {

    private String calculateSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean verifyBackupZip(File zipFile) {
        if (!zipFile.exists() || zipFile.length() == 0) return false;
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry dbEntry = zf.getEntry("kala_data.db");
            ZipEntry manifestEntry = zf.getEntry("manifest.json");
            if (dbEntry == null || manifestEntry == null) {
                return false;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = zf.getInputStream(dbEntry)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String actualDbSha256 = sb.toString();

            String manifestJson;
            try (InputStream is = zf.getInputStream(manifestEntry);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, read);
                }
                manifestJson = baos.toString(StandardCharsets.UTF_8);
            }

            String expectedSha256 = null;
            for (String line : manifestJson.split("\n")) {
                line = line.trim();
                if (line.contains("\"database_sha256\"")) {
                    int colon = line.indexOf(':');
                    if (colon != -1) {
                        expectedSha256 = line.substring(colon + 1).replace("\"", "").replace(",", "").trim();
                        break;
                    }
                }
            }

            return expectedSha256 != null && expectedSha256.equalsIgnoreCase(actualDbSha256);
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void testValidBackupVerification(@TempDir Path tempDir) throws Exception {
        File zipFile = tempDir.resolve("valid_backup.zip").toFile();
        byte[] dbBytes = "SQLite format 3\u0000sample database content for testing".getBytes(StandardCharsets.UTF_8);
        String expectedHash = calculateSha256(dbBytes);

        String manifestContent = String.format("""
            {
              "server": "Kala Nusantara RPG",
              "version": "0.1.0",
              "database_sha256": "%s"
            }
            """, expectedHash);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("kala_data.db"));
            zos.write(dbBytes);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(manifestContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertTrue(verifyBackupZip(zipFile), "Backup dengan hash valid harus terverifikasi sukses");
    }

    @Test
    public void testMismatchedSha256FailsVerification(@TempDir Path tempDir) throws Exception {
        File zipFile = tempDir.resolve("corrupt_backup.zip").toFile();
        byte[] dbBytes = "Tampered database data".getBytes(StandardCharsets.UTF_8);
        String fakeHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        String manifestContent = String.format("""
            {
              "server": "Kala Nusantara RPG",
              "version": "0.1.0",
              "database_sha256": "%s"
            }
            """, fakeHash);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("kala_data.db"));
            zos.write(dbBytes);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(manifestContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertFalse(verifyBackupZip(zipFile), "Backup dengan hash tidak cocok harus gagal verifikasi");
    }

    @Test
    public void testMissingManifestFailsVerification(@TempDir Path tempDir) throws Exception {
        File zipFile = tempDir.resolve("incomplete_backup.zip").toFile();
        byte[] dbBytes = "Incomplete database data".getBytes(StandardCharsets.UTF_8);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("kala_data.db"));
            zos.write(dbBytes);
            zos.closeEntry();
        }

        assertFalse(verifyBackupZip(zipFile), "Backup tanpa manifest.json harus ditolak");
    }
}
