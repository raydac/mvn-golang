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
 * The Mojo wraps the 'test' command.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class GolangTestMojo extends AbstractGolangMojo {

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
  
  /**
   * List of test binary flags.
   */
  @Parameter(name = "testBinaryFlags")
  private String[] testBinaryFlags;

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getAfterCLITailArgs() {
    return GetUtils.ensureNonNull(this.testBinaryFlags, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  public String getCommand() {
    return "test";
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
