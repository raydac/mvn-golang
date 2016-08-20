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

import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public final class WildCardMatcher {

  private final Pattern pattern;
  private final String addressPattern;

  public WildCardMatcher(@Nonnull final String txt) {
    this.addressPattern = txt.trim();
    final StringBuilder builder = new StringBuilder();
    for (final char c : this.addressPattern.toCharArray()) {
      switch (c) {
        case '*':
          builder.append(".*");
          break;
        case '?':
          builder.append('.');
          break;
        default: {
          final String code = Integer.toHexString(c).toUpperCase(Locale.ENGLISH);
          builder.append("\\u").append("0000".substring(0,4-code.length())).append(code);
        }
        break;
      }
    }
    this.pattern = Pattern.compile(builder.toString());
  }

  public boolean match(@Nonnull final String txt) {
    return this.pattern.matcher(txt).matches();
  }

  @Nonnull
  @Override
  public String toString() {
    return this.addressPattern;
  }
}
