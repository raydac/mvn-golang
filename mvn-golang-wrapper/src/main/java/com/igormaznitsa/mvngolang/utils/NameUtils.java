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

import javax.annotation.Nonnull;

public final class NameUtils {
  private NameUtils(){
  }

  @Nonnull
  public static String makePackageNameFromDependency(@Nonnull final String groupId, @Nonnull final String artifactId, @Nonnull final String version) {
    final StringBuilder builder = new StringBuilder();
    builder.append(groupId).append('/').append(artifactId.replace('.', '/'));
    if (!version.isEmpty()){
      builder.append('/').append(version);
    }
    return builder.toString();
  }
  
}
