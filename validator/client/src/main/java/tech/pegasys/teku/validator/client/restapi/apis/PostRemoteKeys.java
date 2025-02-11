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

package tech.pegasys.teku.validator.client.restapi.apis;

import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_OK;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.List;
import tech.pegasys.teku.infrastructure.http.RestApiConstants;
import tech.pegasys.teku.infrastructure.restapi.endpoints.EndpointMetadata;
import tech.pegasys.teku.infrastructure.restapi.endpoints.RestApiEndpoint;
import tech.pegasys.teku.infrastructure.restapi.endpoints.RestApiRequest;
import tech.pegasys.teku.validator.client.KeyManager;
import tech.pegasys.teku.validator.client.restapi.ValidatorTypes;
import tech.pegasys.teku.validator.client.restapi.apis.schema.ExternalValidator;
import tech.pegasys.teku.validator.client.restapi.apis.schema.PostRemoteKeysRequest;

public class PostRemoteKeys extends RestApiEndpoint {
  public static final String ROUTE = "/eth/v1/remotekeys";

  @SuppressWarnings("unused")
  private final KeyManager keyManager;

  public PostRemoteKeys(final KeyManager keyManager) {
    super(
        EndpointMetadata.post(ROUTE)
            .operationId("ImportRemoteKeys")
            .summary("Import Remote Keys")
            .tags(RestApiConstants.TAG_EXPERIMENTAL)
            .description("Import remote keys for the validator client to request duties for.")
            .withBearerAuthSecurity()
            .requestBodyType(ValidatorTypes.POST_REMOTE_KEYS_REQUEST)
            .response(SC_OK, "Success response", ValidatorTypes.POST_KEYS_RESPONSE)
            .withAuthenticationResponses()
            .build());
    this.keyManager = keyManager;
  }

  @Override
  public void handleRequest(final RestApiRequest request) throws JsonProcessingException {
    final PostRemoteKeysRequest body = request.getRequestBody();

    if (body.getExternalValidators().isEmpty()) {
      request.respondOk(Collections.emptyList());
      return;
    }

    List<ExternalValidator> validators = body.getExternalValidators();
    request.respondOk(keyManager.importExternalValidators(validators));
  }
}
