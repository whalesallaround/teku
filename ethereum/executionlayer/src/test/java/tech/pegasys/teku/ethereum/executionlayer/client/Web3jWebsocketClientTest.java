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

package tech.pegasys.teku.ethereum.executionlayer.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.VoidResponse;
import org.web3j.protocol.websocket.WebSocketService;
import tech.pegasys.teku.ethereum.executionlayer.client.schema.Response;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.time.TimeProvider;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class Web3jWebsocketClientTest {
  private final TimeProvider timeProvider = mock(TimeProvider.class);
  private final WebSocketService webSocketService = mock(WebSocketService.class);
  private final URI endpoint = URI.create("");
  private Web3jWebsocketClient web3jWebsocketClient;

  @BeforeEach
  public void setup() {
    this.web3jWebsocketClient = new Web3jWebsocketClient(endpoint, timeProvider, Optional.empty());
    web3jWebsocketClient.initWeb3jService(webSocketService);
    when(timeProvider.getTimeInMillis()).thenReturn(UInt64.ONE);
  }

  @Test
  public void shouldConnectBeforeRequest() throws Exception {
    Request<Void, VoidResponse> request =
        new Request<>("test", new ArrayList<>(), webSocketService, VoidResponse.class);
    doNothing().when(webSocketService).connect(any(), any(), any());
    when(webSocketService.sendAsync(request, VoidResponse.class))
        .thenReturn(CompletableFuture.completedFuture(new VoidResponse()));
    SafeFuture<Response<Void>> result = web3jWebsocketClient.doRequest(request);
    assertThat(result.isCompletedNormally()).isTrue();
    verify(webSocketService, times(1)).connect(any(), any(), any());
  }

  @Test
  public void shouldNotRequestIfConnectFailed() throws Exception {
    Request<Void, VoidResponse> request =
        new Request<>("test", new ArrayList<>(), webSocketService, VoidResponse.class);
    doThrow(new ConnectException("Failed")).when(webSocketService).connect(any(), any(), any());
    SafeFuture<Response<Void>> result = web3jWebsocketClient.doRequest(request);
    assertThat(result.isCompletedExceptionally()).isTrue();
    verify(webSocketService, times(1)).connect(any(), any(), any());
    verify(webSocketService, never()).send(any(), any());
    verify(webSocketService, never()).sendAsync(any(), any());
  }
}
