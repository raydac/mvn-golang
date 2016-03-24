package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The Mojo wraps the 'fmt' command.
 */
@Mojo(name = "fmt", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangFmtMojo extends AbstractPackageGolangMojo {
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "fmt";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
