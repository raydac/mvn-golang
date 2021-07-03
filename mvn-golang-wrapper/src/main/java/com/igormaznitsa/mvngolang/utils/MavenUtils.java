/*
 * Copyright 2019 Igor Maznitsa.
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

package com.igormaznitsa.mvngolang.utils;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.AbstractGolangMojo;
import com.igormaznitsa.mvngolang.GolangMvnInstallMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auxiliary methods to work with maven entities.
 *
 * @since 2.3.0
 */
public final class MavenUtils {

  private static final Pattern ARTIFACT_RECORD_PATTERN =
      Pattern.compile("^([^:]+)::([^:]+)::([^:]*)::([^:]*)::([^:]*)::([^:]*)$");

  private MavenUtils() {

  }

  /**
   * Check that execution in a test mode.
   *
   * @param execution maven execution object, must not be null
   * @return true if a test mode is active, false otherwise
   */
  public static boolean isTestPhase(@Nonnull final MojoExecution execution) {
    final String phase = execution.getLifecyclePhase();
    return phase != null && (phase.equals("test") || phase.equals("process-test-resources") ||
        phase.equals("test-compile"));
  }

  /**
   * Make resolve artifact project building request.
   *
   * @param session            maven session, must not be null
   * @param remoteRepositories list of remote repositories, must not be null and
   *                           can't contain null
   * @return created request, must not be null
   */
  @Nonnull
  public static ProjectBuildingRequest makeResolveArtifactProjectBuildingRequest(
      @Nonnull final MavenSession session,
      @Nonnull @MustNotContainNull final List<ArtifactRepository> remoteRepositories
  ) {
    final ProjectBuildingRequest result =
        new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    result.setRemoteRepositories(remoteRepositories);
    result.setLocalRepository(session.getLocalRepository());
    return result;
  }

  /**
   * Parse string containing artifact record
   *
   * @param record  string containing record, must not be null
   * @param handler artifact handler for created artifact, must not be null
   * @return new created artifact from the record, must not be null
   * @throws InvalidVersionSpecificationException it will be thrown if version
   *                                              format is wrong
   * @throws IllegalArgumentException             it will be thrown if record can't be
   *                                              recognized as artifact record
   */
  @Nonnull
  public static Artifact parseArtifactRecord(
      @Nonnull final String record,
      @Nonnull final ArtifactHandler handler
  ) throws InvalidVersionSpecificationException {
    final Matcher matcher = ARTIFACT_RECORD_PATTERN.matcher(record.trim());
    if (matcher.find()) {
      return new DefaultArtifact(
          matcher.group(1),
          matcher.group(2),
          VersionRange.createFromVersion(matcher.group(3)),
          matcher.group(4).isEmpty() ? null : matcher.group(4),
          matcher.group(5).isEmpty() ? null : matcher.group(5),
          matcher.group(6).isEmpty() ? null : matcher.group(6),
          handler);
    }
    throw new IllegalArgumentException("Can't recognize record as artifact: " + record);
  }

  /**
   * Make artifact record from a maven artifact
   *
   * @param artifact artifact to be converted into string, must not be null
   * @return string representation of artifact, must not be null
   * @see #parseArtifactRecord(java.lang.String,
   * org.apache.maven.artifact.handler.ArtifactHandler)
   */
  @Nonnull
  public static String makeArtifactRecord(@Nonnull final Artifact artifact) {
    return artifact.getGroupId() +
        "::" + artifact.getArtifactId() +
        "::" + artifact.getVersionRange().toString() +
        "::" + GetUtils.ensureNonNull(artifact.getScope(), "compile") +
        "::" + GetUtils.ensureNonNull(artifact.getType(), "zip") +
        "::" + GetUtils.ensureNonNull(artifact.getClassifier(), "");
  }

