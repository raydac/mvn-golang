package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The Mojo wraps the 'clean' command.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangCleanMojo extends AbstractPackageGolangMojo {
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "clean";
  }

}
