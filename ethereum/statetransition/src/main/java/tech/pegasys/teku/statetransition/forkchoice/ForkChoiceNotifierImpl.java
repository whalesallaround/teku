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

package tech.pegasys.teku.statetransition.forkchoice;

import java.util.Collection;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.async.AsyncRunnerFactory;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.async.eventthread.AsyncRunnerEventThread;
import tech.pegasys.teku.infrastructure.async.eventthread.EventThread;
import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.bytes.Bytes8;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.datastructures.operations.versions.bellatrix.BeaconPreparableProposer;
import tech.pegasys.teku.spec.executionengine.ExecutionEngineChannel;
import tech.pegasys.teku.spec.executionengine.ForkChoiceState;
import tech.pegasys.teku.spec.executionengine.ForkChoiceUpdatedResult;
import tech.pegasys.teku.storage.client.RecentChainData;

public class ForkChoiceNotifierImpl implements ForkChoiceNotifier {
  private static final Logger LOG = LogManager.getLogger();

  private final EventThread eventThread;
  private final ExecutionEngineChannel executionEngineChannel;
  private final RecentChainData recentChainData;
  private final PayloadAttributesCalculator payloadAttributesCalculator;
  private final Spec spec;

  private ForkChoiceUpdateData forkChoiceUpdateData = new ForkChoiceUpdateData();

  private boolean inSync = false; // Assume we are not in sync at startup.

  ForkChoiceNotifierImpl(
      final EventThread eventThread,
      final Spec spec,
      final ExecutionEngineChannel executionEngineChannel,
      final RecentChainData recentChainData,
      final PayloadAttributesCalculator payloadAttributesCalculator) {
    this.eventThread = eventThread;
    this.spec = spec;
    this.executionEngineChannel = executionEngineChannel;
    this.recentChainData = recentChainData;
    this.payloadAttributesCalculator = payloadAttributesCalculator;
  }

  public static ForkChoiceNotifier create(
      final AsyncRunnerFactory asyncRunnerFactory,
      final Spec spec,
      final ExecutionEngineChannel executionEngineChannel,
      final RecentChainData recentChainData,
      final Optional<? extends Bytes20> proposerDefaultFeeRecipient) {
    final AsyncRunnerEventThread eventThread =
        new AsyncRunnerEventThread("forkChoiceNotifier", asyncRunnerFactory);
    eventThread.start();
    return new ForkChoiceNotifierImpl(
        eventThread,
        spec,
        executionEngineChannel,
        recentChainData,
        new PayloadAttributesCalculator(
            spec, eventThread, recentChainData, proposerDefaultFeeRecipient));
  }

  @Override
  public void onUpdatePreparableProposers(final Collection<BeaconPreparableProposer> proposers) {
    eventThread.execute(() -> internalUpdatePreparableProposers(proposers));
  }

  @Override
  public SafeFuture<Optional<ForkChoiceUpdatedResult>> onForkChoiceUpdated(
      final ForkChoiceState forkChoiceState) {
    return eventThread.executeFuture(() -> internalForkChoiceUpdated(forkChoiceState));
  }

  @Override
  public void onAttestationsDue(final UInt64 slot) {
    eventThread.execute(() -> internalAttestationsDue(slot));
  }

  @Override
  public void onSyncingStatusChanged(final boolean inSync) {
    eventThread.execute(
        () -> {
          this.inSync = inSync;
        });
  }

  @Override
  public SafeFuture<Optional<Bytes8>> getPayloadId(
      final Bytes32 parentBeaconBlockRoot, final UInt64 blockSlot) {
    return eventThread.executeFuture(() -> internalGetPayloadId(parentBeaconBlockRoot, blockSlot));
  }

  @Override
  public void onTerminalBlockReached(Bytes32 executionBlockHash) {
    eventThread.execute(() -> internalTerminalBlockReached(executionBlockHash));
  }

  @Override
  public PayloadAttributesCalculator getPayloadAttributesCalculator() {
    return payloadAttributesCalculator;
  }

  private void internalTerminalBlockReached(Bytes32 executionBlockHash) {
    eventThread.checkOnEventThread();
    LOG.debug("internalTerminalBlockReached executionBlockHash {}", executionBlockHash);
    forkChoiceUpdateData = forkChoiceUpdateData.withTerminalBlockHash(executionBlockHash);
    LOG.debug("internalTerminalBlockReached forkChoiceUpdateData {}", forkChoiceUpdateData);
  }

