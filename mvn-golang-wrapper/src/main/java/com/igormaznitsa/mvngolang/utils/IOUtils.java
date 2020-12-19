/*
 * Copyright 2017 Igor Maznitsa.
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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.out;


import com.igormaznitsa.meta.annotation.MayContainNull;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Auxiliary class to collect methods for work with IO.
 *
 * @since 2.1.7
 */
public final class IOUtils {

  private IOUtils() {

  }

  /**
   * Print text progress bar.
   *
   * @param text             title of the bar
   * @param value            value to be rendered
   * @param maxValue         max value to be rendered
   * @param progressBarWidth width of bar
   * @param lastValue        value which was rendered last time, if the same then it will not be rendered
   * @return rendered value
   * @since 2.3.0
   */
  public static int printTextProgressBar(@Nonnull final String text, final long value,
                                         final long maxValue, final int progressBarWidth,
                                         final int lastValue) {
    final StringBuilder builder = new StringBuilder();
    builder.append("\r\u001B[?25l");
    builder.append(text);
    builder.append("[");

    final int progress = max(0, min(progressBarWidth,
        (int) Math.round(progressBarWidth * ((double) value / (double) maxValue))));

    for (int i = 0; i < progress; i++) {
      builder.append('▒');
    }
    for (int i = progress; i < progressBarWidth; i++) {
      builder.append('-');
    }
    builder.append("]\u001B[?25h");

    if (progress != lastValue) {
      out.print(builder.toString());
      out.flush();
    }

    return progress;
  }

  /**
   * Make file path appropriate for current OS.
   *
   * @param files files which will be added in the path
   * @return joined file path with OS file separator
   * @since 2.1.7
   */
  @Nonnull
  public static String makeOsFilePathWithoutDuplications(
      @Nonnull @MayContainNull final File[] files) {
    final StringBuilder result = new StringBuilder();
    final Set<File> alreadyAdded = new HashSet<>();

    for (final File f : files) {
      if (f == null || alreadyAdded.contains(f)) {
        continue;
      }
      alreadyAdded.add(f);
      if (result.length() > 0) {
        result.append(File.pathSeparatorChar);
      }
      result.append(f.getAbsolutePath());
    }

    return result.toString();
  }


  /**
   * Make file path from provided strings
   *
   * @param paths path elements
   * @return joined file path with OS file separator
   * @since 2.1.7
   */
  @Nonnull
  public static String makeOsFilePathWithoutDuplications(
      @Nonnull @MayContainNull final String... paths) {
    final StringBuilder result = new StringBuilder();
    final Set<String> alreadyAdded = new HashSet<>();

    for (final String s : paths) {
      if (s != null && !s.isEmpty() && !alreadyAdded.contains(s)) {
        alreadyAdded.add(s);
        if (result.length() > 0) {
          result.append(File.pathSeparatorChar);
        }
        result.append(s);
      }
    }
    return result.toString();
  }

  /**
   * Close a closeable object quietly, added because such method in APACHE-IO
   * has been deprecated
   *
   * @param closeable object to be closed
   */
  public static void closeSilently(@Nullable final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (final IOException ignoring) {
    }
  }
}
