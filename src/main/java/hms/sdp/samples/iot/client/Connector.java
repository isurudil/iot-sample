package hms.sdp.samples.iot.client;

import hms.sdp.samples.iot.ws.WebSocketClientEndpoint;

import java.net.URI;
import java.net.URISyntaxException;

public class Connector {


    public static void connect(){
        try {
            // open websocket
            final WebSocketClientEndpoint clientEndPoint = new WebSocketClientEndpoint(new URI("ws://api.hsenidmobile.com:9008/iot/connect?token=APP_1234"));

            // add listener
            clientEndPoint.addMessageHandler(new WebSocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    System.out.println(message);
                }
            });

            // send message to websocket
            clientEndPoint.sendMessage("{'id':'123456','type':'command','content':'d5=1;','device':'DEVICE_1234'}");

            // wait 5 seconds for messages from websocket
            Thread.sleep(5000);

        } catch (InterruptedException ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }
}
