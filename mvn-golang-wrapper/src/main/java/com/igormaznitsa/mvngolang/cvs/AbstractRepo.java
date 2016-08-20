/*
 * Copyright 2016 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.mvngolang.cvs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.logging.Log;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;

public abstract class AbstractRepo {
  
  private final String command;
  
  public AbstractRepo(@Nonnull final String command){
    this.command = SystemUtils.IS_OS_WINDOWS ? command+".exe" : command;
  }
  
  @Nonnull
  public String getCommand(){
    return this.command;
  }
  
  public int execute(@Nullable String customCommand, @Nonnull final Log logger, @Nonnull final File cvsFolder, @Nonnull @MustNotContainNull final String... args) {
    final List<String> cli = new ArrayList<String>();
    cli.add(GetUtils.findFirstNonNull(customCommand,this.command));
    for (final String s : args) {
      cli.add(s);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Executing repo command : " + cli);
    }

    final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    final ProcessExecutor executor = new ProcessExecutor(cli);

    int result = -1;

    try {
      final ProcessResult processResult = executor.directory(cvsFolder).redirectError(errorStream).redirectOutput(outStream).executeNoTimeout();
      result = processResult.getExitValue();

      if (logger.isDebugEnabled()) {
        logger.debug("Exec.out.........................................");
        logger.debug(new String(errorStream.toByteArray(), Charset.defaultCharset()));
        logger.debug(".................................................");
      }

      if (result != 0) {
        logger.error(new String(errorStream.toByteArray(), Charset.defaultCharset()));
      }

    } catch (Exception ex) {
      logger.error("Unexpected error", ex);
    }

    return result;
  }

  protected boolean checkResult(@Nonnull final Log logger, final int code){
    return code == 0;
  }
  
  public abstract boolean doesContainCVS(@Nonnull File folder);
  public abstract boolean upToBranch(@Nonnull Log logger, @Nullable final ProxySettings proxy, @Nullable String customExe, @Nonnull File cvsFolder, @Nonnull String branchId);
  public abstract boolean upToTag(@Nonnull Log logger, @Nullable final ProxySettings proxy, @Nullable String customExe, @Nonnull File cvsFolder, @Nonnull String tagId);
  public abstract boolean upToRevision(@Nonnull Log logger, @Nullable final ProxySettings proxy, @Nullable String customExe, @Nonnull File cvsFolder, @Nonnull String revisionId);
}
