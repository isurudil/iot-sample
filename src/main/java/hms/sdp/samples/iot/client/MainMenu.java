/*
 *   (C) Copyright 1996-2012 hSenid Software International (Pvt) Limited.
 *   All Rights Reserved.
 *
 *   These materials are unpublished, proprietary, confidential source code of
 *   hSenid Software International (Pvt) Limited and constitute a TRADE SECRET
 *   of hSenid Software International (Pvt) Limited.
 *
 *   hSenid Software International (Pvt) Limited retains all title to and intellectual
 *   property rights in these materials.
 *   @auther emil
 */
package hms.sdp.samples.iot.client;

import hms.kite.samples.api.SdpException;
import hms.kite.samples.api.StatusCodes;
import hms.kite.samples.api.ussd.MoUssdListener;
import hms.kite.samples.api.ussd.UssdRequestSender;
import hms.kite.samples.api.ussd.messages.MoUssdReq;
import hms.kite.samples.api.ussd.messages.MtUssdReq;
import hms.kite.samples.api.ussd.messages.MtUssdResp;
import hms.sdp.samples.iot.util.PropertyLoader;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;

public class MainMenu implements MoUssdListener {

    private final static Logger logger = Logger.getLogger(MainMenu.class);

    private static final String REQUEST_SENDER_SERVICE;

    private static final String USSD_OPERATION_MT_FIN="mt-fin";
    public static int status = 0;

    //service to send the request
    private UssdRequestSender ussdMtSender;

    @Override
    public void init() {
        //create and initialize service
        try {
            ussdMtSender = new UssdRequestSender(new URL(REQUEST_SENDER_SERVICE));
        } catch (MalformedURLException e) {
            logger.info("Unexpected error occurred", e);
        }
    }

    /**
     * Receive requests
     * @param moUssdReq
     * */
    @Override
    public void onReceivedUssd(MoUssdReq moUssdReq) {
        try {
            //start processing request
            processRequest(moUssdReq);
        } catch (SdpException e) {
            logger.info("Unexpected error occurred", e);
        }
    }

    /**
     * Build the response based on the requested service code
     * @param moUssdReq
     */
    private void processRequest(MoUssdReq moUssdReq) throws SdpException {
        final MtUssdReq request = createRequest(moUssdReq, createResponseText(), USSD_OPERATION_MT_FIN);
        sendWebSocketCommand();
        changeStatus();
        sendMtRequest(request);
    }

    private void changeStatus() {
        if (status == 0) {
            status = 1;
        } else {
            status = 0;
        }
    }

    private String createResponseText() {
        String response;
        if (status == 0) {
            response = "Switched Om";
        } else {
            response = "Switched Off";
        }
        return response;
    }

    /**
     * Build request object
     * @param moUssdReq     - Receive request object
     * @param menuContent   - menu to display next
     * @param ussdOperation - operation
     * @return MtUssdReq    - filled request object
     */
    private MtUssdReq createRequest(MoUssdReq moUssdReq, String menuContent, String ussdOperation) {
        final MtUssdReq request = new MtUssdReq();
        request.setApplicationId(moUssdReq.getApplicationId());
        request.setEncoding(moUssdReq.getEncoding());
        request.setMessage(menuContent);
        request.setPassword("password");
        request.setSessionId(moUssdReq.getSessionId());
        request.setUssdOperation(ussdOperation);
        request.setVersion(moUssdReq.getVersion());
        request.setDestinationAddress(moUssdReq.getSourceAddress());
        return request;
    }

    private void sendWebSocketCommand() {
        if (status == 0) {
            send(PropertyLoader.getProperty("command.toggle.on"));
        } else if (status == 1) {
            send(PropertyLoader.getProperty("command.toggle.off"));
        }
    }

    private void send(final String command) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                logger.info("Sending web socket command :" + command);
                Connector.send(command);
            }
        });
    }

    private void sendMtRequest(MtUssdReq request) throws SdpException {
        MtUssdResp response;
        try {
            response = ussdMtSender.sendUssdRequest(request);
        } catch (SdpException e) {
            logger.error("Unable to send request", e);
            throw e;
        }
        //response status
        String statusCode = response.getStatusCode();
        String statusDetails = response.getStatusDetail();
        if (StatusCodes.SuccessK.equals(statusCode)) {
            logger.info("MT USSD message successfully sent");
        } else {
            logger.info("MT USSD message sending failed with status code ["
                    + statusCode + "] " + statusDetails);
        }
    }

    static {
        REQUEST_SENDER_SERVICE = PropertyLoader.getProperty("sender.url");
    }
}