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

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class GoModModelTest {

  private void assertModules(final GoModModel model, final String... texts) {
    final List<GoModModel.GoModule> modules = model.find(GoModModel.GoModule.class);
    assertEquals(texts.length, modules.size());
    for (int i = 0; i < texts.length; i++) {
      assertEquals(texts[i], modules.get(i).getModule().getModule());
    }
  }

  @Test
  public void testComplex1() {
    final GoModModel model = GoModModel.from("module github.com/maruel/panicparse\n"
            + "\n"
            + "go 1.11\n"
            + "\n"
            + "require (\n"
            + "	github.com/mattn/go-colorable v0.1.1\n"
            + "	github.com/mattn/go-isatty v0.0.7\n"
            + "	github.com/mgutz/ansi v0.0.0-20170206155736-9520e82c474b\n"
            + ")");

    assertEquals(5, model.size());
  }

  @Test
  public void testComplex2() {
    final GoModModel model = GoModModel.from("module google.golang.org/api\n"
            + "\n"
            + "require (\n"
            + "	cloud.google.com/go v0.38.0 // indirect\n"
            + "	github.com/golang/protobuf v1.3.1 // indirect\n"
            + "	github.com/google/go-cmp v0.3.0\n"
            + "	github.com/hashicorp/golang-lru v0.5.1 // indirect\n"
            + "	go.opencensus.io v0.21.0\n"
            + "	golang.org/x/lint v0.0.0-20190409202823-959b441ac422\n"
            + "	golang.org/x/net v0.0.0-20190503192946-f4e77d36d62c // indirect\n"
            + "	golang.org/x/oauth2 v0.0.0-20190604053449-0f29369cfe45\n"
            + "	golang.org/x/sync v0.0.0-20190423024810-112230192c58\n"
            + "	golang.org/x/sys v0.0.0-20190507160741-ecd444e8653b\n"
            + "	golang.org/x/text v0.3.2 // indirect\n"
            + "	golang.org/x/tools v0.0.0-20190506145303-2d16b83fe98c\n"
            + "	google.golang.org/appengine v1.5.0\n"
            + "	google.golang.org/genproto v0.0.0-20190502173448-54afdca5d873\n"
            + "	google.golang.org/grpc v1.20.1\n"
            + "	honnef.co/go/tools v0.0.0-20190418001031-e561f6794a2a\n"
            + ")");

    assertEquals(17, model.size());
  }

  @Test
  public void testComplex3() {
    GoModModel model = GoModModel.from("module \"rsc.io/sampler\"\n"
            + "\n"
            + "require \"golang.org/x/text\" v0.0.0-20170915032832-14c0d48ead0c");
    assertEquals(2, model.size());
    model = GoModModel.from("module m\n"
            + "\n"
            + "go 1.11\n"
            + "\n"
            + "require (\n"
            + "	example.com/a v1.0.0\n"
            + "	example.com/d v1.0.0 // indirect\n"
            + ")\n"
            + "\n"
            + "replace example.com/a => example.com/c v1.0.0");
    assertEquals(5, model.size());
  }

  @Test
  public void testComplex4() {
    final GoModModel model = GoModModel.from("module example.com/me/hello\n"
            + "\n"
            + "    require (\n"
            + "     example.com/me/goodbye v0.0.0\n"
            + "     rsc.io/quote v1.5.2\n"
            + "    )\n"
            + "\n"
            + "    replace example.com/me/goodbye => ../goodbye");
    assertEquals(4, model.size());

    assertEquals("module example.com/me/hello\n"
            + "require example.com/me/goodbye v0.0.0\n"
            + "require rsc.io/quote v1.5.2\n"
            + "example.com/me/goodbye => ../goodbye", model.toString());
  }

  @Test
  public void testModule() {
    assertModules(GoModModel.from("module (example.com/hello) // huzzaa"), "example.com/hello");
    assertModules(GoModModel.from("module (\n"
            + "example.com/hello // some\n"
            + "  \"sss.gid/fsdsd\"   \n"
            + ") // huzzaa"), "example.com/hello", "sss.gid/fsdsd");
    assertModules(GoModModel.from("module (example.com/hello) // huzzaa"), "example.com/hello");
    assertModules(GoModModel.from("module example.com/hello"), "example.com/hello");
    assertModules(GoModModel.from("module example.com/hello\n"), "example.com/hello");
    assertModules(GoModModel.from("module example.com/hello // huzzaa"), "example.com/hello");
    assertModules(GoModModel.from("\n   \n    module \"example.com/hello\" // huzzaa"), "example.com/hello");
    assertModules(GoModModel.from("module \"example.com/hello\""), "example.com/hello");
    assertModules(GoModModel.from("module \"example.com/hello\"\n"), "example.com/hello");
    assertModules(GoModModel.from("module \"example.com/hello\" // huzzaa"), "example.com/hello");
  }

}
