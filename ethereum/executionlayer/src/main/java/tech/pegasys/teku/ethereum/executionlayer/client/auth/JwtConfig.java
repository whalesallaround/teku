/*
 * Copyright 2022 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.ethereum.executionlayer.client.auth;

import java.security.Key;

public class JwtConfig {

  /**
   * EL SHOULD only accept iat timestamps which are within +-5 seconds from the current time.
   * https://github.com/ethereum/execution-apis/blob/main/src/engine/authentication.md
   */
  public static final long TOLERANCE_IN_SECONDS = 5;

  /**
   * This constant should be less than {@link #TOLERANCE_IN_SECONDS} for delivery delays/time
   * difference
   */
  public static final long EXPIRES_IN_SECONDS = TOLERANCE_IN_SECONDS - 2;

  private final Key key;

  public JwtConfig(final Key key) {
    this.key = key;
  }

  public Key getKey() {
    return key;
  }
}
