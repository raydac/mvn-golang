package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The Mojo wraps the 'fix' command.
 */
@Mojo(name = "fix", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangFixMojo extends AbstractPackageGolangMojo {
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "fix";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
