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

import com.igormaznitsa.mvngolang.utils.MavenUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.annotation.Nonnull;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * The Mojo wraps the 'install' command.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangInstallMojo extends AbstractGoPackageAndDependencyAwareMojo {

  @Override
  public boolean isSkip() {
    return super.isSkip() 
            || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), "mvn.golang.install.skip", "false"))
            || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), "maven.deploy.skip", "false"));
  }
    
    @Override
    @Nonnull
    public String getGoCommand() {
        return "install";
    }

    @Override
    public boolean isSourceFolderRequired() {
        return true;
    }

    @Override
    public boolean isEnforcePrintOutput() {
        return true;
    }

}
