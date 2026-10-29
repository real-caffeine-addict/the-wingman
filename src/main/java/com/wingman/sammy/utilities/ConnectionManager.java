package com.wingman.sammy.utilities;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.QrCode;
import com.wingman.sammy.bots.SimpleBot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConnectionManager {
    private LinkedWhatsAppClient client;
    private final SimpleBot simpleBot;

    public ConnectionManager (SimpleBot simpleBot) {
        this.simpleBot = simpleBot;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect () throws IOException{
        var options = WhatsAppClient.builder()
                .linkedApi()
                .webClient()
                .loadLatestOrCreateConnection();

        client = options.unregistered(QrCode.toTerminal());

        client.addLoggedInListener(api -> System.out.println("Sammy connected"));
        client.addNewMessageListener(simpleBot::onMessage);
        client.connect();

        client.addDisconnectedListener((api, reason) ->
                System.out.println("Sammy disconnected: " + reason));
    }
}
