pragma solidity ^0.4.19;

import "./strings.sol";

// WARN: Do not use on Mainnet:
// This is created by my hobby and is NOT completely secure.
// Use at your own risk, and be careful for loosing your money.

// FileSourceContract that doesn't use
contract FileSourceContractLight {
    address private owner;
    mapping(string => string) private files;
    bool private alive;

    function FileSourceContractLight() public {
        owner = msg.sender;
        alive = true;
    }

    function getVersion() public pure returns (uint) {
        return 1;
    }

    function explode() public restricted {
        alive = false;
        selfdestruct(owner);
    }

    modifier noAlive() {
        require(msg.sender == owner);
        _;
    }

    modifier restricted() {
        require(msg.sender == owner);
        require(alive);
        _;
    }

    function isAlive() public view noAlive returns (int) {
        return alive ? int(1) : int(- 1);
    }

    function setFile(string filename, string ipfsDir) public restricted {
        files[filename] = ipfsDir;
    }

    function getFile(string filename) public view restricted returns (string) {
        return files[filename];
    }

    function removeFile(string filename) public restricted returns (bool){
        files[filename] = "";
    }
}
