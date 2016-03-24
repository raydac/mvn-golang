package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The Mojo wraps the 'install' command.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangInstallMojo extends AbstractPackageGolangMojo {

  @Override
  @Nonnull
  public String getGoCommand() {
    return "install";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
