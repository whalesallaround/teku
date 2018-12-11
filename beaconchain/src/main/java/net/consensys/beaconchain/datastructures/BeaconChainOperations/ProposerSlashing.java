/*
 * Copyright 2018 ConsenSys AG.
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

package net.consensys.beaconchain.datastructures.BeaconChainOperations;

import net.consensys.beaconchain.datastructures.BeaconChainBlocks.ProposalSignedData;
import net.consensys.beaconchain.util.uint.UInt384;

public class ProposerSlashing {

  private int proposer_index;
  private ProposalSignedData proposal_data_1;
  private UInt384[] proposal_signature_1;
  private ProposalSignedData proposal_data_2;
  private UInt384[] proposal_signature_2;

  public ProposerSlashing() {

  }

}
