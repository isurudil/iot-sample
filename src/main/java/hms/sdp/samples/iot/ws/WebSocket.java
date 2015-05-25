package hms.sdp.samples.iot.ws;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

public class WebSocket {


    public static void connect() throws URISyntaxException {
        WebSocketClient wsClient = new WebSocketClient(new URI("ws://api.hsenidmobile.com:9008/iot/connect?token=APP_1234"),
                new Draft_10()) {

            @Override
            public void onMessage(String message) {
                JSONObject obj = new JSONObject(message);
                String type = obj.getString("type");
                System.out.println("sending message of type : " + message);
            }

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("opened connection");
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("closed connection");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }

        };
        //open websocket
        wsClient.connect();
        JSONObject obj = new JSONObject();
        obj.put("id", "123456   ");
        obj.put("type", "command");
        obj.put("content", "while 1 {if(a0 < 200){print 'level=' a0;}; snooze(1000); };");
        obj.put("device", "message.device");
        String message = obj.toString();
        try {
            System.out.println("connecting");
            wsClient.send(message);
        } catch (NotYetConnectedException e) {
            System.out.println("error");
            throw new NotYetConnectedException();
        } finally {
            wsClient.close();
        }
    }
}
