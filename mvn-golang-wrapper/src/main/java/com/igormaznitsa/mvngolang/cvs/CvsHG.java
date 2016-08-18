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

import java.io.File;
import javax.annotation.Nonnull;
import org.apache.maven.plugin.logging.Log;

class CvsHG extends AbstractRepo {

  CvsHG() {
    super("hg");
  }

  @Override
  public boolean doesContainCVS(@Nonnull final File folder) {
    return new File(folder,".hg").isDirectory();
  }

  @Override
  public boolean upToBranch(@Nonnull final Log logger, @Nonnull final File cvsFolder, @Nonnull final String branchId) {
    return checkResult(logger, execute(logger, cvsFolder, "update", branchId));
  }

  @Override
  public boolean upToTag(@Nonnull final Log logger, @Nonnull final File cvsFolder, @Nonnull final String tagId) {
    return checkResult(logger, execute(logger, cvsFolder, "update", tagId));
  }

  @Override
  public boolean upToRevision(@Nonnull final Log logger, @Nonnull final File cvsFolder, @Nonnull final String revisionId) {
    return checkResult(logger, execute(logger, cvsFolder, "update", "-r",revisionId));
  }
  
}
