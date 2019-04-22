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
import static org.junit.Assert.*;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class GolangBuildMojoTest {

  private GolangBuildMojo makeBuildMojo() {
    final GolangBuildMojo buildMojo = new GolangBuildMojo();
    buildMojo.setPackages(new String[]{"some.pack1", "some.pack2"});
    buildMojo.setResultFolder("some/folder");
    buildMojo.setResultName("targetName");
    return buildMojo;
  }

  @Test
  public void testPackageTest_Multiple() throws Exception {
    final GolangBuildMojo buildMojo = makeBuildMojo();
    assertTrue(buildMojo.getPackages().length > 1);
    assertThat(asList(buildMojo.getCommandFlags()), not(hasItem("-o")));
    assertThat(asList(buildMojo.getCommandFlags()), not(hasItem(endsWith("targetName"))));
  }

  @Test
  public void testPackageTest_Single() throws Exception {
    final GolangBuildMojo buildMojo = makeBuildMojo();
    buildMojo.setPackages(copyOfRange(buildMojo.getPackages(), 0, 1));
    assertEquals(1, buildMojo.getPackages().length);
    assertThat(asList(buildMojo.getCommandFlags()), hasItem("-o"));
    assertThat(asList(buildMojo.getCommandFlags()), hasItem(endsWith("targetName")));
  }

}
