#!/usr/bin/env bash
cd ./contract/
truffle compile
cd ../
web3j truffle generate ./contract/build/contracts/FileSource.json -o ./src/solidity -p com.nao20010128nao.Cryptorage
