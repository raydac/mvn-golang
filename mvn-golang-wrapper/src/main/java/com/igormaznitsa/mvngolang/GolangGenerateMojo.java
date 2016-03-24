package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * The Mojo wraps the 'generate' command.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangGenerateMojo extends AbstractPackageGolangMojo {

  @Override
  @Nonnull
  public String getGoCommand() {
    return "generate";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }
 
}
