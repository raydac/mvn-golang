package com.igormaznitsa.mvngolang;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractModuleAware extends AbstractGoPackageAndDependencyAwareMojo {

  /**
   * Parameter allows define '-mod' parameter for command in format '-mod=value'.
   * Also can be defined through property 'mvn.golang.module.mod'
   * If the parameter is empty then it will not be added into command line and current default GoSDK value will be in use.
   *
   * @since 2.3.8
   */
  @Parameter(name = "mod")
  protected String mod = null;

  @Nullable
  public String getMod() {
    return findMvnProperty("mvn.golang.module.mod", this.mod);
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public final String[] getCommandFlags() {
    final List<String> result = new ArrayList<>();
    Collections.addAll(result, this.getAdditionalCommandFlags());

    final String moduleMod = this.getMod();
    if (moduleMod != null && moduleMod.trim().length() != 0) {
      if (result.stream().anyMatch(x -> x.startsWith("-mod="))) {
        this.getLog().warn("Detected direct 'mod' flag, configuration 'mod' option will be ignored");
      } else {
        this.getLog().debug("Detected mod value: " + moduleMod);
        result.add(0, String.format("-mod=%s", moduleMod.trim()));
      }
    }
    return result.toArray(new String[0]);
  }

  @Nonnull
  @MustNotContainNull
  protected String[] getAdditionalCommandFlags() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

}
