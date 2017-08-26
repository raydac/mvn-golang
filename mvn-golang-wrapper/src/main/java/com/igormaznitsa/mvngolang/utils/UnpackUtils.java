/*
 * Copyright 2016 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.mvngolang.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public final class UnpackUtils {

    private static final ArchiveStreamFactory ARCHIVE_STREAM_FACTORY = new ArchiveStreamFactory();

    private UnpackUtils() {
    }

    public static int unpackFileToFolder(@Nonnull final Log logger, @Nullable final String folder, @Nonnull final File archiveFile, @Nonnull final File destinationFolder, final boolean makeAllExecutable) throws IOException {
        final String normalizedName = archiveFile.getName().toLowerCase(Locale.ENGLISH);

        final ArchEntryGetter entryGetter;

        boolean modeZipFile = false;

        final ZipFile theZipFile;
        final ArchiveInputStream archInputStream;
        if (normalizedName.endsWith(".zip")) {
            logger.debug("Detected ZIP archive");

            modeZipFile = true;

            theZipFile = new ZipFile(archiveFile);
            archInputStream = null;
            entryGetter = new ArchEntryGetter() {
                private final Enumeration<ZipArchiveEntry> iterator = theZipFile.getEntries();

                @Override
                @Nullable
                public ArchiveEntry getNextEntry() throws IOException {
                    ArchiveEntry result = null;
                    if (this.iterator.hasMoreElements()) {
                        result = this.iterator.nextElement();
                    }
                    return result;
                }
            };
        } else {
            theZipFile = null;
            final InputStream in = new BufferedInputStream(new FileInputStream(archiveFile));
            try {
                if (normalizedName.endsWith(".tar.gz")) {
                    logger.debug("Detected TAR.GZ archive");
                    archInputStream = new TarArchiveInputStream(new GZIPInputStream(in));

                    entryGetter = new ArchEntryGetter() {
                        @Override
                        @Nullable
                        public ArchiveEntry getNextEntry() throws IOException {
                            return ((TarArchiveInputStream) archInputStream).getNextTarEntry();
                        }
                    };

                } else {
                    logger.debug("Detected OTHER archive");
                    archInputStream = ARCHIVE_STREAM_FACTORY.createArchiveInputStream(in);
                    logger.debug("Created archive stream : " + archInputStream.getClass().getName());

                    entryGetter = new ArchEntryGetter() {
                        @Override
                        @Nullable
                        public ArchiveEntry getNextEntry() throws IOException {
                            return archInputStream.getNextEntry();
                        }
                    };
                }

            } catch (ArchiveException ex) {
                IOUtils.closeQuietly(in);
                throw new IOException("Can't recognize or read archive file : " + archiveFile, ex);
            } catch (CantReadArchiveEntryException ex) {
                IOUtils.closeQuietly(in);
                throw new IOException("Can't read entry from archive file : " + archiveFile, ex);
            }
        }

        try {

            final String normalizedFolder = folder == null ? null : FilenameUtils.normalize(folder, true) + '/';

            int unpackedFilesCounter = 0;
            while (true) {
                final ArchiveEntry entry = entryGetter.getNextEntry();
                if (entry == null) {
                    break;
                }
                final String normalizedPath = FilenameUtils.normalize(entry.getName(), true);

                logger.debug("Detected archive entry : " + normalizedPath);

                if (normalizedFolder == null || normalizedPath.startsWith(normalizedFolder)) {
                    final File targetFile = new File(destinationFolder, normalizedFolder == null ? normalizedPath : normalizedPath.substring(normalizedFolder.length()));
                    if (entry.isDirectory()) {
                        logger.debug("Folder : " + normalizedPath);
                        if (!targetFile.exists() && !targetFile.mkdirs()) {
                            throw new IOException("Can't create folder " + targetFile);
                        }
                    } else {
                        final File parent = targetFile.getParentFile();

                        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Can't create folder : " + parent);
                        }

                        try (final FileOutputStream fos = new FileOutputStream(targetFile)) {
                            if (modeZipFile) {
                                logger.debug("Unpacking ZIP entry : " + normalizedPath);

                                final InputStream zipEntryInStream = theZipFile.getInputStream((ZipArchiveEntry) entry);
                                try {
                                    if (IOUtils.copy(zipEntryInStream, fos) != entry.getSize()) {
                                        throw new IOException("Can't unpack file, illegal unpacked length : " + entry.getName());
                                    }
                                } finally {
                                    IOUtils.closeQuietly(zipEntryInStream);
                                }
                            } else {
                                logger.debug("Unpacking archive entry : " + normalizedPath);

                                if (!archInputStream.canReadEntryData(entry)) {
                                    throw new IOException("Can't read archive entry data : " + normalizedPath);
                                }
                                if (IOUtils.copy(archInputStream, fos) != entry.getSize()) {
                                    throw new IOException("Can't unpack file, illegal unpacked length : " + entry.getName());
                                }
                            }
                        }

                        if (makeAllExecutable) {
                            try {
                                targetFile.setExecutable(true, true);
                            } catch (SecurityException ex) {
                                throw new IOException("Can't make file executable : " + targetFile, ex);
                            }
                        }
                        unpackedFilesCounter++;
                    }
                } else {
                    logger.debug("Archive entry " + normalizedPath + " ignored");
                }
            }
            return unpackedFilesCounter;
        } finally {
            IOUtils.closeQuietly(theZipFile);
            IOUtils.closeQuietly(archInputStream);
        }
    }

    private interface ArchEntryGetter {

        @Nullable
        ArchiveEntry getNextEntry() throws IOException;
    }

    public static class CantReadArchiveEntryException extends RuntimeException {

        private static final long serialVersionUID = 1989670574345144082L;

        public CantReadArchiveEntryException(@Nullable final Throwable cause) {
            super("Can't read archive entry for exception", cause);
        }
    }

}