  /**
   * @param parentBeaconBlockRoot root of the beacon block the new block will be built on
   * @param blockSlot slot of the block being produced, for which the payloadId has been requested
   * @return must return a Future resolving to:
   *     <p>Optional.empty() only when is safe to produce a block with an empty execution payload
   *     (after the bellatrix fork and before Terminal Block arrival)
   *     <p>Optional.of(payloadId) when one of the following: 1. builds on top of execution head of
   *     parentBeaconBlockRoot 2. builds on top of the terminal block
   *     <p>in all other cases it must Throw to avoid block production
   */
  private SafeFuture<Optional<Bytes8>> internalGetPayloadId(
      final Bytes32 parentBeaconBlockRoot, final UInt64 blockSlot) {
    eventThread.checkOnEventThread();

    LOG.debug(
        "internalGetPayloadId parentBeaconBlockRoot {} blockSlot {}",
        parentBeaconBlockRoot,
        blockSlot);

    final Bytes32 parentExecutionHash =
        recentChainData
            .getExecutionBlockHashForBlockRoot(parentBeaconBlockRoot)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Failed to retrieve execution payload hash from beacon block root"));

    final UInt64 timestamp = spec.getSlotStartTime(blockSlot, recentChainData.getGenesisTime());
    if (forkChoiceUpdateData.isPayloadIdSuitable(parentExecutionHash, timestamp)) {
      return forkChoiceUpdateData.getPayloadId();
    } else if (parentExecutionHash.isZero() && !forkChoiceUpdateData.hasTerminalBlockHash()) {
      // Pre-merge so ok to use default payload
      return SafeFuture.completedFuture(Optional.empty());
    } else {
      // Request a new payload.

      // to make sure that we deal with the same data when calculatePayloadAttributes asynchronously
      // returns, we save locally the current class reference.
      ForkChoiceUpdateData localForkChoiceUpdateData = forkChoiceUpdateData;
      return payloadAttributesCalculator
          .calculatePayloadAttributes(blockSlot, inSync, localForkChoiceUpdateData, true)
          .thenCompose(
              newPayloadAttributes -> {

                // we make the updated local data global, reverting any potential data not yet sent
                // to EL
                forkChoiceUpdateData =
                    localForkChoiceUpdateData.withPayloadAttributes(newPayloadAttributes);
                sendForkChoiceUpdated();
                return forkChoiceUpdateData
                    .getPayloadId()
                    .thenApply(
                        payloadId -> {
                          if (payloadId.isEmpty()) {
                            throw new IllegalStateException("Unable to obtain a payloadId");
                          }
                          return payloadId;
                        });
              });
    }
  }

  private void internalUpdatePreparableProposers(
      final Collection<BeaconPreparableProposer> proposers) {
    eventThread.checkOnEventThread();

    LOG.debug("internalUpdatePreparableProposers proposers {}", proposers);

    // Default to the genesis slot if we're pre-genesis.
    final UInt64 currentSlot = recentChainData.getCurrentSlot().orElse(SpecConfig.GENESIS_SLOT);

    payloadAttributesCalculator.updateProposers(proposers, currentSlot);

    // Update payload attributes in case we now need to propose the next block
    updatePayloadAttributes(currentSlot.plus(1));
  }

  private SafeFuture<Optional<ForkChoiceUpdatedResult>> internalForkChoiceUpdated(
      final ForkChoiceState forkChoiceState) {
    eventThread.checkOnEventThread();

    LOG.debug("internalForkChoiceUpdated forkChoiceState {}", forkChoiceState);

    this.forkChoiceUpdateData = this.forkChoiceUpdateData.withForkChoiceState(forkChoiceState);

    LOG.debug("internalForkChoiceUpdated forkChoiceUpdateData {}", forkChoiceUpdateData);

    recentChainData
        .getCurrentSlot()
        .ifPresent(currentSlot -> updatePayloadAttributes(currentSlot.plus(1)));
    return forkChoiceUpdateData.send(executionEngineChannel);
  }

  private void internalAttestationsDue(final UInt64 slot) {
    eventThread.checkOnEventThread();

    LOG.debug("internalAttestationsDue slot {}", slot);

    // Assume `slot` is empty and check if we need to prepare to propose in the next slot
    updatePayloadAttributes(slot.plus(1));
  }

  private void sendForkChoiceUpdated() {
    forkChoiceUpdateData
        .send(executionEngineChannel)
        .finish(error -> LOG.error("forkChoiceUpdated notification failed", error));
  }

  private void updatePayloadAttributes(final UInt64 blockSlot) {
    LOG.debug("updatePayloadAttributes blockSlot {}", blockSlot);

    forkChoiceUpdateData
        .withPayloadAttributesAsync(
            () ->
                payloadAttributesCalculator.calculatePayloadAttributes(
                    blockSlot, inSync, forkChoiceUpdateData, false),
            eventThread)
        .thenAccept(
            newForkChoiceUpdateData -> {
              if (newForkChoiceUpdateData.isPresent()) {
                forkChoiceUpdateData = newForkChoiceUpdateData.get();
                sendForkChoiceUpdated();
              }
            })
        .finish(
            error ->
                LOG.error("Failed to calculate payload attributes for slot {}", blockSlot, error));
  }
}
