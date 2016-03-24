package com.igormaznitsa.mvngolang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;

/**
 * The Mojo wraps the 'test' command.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class GolangTestMojo extends AbstractPackageGolangMojo {

  /**
   * List of test binary flags.
   */
  @Parameter(name = "testFlags")
  private String[] testFlags;

  @Nullable
  @MustNotContainNull
  public String [] getTestFlags(){
    return this.testFlags;
  }
  
  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getOptionalExtraTailArguments() {
    return GetUtils.ensureNonNull(this.testFlags, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "test";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
