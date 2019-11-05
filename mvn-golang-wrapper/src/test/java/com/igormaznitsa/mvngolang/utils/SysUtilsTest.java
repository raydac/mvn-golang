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

import org.junit.Test;
import static org.junit.Assert.*;

public class SysUtilsTest {
  
  @Test
  public void testDecodeGoSdkArchType() {
    assertNull(SysUtils.decodeGoSdkArchType("some xxx"));
    assertEquals("armv6l",SysUtils.decodeGoSdkArchType("some armv6l xxx"));
    assertEquals("ppc64le",SysUtils.decodeGoSdkArchType("ppc ppc64le"));
    assertEquals("ppc64le",SysUtils.decodeGoSdkArchType("ppc64le"));
    assertEquals("386",SysUtils.decodeGoSdkArchType("i386"));
    assertEquals("386",SysUtils.decodeGoSdkArchType("i686"));
    assertEquals("amd64",SysUtils.decodeGoSdkArchType("amd64"));
    assertEquals("s390x",SysUtils.decodeGoSdkArchType("s390"));
  }
  
}
