pragma solidity ^0.4.19;

// WARN: Do not use on Mainnet:
// This is created by my hobby and is NOT completely secure.
// Use at your own risk, and be careful for loosing your money.

contract FileSourceContract {
    address private owner;
    mapping(string => string) private files;
    string[] private fileList;
    bool private alive;

    function FileSourceContract() public {
        owner = msg.sender;
        alive = true;
    }

    function getVersion() public pure returns (uint) {
        return 1;
    }

    function explode() public restricted {
        for (uint i = 0; i < fileList.length; i++) {
            files[fileList[i]] = "";
        }
        fileList.length = 0;
        alive = false;
        selfdestruct(msg.owner);
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
        if (keccak256(abi.encodePacked(files[filename])) == keccak256(abi.encodePacked("")))
            fileList.push(filename);
        files[filename] = ipfsDir;
    }

    function getFile(string filename) public view restricted returns (string) {
        return files[filename];
    }

    function removeFile(string filename) public restricted returns (bool){
        bytes32 nameHashes = keccak256(abi.encodePacked(filename));
        for (uint i = 0; i < fileList.length; i++) {
            if (keccak256(abi.encodePacked(fileList[i])) == nameHashed) {
                fileList[i] = fileList[fileList.length - 1];
                fileList.length--;
            }
        }
        files[filename] = "";
    }

    function getFileListLength() public view restricted returns (uint){
        return fileList.length;
    }

    function getFileList(uint index) public view restricted returns (string) {
        return fileList[index];
    }

    function getFileListCombined(byte terminator) public view restricted returns (string) {
        uint length = 0;
        uint flLength = fileList.length;
        for (uint i = 0; i < flLength; i++) {
            length += bytes(fileList[i]).length;
        }
        length += flLength - 1;
        bytes memory data = new bytes(length);
        uint offset = 0;
        for (i = 0; i < flLength; i++) {
            if (i > 0) {
                data[offset++] = terminator;
            }
            bytes storage entry = bytes(fileList[i]);
            for (uint j = 0; j < bytes(entry).length; j++) {
                data[offset++] = entry[j];
            }
        }
        return string(data);
    }
}
