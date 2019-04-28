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
package com.igormaznitsa.mvngolang;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GolangBuildMojoTest {

  @Test
  public void testPackageTest_Multiple() throws Exception {
    final GolangBuildMojo buildMojo = new SpyGolangBuildMojo();
    buildMojo.setPackages(new String[]{"some.pack1", "some.pack2"});
    buildMojo.setResultFolder("some/folder");
    buildMojo.setResultName("targetName");
    assertTrue(buildMojo.getPackages().length > 1);
    // multiple packages should be install
    assertEquals("install", buildMojo.getGoCommand());
    assertThat(asList(buildMojo.getCommandFlags()), not(hasItem("-o")));
    assertThat(asList(buildMojo.getCommandFlags()), not(hasItem(endsWith("targetName"))));
    buildMojo.afterExecution(null, false);
  }

  @Test
  public void testPackageTest_Single() throws Exception {
    final GolangBuildMojo buildMojo = new SpyGolangBuildMojo();
    buildMojo.setPackages(new String[]{"some.pack1"});
    buildMojo.setResultFolder("some/folder");
    buildMojo.setResultName("targetName");
    assertEquals(1, buildMojo.getPackages().length);
    // single package should be build
    assertEquals("build", buildMojo.getGoCommand());
    assertThat(asList(buildMojo.getCommandFlags()), hasItem("-o"));
    assertThat(asList(buildMojo.getCommandFlags()), hasItem(endsWith("targetName")));
    buildMojo.afterExecution(null, false);
  }

}
