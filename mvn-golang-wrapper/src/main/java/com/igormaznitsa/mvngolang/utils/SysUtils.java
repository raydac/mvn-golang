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

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;

/**
 * Class contains some auxiliary methods.
 * @since 2.3.4
 */
public final class SysUtils {

  private SysUtils() {
  }

  @Nullable
  public static String findGoSdkOsType() {
    final String result;
    if (SystemUtils.IS_OS_WINDOWS) {
      result = "windows";
    } else if (SystemUtils.IS_OS_LINUX) {
      result = "linux";
    } else if (SystemUtils.IS_OS_FREE_BSD) {
      result = "freebsd";
    } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
      result = "darwin";
    } else {
      result = null;
    }
    return result;
  }
  
  @Nullable
  public static String decodeGoSdkArchType(@Nonnull final String osArchProperty) {
    final String arch = osArchProperty.toLowerCase(Locale.ENGLISH);
    if (arch.contains("ppc64le")) {
      return "ppc64le";
    } else if (arch.contains("armv6l")) {
      return "armv6l";
    } else if (arch.contains("arm64")) {
      return "arm64";
    } else if (arch.contains("s390")) {
      return "s390x";
    }
    if (arch.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
      return "386";
    } else if (arch.contains("em64t") 
            || arch.contains("x8664") 
            || arch.contains("ia32e") 
            || arch.contains("x64") 
            || arch.contains("amd64") 
            || arch.contains("x86_64")) {
      return "amd64";
    }
    return null;
  }

}
