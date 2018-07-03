pragma solidity ^0.4.19;

import "truffle/Assert.sol";
import "truffle/DeployedAddresses.sol";
import "../contracts/FileSourceContract.sol";

contract Tests {
    function testContract() {
        FileSourceContract fs = FileSourceContract(DeployedAddresses.FileSourceContract());
        fs.explode();
    }
}