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
import org.apache.maven.settings.Proxy;

/**
 * Class container for proxy server parameter.
 *
 * @since 2.1.1
 */
public class ProxySettings {

  /**
   * The proxy host.
   */
  public String host = "127.0.0.1";
  
  /**
   * The proxy protocol.
   */
  public String protocol = "http";
  
  /**
   * The proxy port.
   */
  public int port = 80;
  
  /**
   * The proxy user.
   */
  public String username;
  
  /**
   * The proxy password.
   */
  public String password = "";

  /**
   * The list of non-proxied hosts (delimited by |).
   */
  public String nonProxyHosts;
  
  public ProxySettings(){
  }

  public ProxySettings(@Nonnull final Proxy mavenProxy){
    this.protocol = mavenProxy.getProtocol();
    this.host = mavenProxy.getHost();
    this.port = mavenProxy.getPort();
    this.username = mavenProxy.getUsername();
    this.password = mavenProxy.getPassword();
    this.nonProxyHosts = mavenProxy.getNonProxyHosts();
  }

  public boolean hasCredentials(){
    return this.username!=null && this.password!=null;
  }
  
  @Override
  @Nonnull
  public String toString() {
    return this.protocol + "://" + this.host + ":" + this.port;
  }
}
