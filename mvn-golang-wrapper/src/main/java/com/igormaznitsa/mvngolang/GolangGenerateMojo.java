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
 * The Mojo wraps the 'generate' command.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangGenerateMojo extends AbstractGolangMojo {

  /**
   * List of either filer or packages to be processed.
   */
  @Parameter(name = "filesOrPackages")
  private String[] filesOrPackages;

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCLITailArgs() {
    return GetUtils.ensureNonNull(this.filesOrPackages, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  public String getCommand() {
    return "generate";
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
