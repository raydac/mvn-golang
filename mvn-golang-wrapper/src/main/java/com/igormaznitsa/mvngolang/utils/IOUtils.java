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

import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Auxiliary class to collect methods for work with IO.
 * @since 2.1.7
 */
public final class IOUtils {
  private IOUtils() {
    
  }
  
  /**
   * Close a closeable object quietly, added because such method in APACHE-IO has been deprecated
   * @param closeable object to be closed
   */
  public static void closeSilently(@Nullable final Closeable closeable) {
    try{
      if (closeable!=null) closeable.close();
    }catch(final IOException ex) {
    }
  }
}
