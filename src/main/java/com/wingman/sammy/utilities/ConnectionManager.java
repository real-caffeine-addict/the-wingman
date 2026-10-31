package com.wingman.sammy.utilities;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler.Web.QrCode;
import com.wingman.sammy.shoppingbot.ShoppingBot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConnectionManager {
    private LinkedWhatsAppClient client;
    private final ShoppingBot shoppingBot;

    public ConnectionManager (ShoppingBot shoppingBot) {
    this.shoppingBot = shoppingBot;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect () throws IOException{
        var options = WhatsAppClient.builder()
                .linkedApi()
                .webClient()
                .loadLatestOrCreateConnection();

        client = options.unregistered(QrCode.toTerminal());

        client.addLoggedInListener(api -> System.out.println("Sammy connected"));
        client.addNewMessageListener(shoppingBot::onAddItem);
        client.connect();

        client.addDisconnectedListener((api, reason) ->
                System.out.println("Sammy disconnected: " + reason));
    }
}
