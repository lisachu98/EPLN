package blockchain.service;

import blockchain.model.Block;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class BlockchainService {
    private List<Block> blockchain;

    @PostConstruct
    public void initializeBlockchain() {
        this.blockchain = new ArrayList<>();
        createGenesisBlock();
        System.out.println("Blockchain initialized");
    }

    private void createGenesisBlock() {
        Block genesis = new Block("0");
        genesis.mineBlock(4);
        blockchain.add(genesis);
    }

    public void addBlock() {
        Block block = new Block(blockchain.get(blockchain.size() - 1).getHash());
        block.mineBlock(4);
        blockchain.add(block);
        System.out.println("Block added");
    }

    public boolean isChainValid() {
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                return false;
            }
        }
        return true;
    }
}
