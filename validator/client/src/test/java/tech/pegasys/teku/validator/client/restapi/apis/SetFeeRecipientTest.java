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

package tech.pegasys.teku.validator.client.restapi.apis;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_ACCEPTED;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_BAD_REQUEST;
import static tech.pegasys.teku.infrastructure.http.HttpStatusCodes.SC_INTERNAL_SERVER_ERROR;
import static tech.pegasys.teku.infrastructure.restapi.MetadataTestUtil.getRequestBodyFromMetadata;
import static tech.pegasys.teku.infrastructure.restapi.MetadataTestUtil.verifyMetadataEmptyResponse;
import static tech.pegasys.teku.infrastructure.restapi.MetadataTestUtil.verifyMetadataErrorResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.spec.datastructures.eth1.Eth1Address;
import tech.pegasys.teku.validator.client.BeaconProposerPreparer;

public class SetFeeRecipientTest {
  private final BeaconProposerPreparer beaconProposerPreparer = mock(BeaconProposerPreparer.class);
  private final SetFeeRecipient handler = new SetFeeRecipient(Optional.of(beaconProposerPreparer));

  @Test
  void metadata_shouldHandle400() throws JsonProcessingException {
    verifyMetadataErrorResponse(handler, SC_BAD_REQUEST);
  }

  @Test
  void metadata_shouldHandle500() throws JsonProcessingException {
    verifyMetadataErrorResponse(handler, SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void metadata_shouldHandle202() {
    verifyMetadataEmptyResponse(handler, SC_ACCEPTED);
  }

  @Test
  void metadata_shouldReadRequestBody() throws IOException {
    SetFeeRecipient.SetFeeRecipientBody body =
        (SetFeeRecipient.SetFeeRecipientBody)
            getRequestBodyFromMetadata(
                handler, "{\"ethaddress\":\"0xabcf8e0d4e9587369b2301d0790347320302cc09\"}");
    assertThat(body)
        .isEqualTo(
            new SetFeeRecipient.SetFeeRecipientBody(
                Eth1Address.fromHexString("0xabcf8e0d4e9587369b2301d0790347320302cc09")));
  }
}
