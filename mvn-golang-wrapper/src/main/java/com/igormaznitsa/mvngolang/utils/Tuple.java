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

import com.igormaznitsa.meta.common.utils.Assertions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Tuple<A, B> {

  private final A left;
  private final B right;

  private Tuple(@Nonnull final A left, @Nonnull final B right) {
    this.left = Assertions.assertNotNull(left);
    this.right = Assertions.assertNotNull(right);
  }

  @Nonnull
  public static <A, B> Tuple<A, B> of(@Nonnull A left, @Nonnull B right) {
    return new Tuple<>(left, right);
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof Tuple) {
      final Tuple<?, ?> that = (Tuple<?, ?>) obj;
      return this.left.equals(that.left) && this.right.equals(that.right);
    }
    return false;
  }

  @Override
  @Nonnull
  public String toString() {
    return "Tuple(" + this.left + " ; " + this.right + ')';
  }

  @Override
  public int hashCode() {
    return this.left.hashCode() ^ this.right.hashCode();
  }

  @Nonnull
  public A left() {
    return this.left;
  }

  @Nonnull
  public B right() {
    return this.right;
  }
}
