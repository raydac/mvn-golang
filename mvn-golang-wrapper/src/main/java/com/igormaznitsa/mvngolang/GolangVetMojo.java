package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * The Mojo wraps the 'vet' command.
 */
@Mojo(name = "vet", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangVetMojo extends AbstractPackageGolangMojo {
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "vet";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
