package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;

/**
 * The Mojo wraps the 'install' command.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangInstallMojo extends AbstractGolangMojo {

  /**
   * List of packages to be built.
   */
  @Parameter(name = "packages")
  private String[] packages;

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCLITailArgs() {
    return GetUtils.ensureNonNull(this.packages, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  public String getCommand() {
    return "install";
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }
  
  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