  /**
   * Scan project dependencies to find artifacts generated by mvn golang
   * project.
   *
   * @param mavenProject            maven project, must not be null
   * @param ignoreNonResolvedArtifact   if true then missing artifact will be ignored, otherwise exception will be thrown
   * @param includeTestDependencies flag to process dependencies marked for test
   *                                phases
   * @param mojo                    calling mojo, must not be null
   * @param session                 maven session, must not be null
   * @param execution               maven execution, must not be null
   * @param resolver                artifact resolver, must not be null
   * @param remoteRepositories      list of remote repositories, must not be null
   * @return list of files found in artifacts generated by mvn golang plugin
   * @throws ArtifactResolverException exception thrown if some artifact can't
   *                                   be resolved
   */
  @Nonnull
  @MustNotContainNull
  public static List<Tuple<Artifact, File>> scanForMvnGoArtifacts(
          @Nonnull final MavenProject mavenProject,
          final boolean ignoreNonResolvedArtifact,
          final boolean includeTestDependencies,
          @Nonnull final AbstractMojo mojo,
          @Nonnull final MavenSession session,
          @Nonnull final MojoExecution execution,
          @Nonnull final ArtifactResolver resolver,
          @Nonnull @MustNotContainNull final List<ArtifactRepository> remoteRepositories
  ) throws ArtifactResolverException {
    final List<Tuple<Artifact, File>> result = new ArrayList<>();
//    final String phase = execution.getLifecyclePhase();

    final Set<String> alreadyFoundArtifactRecords = new HashSet<>();

    MavenProject currentProject = mavenProject;
    while (currentProject != null && !Thread.currentThread().isInterrupted()) {
      final Set<Artifact> projectDependencies = currentProject.getDependencyArtifacts();
      final List<Artifact> artifacts = new ArrayList<>(
          projectDependencies == null ? Collections.emptySet() : projectDependencies);
      mojo.getLog().debug("Detected dependency artifacts: " + artifacts);

      while (!artifacts.isEmpty() && !Thread.currentThread().isInterrupted()) {
        final Artifact artifact = artifacts.remove(0);

        if (Artifact.SCOPE_TEST.equals(artifact.getScope()) && !includeTestDependencies) {
          continue;
        }

        if (artifact.getType().equals(AbstractGolangMojo.GOARTIFACT_PACKAGING)) {
          final ArtifactResult artifactResult;
          try {
            artifactResult = resolver.resolveArtifact(
                    makeResolveArtifactProjectBuildingRequest(session, remoteRepositories), artifact);
          } catch (ArtifactResolverException ex) {
            if (ignoreNonResolvedArtifact) {
              mojo.getLog().debug("Can't resolve artifact: " + artifact, ex);
              continue;
            } else {
              throw ex;
            }
          }
          final File zipFilePath = artifactResult.getArtifact().getFile();

          mojo.getLog().debug(
                  "Detected MVN-GOLANG marker inside ZIP dependency: " + artifact.getGroupId() + ':' +
                          artifact.getArtifactId() + ':' + artifact.getVersion() + ':' +
                          artifact.getType());

          if (ZipUtil
                  .containsEntry(zipFilePath, GolangMvnInstallMojo.MVNGOLANG_DEPENDENCIES_FILE)) {
            final byte[] artifactFlagFile = ZipUtil
                .unpackEntry(zipFilePath, GolangMvnInstallMojo.MVNGOLANG_DEPENDENCIES_FILE,
                    StandardCharsets.UTF_8);

            for (final String str : new String(artifactFlagFile, StandardCharsets.UTF_8)
                .split("\\R")) {
              if (str.trim().isEmpty() || alreadyFoundArtifactRecords.contains(str)) {
                continue;
              }
              mojo.getLog().debug("Adding mvn-golang dependency: " + str);
              alreadyFoundArtifactRecords.add(str);
              try {
                artifacts.add(parseArtifactRecord(str, new MvnGolangArtifactHandler()));
              } catch (InvalidVersionSpecificationException ex) {
                throw new ArtifactResolverException("Can't make artifact: " + str, ex);
              }
            }
          }

          final File artifactFile = artifactResult.getArtifact().getFile();
          mojo.getLog().debug("Artifact file: " + artifactFile);
          if (doesContainFile(result, artifactFile)) {
            mojo.getLog().debug("Artifact file ignored as duplication: " + artifactFile);
          } else {
            result.add(Tuple.of(artifact, artifactFile));
          }
        }
      }
      currentProject = currentProject.hasParent() ? currentProject.getParent() : null;
    }

    return result;
  }

  private static boolean doesContainFile(
      @Nonnull @MustNotContainNull final List<Tuple<Artifact, File>> list,
      @Nonnull final File file) {
    boolean result = false;
    for (final Tuple<Artifact, File> t : list) {
      if (t.right().equals(file)) {
        result = true;
        break;
      }
    }
    return result;
  }

  @Nullable
  public static String findProperty(
      @Nonnull final MavenSession session,
      @Nonnull final MavenProject project,
      @Nonnull final String key,
      @Nullable final String defaultValue
  ) {
    final Properties properties = new Properties(project.getProperties());
    properties.putAll(session.getSystemProperties());
    properties.putAll(session.getUserProperties());
    return properties.getProperty(key, defaultValue);
  }
}
