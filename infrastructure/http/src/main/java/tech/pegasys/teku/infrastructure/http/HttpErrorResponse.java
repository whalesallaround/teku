/*
 * Copyright 2021 ConsenSys AG.
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

package tech.pegasys.teku.infrastructure.http;

import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_BAD_REQUEST;

import java.util.Objects;

public class HttpErrorResponse {

  private final Integer status;
  private final String message;

  public static HttpErrorResponse badRequest(String message) {
    return new HttpErrorResponse(SC_BAD_REQUEST, message);
  }

  public HttpErrorResponse(final Integer status, final String message) {
    this.status = status;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public Integer getStatus() {
    return status;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final HttpErrorResponse that = (HttpErrorResponse) o;
    return Objects.equals(status, that.status) && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, message);
  }
}
