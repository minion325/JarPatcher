package me.saif.jarpatcher;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarPatcher {
    private File mainFile;
    private List<File> addendumFiles = new ArrayList<>();

    public JarPatcher(File mainFile) {
        if (mainFile.isDirectory())
            throw new IllegalArgumentException("The file provided was found to be a directory.");

        if (!mainFile.getName().endsWith(".jar"))
            throw new IllegalArgumentException("The file provided is not a .jar file");

        this.mainFile = mainFile;
    }

    public void patch(File patchTo, JProgressBar progressBar) throws IOException {
        if (patchTo.exists())
            patchTo.delete();

        patchTo.createNewFile();

        int progress = 0;

        if (progressBar != null) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(this.addendumFiles.size() + 1);
        }

        List<File> addendumListCopyReversed = new ArrayList<>(this.addendumFiles);
        Collections.reverse(addendumListCopyReversed);

        Set<String> alreadyCopied = new HashSet<>();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(patchTo));
        ZipInputStream zipFile;
        for (File addendumFile : addendumListCopyReversed) {
            zipFile = new ZipInputStream(new FileInputStream(addendumFile));

            copyEntries(zipFile, zipOutputStream, alreadyCopied);

            if (progressBar != null)
                progressBar.setValue(++progress);

            zipFile.close();
        }

        zipFile = new ZipInputStream(new FileInputStream(mainFile));

        copyEntries(zipFile, zipOutputStream, alreadyCopied);
        if (progressBar != null)
            progressBar.setValue(++progress);
        zipFile.close();

        zipOutputStream.close();
    }

    public void patch(File file) throws IOException {
        this.patch(file, null);
    }

    public List<File> getAddendumFiles() {
        return addendumFiles;
    }

    public boolean isReady() {
        return this.mainFile != null && this.addendumFiles.size() > 0;
    }

    public File getMainFile() {
        return mainFile;
    }

    private void copyEntries(ZipInputStream zipInStream, ZipOutputStream zipOutStream, Set<String> entries) throws IOException {
        byte[] byteBuff = new byte[1024];
        for (ZipEntry entry; (entry = zipInStream.getNextEntry()) != null; ) {
            if (entries.contains(entry.getName()))
                continue;

            zipOutStream.putNextEntry(new ZipEntry(entry.getName()));
            for (int bytesRead; (bytesRead = zipInStream.read(byteBuff)) != -1; ) {
                zipOutStream.write(byteBuff, 0, bytesRead);
            }

            entries.add(entry.getName());
        }

    }

}
