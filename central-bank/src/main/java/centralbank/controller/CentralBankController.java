package centralbank.controller;

import centralbank.model.Transaction;
import centralbank.service.CentralBankService;
import centralbank.util.StringUtil;
import dtos.SendFunds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/blockchain")
public class CentralBankController {
    @Autowired
    private CentralBankService centralBankService;

    @PostMapping("/issueFunds")
    public ResponseEntity<String> sendFunds(@RequestBody SendFunds request) {
        centralBankService.issueFunds(request.getAmount());
        return ResponseEntity.ok("Funds issued");
    }
}
