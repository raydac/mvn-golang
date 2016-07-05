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

/**
 * Class container for proxy server parameter.
 *
 * @since 6.1.1
 */
public class ProxySettings {

  /**
   * Host.
   */
  public String host = "127.0.0.1";
  
  /**
   * Scheme.
   */
  public String scheme = "http";
  
  /**
   * Port.
   */
  public int port = 80;
  
  /**
   * Authentication name.
   */
  public String authName;
  
  /**
   * Authentication password.
   */
  public String authPassword = "";

  @Override
  @Nonnull
  public String toString() {
    return this.scheme + "://" + this.host + ":" + this.port;
  }
}
