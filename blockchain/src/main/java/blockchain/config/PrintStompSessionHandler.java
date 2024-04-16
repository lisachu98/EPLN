package blockchain.config;

import blockchain.model.Block;
import blockchain.model.Transaction;
import blockchain.service.BlockchainService;
import blockchain.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;

@Service
public class PrintStompSessionHandler extends StompSessionHandlerAdapter {

    private BlockchainService blockchainService;

    @Autowired
    public PrintStompSessionHandler(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }
    @Override
    public Type getPayloadType(StompHeaders headers) {
        return String.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        String topic = headers.getDestination();
        switch (topic) {
            case "/topic/blocks":
                handleBlock(payload);
                break;
            case "/topic/transactions":
                handleTransaction(payload);
                break;
            case "/topic/centralblocks":
                handleCentralBlock(payload);
                break;
            case "/topic/centraltransactions":
                handleCentralTransaction(payload);
                break;
            case "/topic/test":
                handleTest(payload);
                break;
        }
    }

    private void handleBlock(Object payload) {
        Block block = StringUtil.getObjectFromJson(payload.toString(), Block.class);
        blockchainService.addBlock(block);
    }

    private void handleCentralBlock(Object payload) {
        Block block = StringUtil.getObjectFromJson(payload.toString(), Block.class);
        blockchainService.addCentralBlock(block);
    }

    private void handleTransaction(Object payload) {
        try {
            Transaction transaction = StringUtil.getObjectFromJson(payload.toString(), Transaction.class);
            transaction.verifySignature();
            blockchainService.addTransaction(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCentralTransaction(Object payload) {
        try {
            Transaction transaction = StringUtil.getObjectFromJson(payload.toString(), Transaction.class);
            blockchainService.addCentralTransaction(transaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTest(Object payload) {
        new Thread(() -> blockchainService.sendFundsTest(1500)).start();
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        session.subscribe("/topic/transactions", this);
        System.out.println("Subscribed to /topic/transactions");
        session.subscribe("/topic/blocks", this);
        System.out.println("Subscribed to /topic/blocks");
        session.subscribe("/topic/centraltransactions", this);
        System.out.println("Subscribed to /topic/centraltransactions");
        session.subscribe("/topic/centralblocks", this);
        System.out.println("Subscribed to /topic/centralblocks");
        session.subscribe("/topic/test", this);
        System.out.println("Subscribed to /topic/test");
    }
}