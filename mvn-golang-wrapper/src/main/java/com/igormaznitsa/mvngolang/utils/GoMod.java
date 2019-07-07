/*
 * Copyright 2019 Igor Maznitsa.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class GoMod {

  private static final Pattern TOKENIZER = Pattern.compile("(\\/\\/|\\\"[^\\\"]+\\\"|[\\w.\\-/]+|[<>=-]+|\\(|\\)|[\\s\\n]+)");

  @Nonnull
  private static String quoteIfHasSpace(@Nonnull final String str) {
    return str.contains(" ") ? '\"' + str + '\"' : str;
  }

  @Nonnull
  private static String ensureNoQuoting(@Nonnull final String text) {
    return text.startsWith("\"") ? text.substring(1, text.length() - 1) : text;
  }

  @Nonnull
  @MustNotContainNull
  private static List<ModuleInfo> extractModuleInfo(@Nonnull @MustNotContainNull final List<String> tokens, @Nonnull @MustNotContainNull final String... separators) {
    final List<ModuleInfo> result = new ArrayList<>();
    final List<String> tokenBuffer = new ArrayList<>(tokens);
    final List<String> accum = new ArrayList<>();

    while (!tokenBuffer.isEmpty()) {
      final String next = tokenBuffer.remove(0);
      boolean separator = false;
      for (final String s : separators) {
        if (s.equals(next)) {
          separator = true;
          break;
        }
      }
      if (separator) {
        switch (accum.size()) {
          case 1: {
            result.add(new ModuleInfo(accum.remove(0)));
          }
          break;
          case 2: {
            final String name = accum.remove(0);
            final String version = accum.remove(0);
            result.add(new ModuleInfo(name, version));
          }
          break;
          default:
            throw new IllegalArgumentException("Can't extract module info from tokens: " + tokens);
        }
      } else {
        accum.add(next);
      }
    }

    switch (accum.size()) {
      case 1: {
        result.add(new ModuleInfo(accum.remove(0)));
      }
      break;
      case 2: {
        final String name = accum.remove(0);
        final String version = accum.remove(0);
        result.add(new ModuleInfo(name, version));
      }
      break;
      default:
        throw new IllegalArgumentException("Can't extract module info from tokens: " + tokens);
    }

    return result;
  }

  @Nonnull
  public static GoMod from(@Nonnull final String str) {
    final List<GoModItem> foundItems = new ArrayList<>();

    final Matcher matcher = TOKENIZER.matcher(str);

    ParserState state = ParserState.FIND;

    boolean findEol = false;
    boolean bracket = false;

    final List<String> tokenList = new ArrayList<>();

    String customTokenName = null;

    while (matcher.find()) {
      final String token = matcher.group(1);
      if (findEol) {
        if (token.contains("\n")) {
          findEol = false;
          state = bracket ? state : ParserState.FIND;
        }
      } else {
        switch (state) {
          case FIND: {
            tokenList.clear();
            switch (token) {
              case "module": {
                state = ParserState.MODULE;
              }
              break;
              case "exclude": {
                state = ParserState.EXCLUDE;
              }
              break;
              case "replace": {
                state = ParserState.REPLACE;
              }
              break;
              case "require": {
                state = ParserState.REQUIRE;
              }
              break;
              default: {
                if ("//".equals(token)) {
                  findEol = true;
                } else if (!token.trim().isEmpty()) {
                  state = ParserState.CUSTOM;
                  customTokenName = token;
                }
              }
              break;
            }
          }
          break;
          case CUSTOM: {
            if ("//".equals(token)) {
              if (!bracket) {
                foundItems.add(new GoCustom(customTokenName, tokenList.toArray(new String[0])));
                foundItems.clear();
                customTokenName = null;
                state = ParserState.FIND;
              }
            } else {
              if ("(".equals(token)) {
                if (bracket) {
                  throw new IllegalArgumentException("Duplicated opening bracket in " + state);
                }
                bracket = true;
              } else if (")".equals(token)) {
                if (!bracket) {
                  throw new IllegalArgumentException("Unexpected closing bracket in " + state);
                }
                bracket = false;
                foundItems.add(new GoCustom(customTokenName, tokenList.toArray(new String[0])));
                foundItems.clear();
                customTokenName = null;
                state = ParserState.FIND;
              } else if (token.contains("\n")) {
                if (!bracket) {
                  state = ParserState.FIND;
                  foundItems.add(new GoCustom(customTokenName, tokenList.toArray(new String[0])));
                }
              } else {
                if (!token.trim().isEmpty()) {
                  tokenList.add(token);
                }
              }
            }
          }
          break;
          case MODULE:
          case EXCLUDE:
          case REPLACE:
          case REQUIRE: {
            if ("(".equals(token)) {
              if (bracket) {
                throw new IllegalArgumentException("Duplicated opening bracket in " + state);
              }
              if (!tokenList.isEmpty()) {
                throw new IllegalArgumentException("Unexpected tokens " + tokenList + " before bracket in " + state);
              }
              bracket = true;
            } else {
              final boolean processTokenList;
              ParserState nextState = state;

              if (")".equals(token)) {
                if (!bracket) {
                  throw new IllegalArgumentException("Unexpected closing bracket in " + state);
                }
                bracket = false;
                processTokenList = !tokenList.isEmpty();
                nextState = ParserState.FIND;
              } else if ("//".equals(token)) {
                findEol = true;
                processTokenList = bracket ? !tokenList.isEmpty() : true;
                nextState = bracket ? state : ParserState.FIND;
              } else if (token.contains("\n")) {
                processTokenList = bracket ? !tokenList.isEmpty() : true;
                nextState = bracket ? state : ParserState.FIND;
              } else {
                if (!token.trim().isEmpty()) {
                  tokenList.add(ensureNoQuoting(token));
                }
                processTokenList = false;
              }

              if (processTokenList) {
                switch (state) {
                  case MODULE: {
                    final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
                    tokenList.clear();
                    while (!moduleInfos.isEmpty()) {
                      foundItems.add(new GoModule(moduleInfos.remove(0)));
                    }
                  }
                  break;
                  case REQUIRE: {
                    final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
                    tokenList.clear();
                    while (!moduleInfos.isEmpty()) {
                      foundItems.add(new GoRequire(moduleInfos.remove(0)));
                    }
                  }
                  break;
                  case EXCLUDE: {
                    final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
                    tokenList.clear();
                    while (!moduleInfos.isEmpty()) {
                      foundItems.add(new GoExclude(moduleInfos.remove(0)));
                    }
                  }
                  break;
                  case REPLACE: {
                    final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList, "=>");
                    tokenList.clear();
                    while (!moduleInfos.isEmpty()) {
                      final ModuleInfo from = moduleInfos.remove(0);
                      if (moduleInfos.isEmpty()) {
                        throw new IllegalArgumentException("Can't find target in replace");
                      }
                      final ModuleInfo to = moduleInfos.remove(0);
                      foundItems.add(new GoReplace(from, to));
                    }
                  }
                  break;
                  default:
                    throw new Error("Unexpected: " + state);
                }
              }

              state = nextState;
            }
          }
          break;
        }
      }
    }

    if (!tokenList.isEmpty()) {
      switch (state) {
        case MODULE: {
          final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
          tokenList.clear();
          while (!moduleInfos.isEmpty()) {
            foundItems.add(new GoModule(moduleInfos.remove(0)));
          }
        }
        break;
        case REQUIRE: {
          final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
          tokenList.clear();
          while (!moduleInfos.isEmpty()) {
            foundItems.add(new GoRequire(moduleInfos.remove(0)));
          }
        }
        break;
        case EXCLUDE: {
          final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList);
          tokenList.clear();
          while (!moduleInfos.isEmpty()) {
            foundItems.add(new GoExclude(moduleInfos.remove(0)));
          }
        }
        break;
        case REPLACE: {
          final List<ModuleInfo> moduleInfos = extractModuleInfo(tokenList, "=>");
          tokenList.clear();
          while (!moduleInfos.isEmpty()) {
            final ModuleInfo from = moduleInfos.remove(0);
            if (moduleInfos.isEmpty()) {
              throw new IllegalArgumentException("Can't find target in replace");
            }
            final ModuleInfo to = moduleInfos.remove(0);
            foundItems.add(new GoReplace(from, to));
          }
        }
        break;
        case CUSTOM: {
          foundItems.add(new GoCustom(customTokenName, tokenList.toArray(new String[0])));
        }
        break;
      }
    }

    return new GoMod(foundItems);
  }
  private final List<GoModItem> items;

  private GoMod(@Nonnull @MustNotContainNull final List<GoModItem> items) {
    final List<GoModItem> newList = new ArrayList<>(items);
    Collections.sort(newList);
    this.items = newList;
  }

  @Nonnull
  public GoMod addItem(@Nonnull final GoModItem item) {
    this.items.add(item);
    Collections.sort(this.items);
    return this;
  }

  @Nonnull
  @MustNotContainNull
  public <T extends GoModItem> List<T> find(@Nonnull final Class<T> klass) {
    final List<T> result = new ArrayList<>();

    for (final GoModItem i : this.items) {
      if (klass == i.getClass()) {
        result.add(klass.cast(i));
      }
    }

    return result;
  }

  public int size() {
    return this.items.size();
  }

  @Nonnull
  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    for (final GoModItem i : this.items) {
      if (buffer.length() > 0) {
        buffer.append('\n');
      }
      buffer.append(i.toString());
    }
    return buffer.toString();
  }

  public static abstract class GoModItem implements Comparable<GoModItem> {

    @Override
    public int compareTo(@Nonnull final GoModItem that) {
      return Integer.compare(this.getPriority(), that.getPriority());
    }

    public abstract int getPriority();
  }

  public final static class ModuleInfo {

    private final String name;
    private final String version;

    public ModuleInfo(@Nonnull final String name) {
      this.name = Assertions.assertNotNull(name);
      this.version = null;
    }

    public ModuleInfo(@Nonnull final String name, @Nullable final String version) {
      this.name = Assertions.assertNotNull(name);
      this.version = version;
    }

    @Nonnull
    public String getName() {
      return this.name;
    }

    @Nullable
    public String getVersion() {
      return this.version;
    }

    @Nonnull
    @Override
    public String toString() {
      return quoteIfHasSpace(this.name) + (this.version == null ? "" : " " + quoteIfHasSpace(this.version));
    }
  }

  public static final class GoModule extends GoModItem {

    private final ModuleInfo moduleInfo;

    public GoModule(@Nonnull final ModuleInfo module) {
      this.moduleInfo = Assertions.assertNotNull(module);
    }

    @Nonnull
    public ModuleInfo getModuleInfo() {
      return this.moduleInfo;
    }

    @Override
    @Nonnull
    public String toString() {
      return "module " + this.moduleInfo;
    }

    @Override
    public int getPriority() {
      return 0;
    }
  }

  public static class GoRequire extends GoModItem {

    private final ModuleInfo moduleInfo;

    public GoRequire(@Nonnull final ModuleInfo moduleInfo) {
      this.moduleInfo = Assertions.assertNotNull(moduleInfo);
    }

    @Nonnull
    public ModuleInfo getModuleInfo() {
      return this.moduleInfo;
    }

    @Nonnull
    @Override
    public String toString() {
      return "require " + this.moduleInfo;
    }

    @Override
    public int getPriority() {
      return 1;
    }
  }

  public static final class GoCustom extends GoModItem {

    private final String name;
    private final String[] tokens;

    public GoCustom(@Nonnull final String name, @Nonnull @MustNotContainNull final String[] tokens) {
      this.name = Assertions.assertNotNull(name);
      this.tokens = tokens.clone();
    }

    @Nonnull
    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(name);
      for (final String t : tokens) {
        if (")".equals(t)) {
          result.append('\n');
        }
        result.append(' ').append(quoteIfHasSpace(t));
        if ("(".equals(t)) {
          result.append('\n');
        }
      }
      return result.toString();
    }

    @Override
    public int getPriority() {
      return 100;
    }
  }

  public static final class GoReplace extends GoModItem {

    private final ModuleInfo module;
    private final ModuleInfo replacement;

    public GoReplace(@Nonnull final ModuleInfo module, @Nonnull final ModuleInfo replacement) {
      this.module = Assertions.assertNotNull(module);
      this.replacement = Assertions.assertNotNull(replacement);
    }

    @Nonnull
    public ModuleInfo getModule() {
      return this.module;
    }

    @Nonnull
    public ModuleInfo getReplacement() {
      return this.replacement;
    }

    @Nonnull
    @Override
    public String toString() {
      return "replace " + this.module + " => " + this.replacement;
    }

    @Override
    public int getPriority() {
      return 2;
    }

  }

  public static final class GoExclude extends GoModItem {

    private final ModuleInfo module;

    public GoExclude(@Nonnull final ModuleInfo module) {
      this.module = Assertions.assertNotNull(module);
    }

    @Nonnull
    public ModuleInfo getModule() {
      return this.module;
    }

    @Nonnull
    @Override
    public String toString() {
      return "exclude " + this.module;
    }

    @Override
    public int getPriority() {
      return 3;
    }
  }

  private enum ParserState {
    FIND,
    MODULE,
    REQUIRE,
    REPLACE,
    EXCLUDE,
    CUSTOM
  }

}
