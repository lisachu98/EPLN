package wallet.controller;

import dtos.SendFunds;

import wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;

    @PostMapping("/sendFunds")
    public ResponseEntity<String> sendFunds(@RequestBody SendFunds request) {
        walletService.sendFunds(request.getReceiver(), request.getAmount());
        return ResponseEntity.ok("Funds sent");
    }
}
