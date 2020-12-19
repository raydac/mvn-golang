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

import static org.junit.Assert.assertEquals;


import org.apache.maven.artifact.DefaultArtifact;
import org.junit.Test;

public class MavenUtilsTest {

  @Test
  public void testArtifactWithVersion() throws Exception {
    final DefaultArtifact artifact =
        new DefaultArtifact("com.igormaznitsa", "some-plugin-test", "1.0", "compile", "jar", null,
            new MvnGolangArtifactHandler());
    assertEquals(artifact, MavenUtils.parseArtifactRecord(MavenUtils.makeArtifactRecord(artifact),
        new MvnGolangArtifactHandler()));
  }

  @Test
  public void testArtifactWithVersionRange_TwoVersions() throws Exception {
    final DefaultArtifact artifact =
        new DefaultArtifact("com.igormaznitsa", "some-plugin-test", "1.0,2.3", "compile", "jar",
            null, new MvnGolangArtifactHandler());
    assertEquals(artifact, MavenUtils.parseArtifactRecord(MavenUtils.makeArtifactRecord(artifact),
        new MvnGolangArtifactHandler()));
  }

  @Test
  public void testArtifactWithVersionRange_RangeDiapasone() throws Exception {
    final DefaultArtifact artifact =
        new DefaultArtifact("com.igormaznitsa", "some-plugin-test", "(,1.0]", "compile", "jar",
            null, new MvnGolangArtifactHandler());
    assertEquals(artifact, MavenUtils.parseArtifactRecord(MavenUtils.makeArtifactRecord(artifact),
        new MvnGolangArtifactHandler()));
  }

  @Test
  public void testArtifactWithVersionRange_AllFieldsSet() throws Exception {
    final DefaultArtifact artifact =
        new DefaultArtifact("com.igormaznitsa", "some-plugin-test", "(,1.0]", "compile", "jar",
            "someclassifier", new MvnGolangArtifactHandler());
    assertEquals(artifact, MavenUtils.parseArtifactRecord(MavenUtils.makeArtifactRecord(artifact),
        new MvnGolangArtifactHandler()));
  }

}
