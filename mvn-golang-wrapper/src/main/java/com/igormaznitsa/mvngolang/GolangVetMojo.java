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

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * The Mojo wraps the 'vet' command.
 */
@Mojo(name = "vet", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangVetMojo extends AbstractGoPackageAndDependencyAwareMojo {

    @Override
    @Nullable
    @MustNotContainNull
    protected String[] getDefaultPackages() {
        return new String[]{'.' + File.separator + "..."};
    }

    @Override
    @Nonnull
    public String getGoCommand() {
        return "vet";
    }

    @Override
    public boolean isEnforcePrintOutput() {
        return true;
    }

}
