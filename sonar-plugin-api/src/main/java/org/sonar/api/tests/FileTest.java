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

package org.sonar.api.tests;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class FileTest {

  private String key;

  private List<Test> tests;

  public FileTest(String key) {
    this.key = key;
    tests = newArrayList();
  }

  public String getKey() {
    return key;
  }

  public List<Test> getTests() {
    return tests;
  }

  public void addTest(Test test) {
    tests.add(test);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileTest fileTest = (FileTest) o;

    if (!key.equals(fileTest.key)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("key", key)
        .append("tests", tests)
        .toString();
  }
}
