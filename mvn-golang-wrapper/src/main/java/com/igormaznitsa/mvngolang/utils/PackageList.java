/*
 * Copyright 2018 Igor Maznitsa.
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

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.Assertions;
import java.text.ParseException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Parser of package information.
 *
 * @since 2.1.9
 */
public final class PackageList {

  /**
   * Container of package information.
   */
  public static final class Package {

    private final String pkg;
    private final String branch;
    private final String tag;
    private final String revision;

    private static final String TAG_PACKAGE = "package";
    private static final String TAG_BRANCH = "branch";
    private static final String TAG_TAG = "tag";
    private static final String TAG_REVISION = "revision";

    private static final Set<String> ALLOWED_KEYS = new HashSet<>(asList(TAG_BRANCH, TAG_PACKAGE, TAG_REVISION, TAG_TAG));

    private final Pattern PATTERN = Pattern.compile("(?:\\s*([^:\\s]+)\\s*:\\s*([^,\\s]+)\\s*(?:,|$)?)|(.+?)", Pattern.CASE_INSENSITIVE);

    public Package(@Nonnull final String pkg, @Nullable final String branch, @Nullable final String tag, @Nullable final String revision) {
      this.pkg = Assertions.assertNotNull(pkg);
      this.branch = branch;
      this.revision = revision;
      this.tag = tag;
    }

    private Package(@Nonnull String textLine) throws ParseException {
      final int startComment = textLine.indexOf("//");
      if (startComment >= 0) {
        textLine = textLine.substring(0, startComment);
      }

      final Matcher matcher = PATTERN.matcher(textLine);

      final Map<String, String> map = new HashMap<>();

      while (matcher.find()) {
        final String unknown = matcher.group(3);
        if (unknown != null) {
          throw new ParseException(textLine, matcher.start(3));
        }

        final String name = matcher.group(1).trim().toLowerCase(Locale.ENGLISH);
        final String value = matcher.group(2).trim();

        if (!ALLOWED_KEYS.contains(name)) {
          throw new IllegalArgumentException("Unsupported key: " + name);
        }

        if (map.containsKey(name)) {
          throw new ParseException(textLine, matcher.start(1));
        }

        map.put(name, value);
      }

      if (!matcher.hitEnd()) {
        throw new ParseException(textLine, 0);
      }

      if (!map.containsKey(TAG_PACKAGE)) {
        throw new IllegalArgumentException("Can't find package name : " + textLine);
      }

      this.pkg = map.get(TAG_PACKAGE);
      if (this.pkg.isEmpty()) {
        throw new IllegalArgumentException("Empty package name : " + textLine);
      }

      this.branch = map.get(TAG_BRANCH);
      this.tag = map.get(TAG_TAG);
      this.revision = map.get(TAG_REVISION);
    }

    public boolean doesNeedCvsProcessing() {
      return this.branch != null || this.tag != null || this.revision != null;
    }

    @Nonnull
    public String getPackage() {
      return this.pkg;
    }

    @Nullable
    public String getBranch() {
      return this.branch;
    }

    @Nullable
    public String getTag() {
      return this.tag;
    }

    @Nullable
    public String getRevision() {
      return this.revision;
    }

    @Override
    @Nonnull
    public String toString() {
      return this.pkg;
    }
  }

  private final List<Package> packages;

  /**
   * Constructor parsing multi-line text. Each package must be described on new
   * string, comment started from '#' as the first line char
   *
   * @param text multi-line text
   * @throws ParseException thrown if format exception
   * @throws IllegalArgumentException if content problems
   */
  public PackageList(@Nonnull final String text) throws ParseException {
    final List<Package> list = new ArrayList<>();

    for (final String s : text.split("\\n")) {
      final String trimmed = s.trim();

      if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
        list.add(new Package(trimmed));
      }
    }

    this.packages = Collections.unmodifiableList(list);
  }

  @Nonnull
  @MustNotContainNull
  public List<Package> getPackages() {
    return this.packages;
  }
}
