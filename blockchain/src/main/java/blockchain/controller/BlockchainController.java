package blockchain.controller;

import blockchain.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blockchain")
public class BlockchainController {
    @Autowired
    private BlockchainService blockchainService;

    @PostMapping("/addBlock")
    public ResponseEntity<String> addBlock() {
        blockchainService.addBlock();
        blockchainService.isChainValid();
        System.out.println("Blockchain is valid: " + blockchainService.isChainValid());
        return ResponseEntity.ok("Block added");
    }
}
