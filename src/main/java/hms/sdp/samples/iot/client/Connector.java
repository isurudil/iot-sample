package hms.sdp.samples.iot.client;

import hms.sdp.samples.iot.util.PropertyLoader;
import hms.sdp.samples.iot.ws.WebSocketClientEndpoint;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class Connector {

    private static Logger logger = Logger.getLogger(Connector.class);

    public static void send(String jsonMessage){
        try {
            final WebSocketClientEndpoint clientEndPoint = new WebSocketClientEndpoint(new URI(PropertyLoader.getProperty("web.socket.endpoint")));

            clientEndPoint.addMessageHandler(new WebSocketClientEndpoint.MessageHandler() {
                public void handleMessage(String msg) {
                    logger.info("Sending message : " + msg);
                }
            });
            clientEndPoint.sendMessage(jsonMessage);
        } catch (URISyntaxException ex) {
            logger.error("URISyntaxException exception: " , ex);
        }
    }
}
