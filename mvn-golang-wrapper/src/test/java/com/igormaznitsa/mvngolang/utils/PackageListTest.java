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

import java.text.ParseException;
import org.junit.Test;
import static org.junit.Assert.*;

public class PackageListTest {
  
  @Test
  public void testEmptyText() throws Exception {
    assertEquals(0, new PackageList("").getPackages().size());
  }
  
  @Test
  public void testOnlyCommentLine() throws Exception {
    assertEquals(0, new PackageList("// comment").getPackages().size());
  }
  
  @Test
  public void testOnlyPackageName() throws Exception {
    final PackageList parsed = new PackageList("// text\npackage: github.com/gizak/termui");
    assertEquals(1, parsed.getPackages().size());
    assertEquals("github.com/gizak/termui", parsed.getPackages().get(0).getPackage());
    assertNull(parsed.getPackages().get(0).getBranch());
    assertNull(parsed.getPackages().get(0).getRevision());
    assertNull(parsed.getPackages().get(0).getTag());
  }
  
  @Test
  public void testTwoPackages() throws Exception {
    final PackageList parsed = new PackageList("// text\n"
            + "package: github.com/gizak/termui // ,tag:mustbeignored\n"
            + "package: some/pack , branch:445566, tag:sometag, revision: r.33.3434342323");
    assertEquals(2, parsed.getPackages().size());

    assertEquals("github.com/gizak/termui", parsed.getPackages().get(0).getPackage());
    assertNull(parsed.getPackages().get(0).getBranch());
    assertNull(parsed.getPackages().get(0).getRevision());
    assertNull(parsed.getPackages().get(0).getTag());

    assertEquals("some/pack", parsed.getPackages().get(1).getPackage());
    assertEquals("445566", parsed.getPackages().get(1).getBranch());
    assertEquals("sometag", parsed.getPackages().get(1).getTag());
    assertEquals("r.33.3434342323", parsed.getPackages().get(1).getRevision());
  }
  
  @Test (expected = ParseException.class)
  public void testWrongFormat_NoKey() throws Exception {
    new PackageList(":jjj");
  }
  
  @Test (expected = ParseException.class)
  public void testWrongFormat_OnlyKey() throws Exception {
    new PackageList("package:");
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testWrongFormat_WrongKey() throws Exception {
    new PackageList("packge: some");
  }
  
  @Test (expected = ParseException.class)
  public void testWrongFormat_DoubleQuotes() throws Exception {
    new PackageList("package: some/pack , branch:445566, tag:sometag,, revision: r.33.3434342323");
  }
  
}
