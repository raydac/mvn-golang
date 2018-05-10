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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import javax.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class PackageListTest {

  private static final File FAKE_FILE = new File(".");

  private static final PackageList.ContentProvider STUB_CP = new PackageList.ContentProvider() {
    @Override
    @Nonnull
    public String readContent(@Nonnull final File file) throws IOException {
      throw new Error("Must not be called");
    }
  };

  @Test
  public void testRemoveQuotes() {
    assertEquals("", PackageList.removeQuotes(""));
    assertEquals("\"", PackageList.removeQuotes("\""));
    assertEquals("", PackageList.removeQuotes("\"\""));
    assertEquals("a", PackageList.removeQuotes("\"a\""));
    assertEquals("a\"", PackageList.removeQuotes("a\""));
    assertEquals("\"", PackageList.removeQuotes("\"\"\""));
  }
  
  @Test
  public void testRemoveComment_CheckQuotes_False() {
    assertEquals("", PackageList.removeComment("//",false));
    assertEquals("a", PackageList.removeComment("a//",false));
    assertEquals("a", PackageList.removeComment("a//b",false));
    assertEquals("a ", PackageList.removeComment("a //b",false));
    assertEquals("#include", PackageList.removeComment("#include//dddd",false));
    assertEquals("#include a/b/c", PackageList.removeComment("#include a/b/c//dddd",false));
    assertEquals("#include \"a/b/c", PackageList.removeComment("#include \"a/b/c//dddd\"",false));
    assertEquals("#include \"a/b/c", PackageList.removeComment("#include \"a/b/c//dddd\" // jjj",false));
    assertEquals("#include \"a/b/c\"", PackageList.removeComment("#include \"a/b/c\"//dddd\"",false));
  }
  
  @Test
  public void testRemoveComment_CheckQuotes_True() {
    assertEquals("", PackageList.removeComment("//",true));
    assertEquals("a", PackageList.removeComment("a//",true));
    assertEquals("a", PackageList.removeComment("a//b",true));
    assertEquals("a ", PackageList.removeComment("a //b",true));
    assertEquals("#include", PackageList.removeComment("#include//dddd",true));
    assertEquals("#include a/b/c", PackageList.removeComment("#include a/b/c//dddd",true));
    assertEquals("#include \"a/b/c//dddd\"", PackageList.removeComment("#include \"a/b/c//dddd\"",true));
    assertEquals("#include \"a/b/c//dddd\" ", PackageList.removeComment("#include \"a/b/c//dddd\" // kkkk",true));
    assertEquals("#include \"a/b/c\"", PackageList.removeComment("#include \"a/b/c\"//dddd\"",true));
  }
  
  @Test
  public void testEmptyText() throws Exception {
    assertEquals(0, new PackageList(FAKE_FILE, "", STUB_CP).getPackages().size());
  }

  @Test
  public void testOnlyCommentLine() throws Exception {
    assertEquals(0, new PackageList(FAKE_FILE, "// comment", STUB_CP).getPackages().size());
  }

  @Test
  public void testOnlyPackageName() throws Exception {
    final PackageList parsed = new PackageList(FAKE_FILE, "// text\npackage: github.com/gizak/termui", STUB_CP);
    assertEquals(1, parsed.getPackages().size());
    assertEquals("github.com/gizak/termui", parsed.getPackages().get(0).getPackage());
    assertNull(parsed.getPackages().get(0).getBranch());
    assertNull(parsed.getPackages().get(0).getRevision());
    assertNull(parsed.getPackages().get(0).getTag());
  }

  @Test
  public void testTwoPackages() throws Exception {
    final PackageList parsed = new PackageList(FAKE_FILE, "// text\n"
            + "package: github.com/gizak/termui // ,tag:mustbeignored\n"
            + "package: some/pack , branch:445566, tag:sometag, revision: r.33.3434342323", STUB_CP);
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

  @Test
  public void testInclude() throws Exception {
    final PackageList parsed = new PackageList(FAKE_FILE, "#include \"./another\"//testinclude\npackage: one\npackage: two", new PackageList.ContentProvider() {
      @Override
      @Nonnull
      public String readContent(@Nonnull final File pathElements) throws IOException {
        assertEquals("another", FilenameUtils.normalize(pathElements.getPath()));
        return "package: external"; 
      }
    });
    
    assertEquals(3,parsed.getPackages().size());
    assertEquals("external", parsed.getPackages().get(0).getPackage());
    assertEquals("one", parsed.getPackages().get(1).getPackage());
    assertEquals("two", parsed.getPackages().get(2).getPackage());
  }
  
  @Test(expected = ParseException.class)
  public void testWrongFormat_NoKey() throws Exception {
    new PackageList(FAKE_FILE, ":jjj", STUB_CP);
  }

  @Test(expected = ParseException.class)
  public void testWrongFormat_OnlyKey() throws Exception {
    new PackageList(FAKE_FILE, "package:", STUB_CP);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWrongFormat_WrongKey() throws Exception {
    new PackageList(FAKE_FILE, "packge: some", STUB_CP);
  }

  @Test(expected = ParseException.class)
  public void testWrongFormat_DoubleQuotes() throws Exception {
    new PackageList(FAKE_FILE, "package: some/pack , branch:445566, tag:sometag,, revision: r.33.3434342323", STUB_CP);
  }

}
