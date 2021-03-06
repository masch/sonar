/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ProjectTree;

public class ProjectLock {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectLock.class);

  private final Semaphores semaphores;
  private final ProjectTree projectTree;
  private final Settings settings;

  public ProjectLock(Semaphores semaphores, ProjectTree projectTree, Settings settings) {
    this.semaphores = semaphores;
    this.projectTree = projectTree;
    this.settings = settings;
  }

  public void start() {
    if (!isInDryRunMode() && StringUtils.isNotBlank(getProject().getKey())) {
      Semaphores.Semaphore semaphore = acquire();
      if (!semaphore.isLocked()) {
        LOG.error(getErrorMessage(semaphore));
        throw new SonarException("The project is already been analysing.");
      }
    }
  }

  private String getErrorMessage(Semaphores.Semaphore semaphore) {
    long duration = semaphore.getDurationSinceLocked();
    DurationLabel durationLabel = new DurationLabel();
    String durationDisplay = durationLabel.label(duration);

    return "It looks like an analysis of '" + getProject().getName() + "' is already running (started " + durationDisplay + "). " +
      "If this is not the case, it probably means that previous analysis was interrupted " +
      "and you should then force a re-run by using the option '" + CoreProperties.FORCE_ANALYSIS + "=true'.";
  }

  public void stop() {
    if (!isInDryRunMode()) {
      release();
    }
  }

  private Semaphores.Semaphore acquire() {
    LOG.debug("Acquire semaphore on project : {}, with key {}", getProject(), getSemaphoreKey());
    if (shouldForce()) {
      // In force mode, we acquire the lock regardless there's a existing lock or not
      return semaphores.acquire(getSemaphoreKey(), 0);
    }
    return semaphores.acquire(getSemaphoreKey());
  }

  private void release() {
    LOG.debug("Release semaphore on project : {}, with key {}", getProject(), getSemaphoreKey());
    semaphores.release(getSemaphoreKey());
  }

  private String getSemaphoreKey() {
    return "batch-" + getProject().getKey();
  }

  private Project getProject() {
    return projectTree.getRootProject();
  }

  private boolean isInDryRunMode() {
    return settings.getBoolean(CoreProperties.DRY_RUN);
  }

  private boolean shouldForce() {
    return settings.getBoolean(CoreProperties.FORCE_ANALYSIS);
  }
}
