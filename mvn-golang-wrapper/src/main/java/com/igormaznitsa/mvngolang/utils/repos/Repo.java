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
package com.igormaznitsa.mvngolang.utils.repos;

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

public enum Repo {
  UNKNOWN("none"),
  GIT("git"),
  HG("hg"),
  SVN("svn");

  private final String executableBaseName;

  private Repo(@Nonnull final String executableBaseName) {
    this.executableBaseName = executableBaseName;
  }

  public boolean changeBranchAndTag(@Nonnull final File folder, @Nonnull final Log logger, @Nullable final String branchName, @Nullable final String tagName){
    boolean result = false;
    final List<String> args = new ArrayList<String>();
    switch(this){
      case SVN : {
        args.add("switch");
        if (branchName!=null){
          args.add(branchName);
        }
        if (tagName!=null){
          args.add("--revision");
          args.add(tagName);
        }
        result = execute(folder, logger, args.toArray(new String[args.size()])) == 0;
      }break;
      case GIT :  {
        args.add("checkout");
        boolean ok = true;
        if (branchName != null) {
          args.add(branchName);
          result = execute(folder, logger, args.toArray(new String[args.size()])) == 0;
          ok = result;
        }
        if (ok && tagName!=null){
          args.add("tags/"+tagName);
          result = execute(folder, logger, args.toArray(new String[args.size()])) == 0;
        }
      }break;
      case HG : {
        if (branchName!=null){
          args.add("update");
          args.add("-r");
          args.add(branchName);
          result = execute(folder, logger, args.toArray(new String[args.size()])) == 0;
          if (result && tagName!=null){
            args.clear();
            args.add("update");
            args.add(tagName);
            result = execute(folder, logger, args.toArray(new String[args.size()])) == 0;
          }
        }
      }break;
      default: {
        logger.error("Changing of branch and tag is unsupported for CVS : "+this.name());
      }break;
    }
    
    return result;
  }
  
  /**
   * Execute CVS command with parameters.
   * @param folder the home folder
   * @param log the logger
   * @param args list of arguments
   * @return exit code, 0 if all is ok
   */
  public int execute(@Nonnull final File folder, @Nonnull final Log log, @Nonnull @MustNotContainNull final String... args) {
    final String exeName = SystemUtils.IS_OS_WINDOWS ? this.executableBaseName + ".exe" : this.executableBaseName;
    final List<String> cli = new ArrayList<String>();
    cli.add(exeName);
    for (final String s : args) {
      cli.add(s);
    }

    if (log.isDebugEnabled()) {
      log.debug("Executing repo command : " + cli);
    }

    final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    final ProcessExecutor executor = new ProcessExecutor(cli);
    
    int result = -1;
    
    try{
      final ProcessResult processResult = executor.directory(folder).redirectError(errorStream).redirectOutput(outStream).executeNoTimeout();
      result = processResult.getExitValue();

      if (log.isDebugEnabled()){
        log.debug("Exec.out.........................................");
        log.debug(new String(errorStream.toByteArray(), Charset.defaultCharset()));
        log.debug(".................................................");
      }
      
      if (result!=0){
        log.error(new String(errorStream.toByteArray(), Charset.defaultCharset()));
      }
      
    }catch(Exception ex){
      log.error("Unexpected error",ex);
    }
    
    return result;
  }

  @Nonnull
  public static Repo investigateFolder(@Nullable final File folder) {
    Repo result = UNKNOWN;

    if (folder!=null && folder.isDirectory()) {
      if (new File(folder, ".git").isDirectory()) {
        result = GIT;
      } else if (new File(folder, ".hg").isDirectory()) {
        result = HG;
      } else if (new File(folder, ".svn").isDirectory()) {
        result = SVN;
      }
    }

    return result;
  }
}
