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
package com.igormaznitsa.mvngolang;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;
import static org.junit.Assert.*;

public class GolangMojoCfgTest extends AbstractMojoTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testGolangCleanMojoConfiguration() throws Exception {
    final GolangCleanMojo cleanMojo = (GolangCleanMojo) lookupMojo("clean", new File(GolangMojoCfgTest.class.getResource("mojoClean.xml").toURI()));
    assertNull(cleanMojo.getTargetArch());
    assertNull(cleanMojo.getTargetOS());
    assertFalse(cleanMojo.isUseEnvVars());
    assertEquals("clean",cleanMojo.getGoCommand());
    assertFalse(cleanMojo.isVerbose());
    assertFalse(cleanMojo.isHideBanner());
    assertEquals("some/sources", cleanMojo.getSources(false).getPath());
    assertEquals("some/root", cleanMojo.getGoRoot());
    assertEquals("some/path", cleanMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},cleanMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},cleanMojo.getBuildFlags());
  }

  @Test
  public void testGolangFixMojoConfiguration() throws Exception {
    final GolangFixMojo fixMojo = (GolangFixMojo) lookupMojo("fix", new File(GolangMojoCfgTest.class.getResource("mojoFix.xml").toURI()));
    assertFalse(fixMojo.isUseEnvVars());
    assertEquals("fix",fixMojo.getGoCommand());
    assertFalse(fixMojo.isVerbose());
    assertFalse(fixMojo.isHideBanner());
    assertEquals("some/sources", fixMojo.getSources(false).getPath());
    assertEquals("some/root", fixMojo.getGoRoot());
    assertEquals("some/path", fixMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},fixMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},fixMojo.getBuildFlags());
  }

  @Test
  public void testGolangFmtMojoConfiguration() throws Exception {
    final GolangFmtMojo fmtMojo = (GolangFmtMojo) lookupMojo("fmt", new File(GolangMojoCfgTest.class.getResource("mojoFmt.xml").toURI()));
    assertFalse(fmtMojo.isUseEnvVars());
    assertEquals("fmt",fmtMojo.getGoCommand());
    assertFalse(fmtMojo.isVerbose());
    assertFalse(fmtMojo.isHideBanner());
    assertEquals("some/sources", fmtMojo.getSources(false).getPath());
    assertEquals("some/root", fmtMojo.getGoRoot());
    assertEquals("some/path", fmtMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},fmtMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},fmtMojo.getBuildFlags());
  }

  @Test
  public void testGolangGenerateMojoConfiguration() throws Exception {
    final GolangGenerateMojo genMojo = (GolangGenerateMojo) lookupMojo("generate", new File(GolangMojoCfgTest.class.getResource("mojoGenerate.xml").toURI()));
    assertFalse(genMojo.isUseEnvVars());
    assertEquals("generate",genMojo.getGoCommand());
    assertFalse(genMojo.isVerbose());
    assertFalse(genMojo.isHideBanner());
    assertEquals("some/sources", genMojo.getSources(false).getPath());
    assertEquals("some/root", genMojo.getGoRoot());
    assertEquals("some/path", genMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},genMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},genMojo.getBuildFlags());
  }

  @Test
  public void testGolangInstallMojoConfiguration() throws Exception {
    final GolangInstallMojo instMojo = (GolangInstallMojo) lookupMojo("install", new File(GolangMojoCfgTest.class.getResource("mojoInstall.xml").toURI()));
    assertFalse(instMojo.isUseEnvVars());
    assertEquals("install",instMojo.getGoCommand());
    assertFalse(instMojo.isVerbose());
    assertFalse(instMojo.isHideBanner());
    assertEquals("some/sources", instMojo.getSources(false).getPath());
    assertEquals("some/root", instMojo.getGoRoot());
    assertEquals("some/path", instMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},instMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},instMojo.getBuildFlags());
  }

  @Test
  public void testGolangVetMojoConfiguration() throws Exception {
    final GolangVetMojo vetMojo = (GolangVetMojo) lookupMojo("vet", new File(GolangMojoCfgTest.class.getResource("mojoVet.xml").toURI()));
    assertFalse(vetMojo.isUseEnvVars());
    assertEquals("vet",vetMojo.getGoCommand());
    assertFalse(vetMojo.isVerbose());
    assertFalse(vetMojo.isHideBanner());
    assertEquals("some/sources", vetMojo.getSources(false).getPath());
    assertEquals("some/root", vetMojo.getGoRoot());
    assertEquals("some/path", vetMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},vetMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},vetMojo.getBuildFlags());
  }

  @Test
  public void testGolangTestMojoConfiguration() throws Exception {
    final GolangTestMojo instMojo = (GolangTestMojo) lookupMojo("test", new File(GolangMojoCfgTest.class.getResource("mojoTest.xml").toURI()));
    assertFalse(instMojo.isUseEnvVars());
    assertEquals("test",instMojo.getGoCommand());
    assertFalse(instMojo.isVerbose());
    assertFalse(instMojo.isHideBanner());
    assertEquals("some/sources", instMojo.getSources(false).getPath());
    assertEquals("some/root", instMojo.getGoRoot());
    assertEquals("some/path", instMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},instMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},instMojo.getBuildFlags());
    assertArrayEquals(new String[]{"binFlag1","binFlag2"},instMojo.getTestFlags());
  }

  @Test
  public void testGolangToolMojoConfiguration() throws Exception {
    final GolangToolMojo toolMojo = (GolangToolMojo) lookupMojo("tool", new File(GolangMojoCfgTest.class.getResource("mojoTool.xml").toURI()));
    assertFalse(toolMojo.isUseEnvVars());
    assertEquals("tool",toolMojo.getGoCommand());
    assertFalse(toolMojo.isVerbose());
    assertFalse(toolMojo.isHideBanner());
    assertEquals("some/sources", toolMojo.getSources(false).getPath());
    assertEquals("theCommand", toolMojo.getCommand());
    assertEquals("some/root", toolMojo.getGoRoot());
    assertEquals("some/path", toolMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"theCommand","arg1","arg2"},toolMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},toolMojo.getBuildFlags());
    assertArrayEquals(new String[]{"arg1","arg2"},toolMojo.getArgs());
  }

  @Test
  public void testGolangGetMojoConfiguration() throws Exception {
    final GolangGetMojo getMojo = (GolangGetMojo) lookupMojo("get", new File(GolangMojoCfgTest.class.getResource("mojoGet.xml").toURI()));
    assertFalse(getMojo.isUseEnvVars());
    assertEquals("get",getMojo.getGoCommand());
    assertNull(getMojo.getUseGoTool());
    assertTrue(getMojo.isAutoFixGitCache());
    assertFalse(getMojo.isVerbose());
    assertFalse(getMojo.isHideBanner());
    assertEquals("some/sources", getMojo.getSources(false).getPath());
    assertEquals("some/root", getMojo.getGoRoot());
    assertEquals("some/path", getMojo.findGoPath(false).getPath());
    assertArrayEquals(new String[]{"one_pack","two_pack"},getMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},getMojo.getBuildFlags());
  }

  @Test
  public void testGolangBuildMojoConfiguration() throws Exception {
    final GolangBuildMojo buildMojo = (GolangBuildMojo) lookupMojo("build", new File(GolangMojoCfgTest.class.getResource("mojoBuild.xml").toURI()));
    assertEquals("somearch",buildMojo.getTargetArch());
    assertEquals("someos",buildMojo.getTargetOS());
    assertFalse(buildMojo.isUseEnvVars());
    assertEquals("build", buildMojo.getGoCommand());
    assertEquals("someGo.bat",buildMojo.getUseGoTool());
    assertFalse(buildMojo.isVerbose());
    assertTrue(buildMojo.isHideBanner());
    assertEquals("some/sources", buildMojo.getSources(false).getPath());
    assertEquals("some/root", buildMojo.getGoRoot());
    assertEquals("some/path", buildMojo.findGoPath(false).getPath());
    assertEquals("target/place", buildMojo.getTarget());
    assertEquals("targetName", buildMojo.getName());
    assertArrayEquals(new String[]{"one_pack","two_pack"},buildMojo.getTailArguments());
    assertArrayEquals(new String[]{"flag1","flag2"},buildMojo.getBuildFlags());
    assertEquals(1,buildMojo.getEnv().size());
    assertEquals("somevalue",buildMojo.getEnv().get("somekey"));
  }
}
