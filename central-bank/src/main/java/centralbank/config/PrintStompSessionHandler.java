package centralbank.config;

import centralbank.model.Transaction;
import centralbank.service.CentralBankService;
import centralbank.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;

@Service
public class PrintStompSessionHandler extends StompSessionHandlerAdapter {

    private CentralBankService centralBankService;

    @Autowired
    public PrintStompSessionHandler(CentralBankService centralBankService) {
        this.centralBankService = centralBankService;
    }
    @Override
    public Type getPayloadType(StompHeaders headers) {
        return String.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        String topic = headers.getDestination();
        switch (topic) {
            case "/topic/centraltransactions":
                handleTransaction(payload);
                break;
        }
    }

    private void handleTransaction(Object payload) {
        try {
            Transaction transaction = StringUtil.getObjectFromJson(payload.toString(), Transaction.class);
            centralBankService.addTransaction(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        session.subscribe("/topic/centraltransactions", this);
        System.out.println("Subscribed to /topic/centraltransactions");
    }
}