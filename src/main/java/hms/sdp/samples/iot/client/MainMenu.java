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
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.Executors;

public class MainMenu implements MoUssdListener {

    private final static Logger logger = Logger.getLogger(MainMenu.class);

    //hardcoded values
    private static final String EXIT_SERVICE_CODE = "000";
    private static final String BACK_SERVICE_CODE = "999";
    private static final String INIT_SERVICE_CODE = "#678*";
    private static final String REQUEST_SENDER_SERVICE = "http://localhost:7000/ussd/send";
    private static final String PROPERTY_KEY_PREFIX = "menu.level.";
    private static final String USSD_OPERATION_MT_CONT="mt-cont";
    private static final String USSD_OPERATION_MT_FIN="mt-fin";

    //menu state saving for back button
    private List<Integer> menuState = new ArrayList<Integer>();

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
        //exit request - session destroy
        if(moUssdReq.getMessage().equals(EXIT_SERVICE_CODE)){
            terminateSession(moUssdReq);
            return;//completed work and return
        }

        //back button handling
        if (moUssdReq.getMessage().equals(BACK_SERVICE_CODE)) {
            backButtonHandle(moUssdReq);
            return;//completed work and return
        }

        //get current service code
        int serviceCode;
        if (moUssdReq.getMessage().equals(INIT_SERVICE_CODE)) {
            serviceCode=0;
            clearMenuState();
        }else{
            serviceCode=getServiceCode(moUssdReq);
        }
        //create request to display user
        final MtUssdReq request = createRequest(moUssdReq, buildNextMenuContent(serviceCode), USSD_OPERATION_MT_CONT);
        sendMtRequest(request);
        sendWebSocketCommand(serviceCode);
        //record menu state
        menuState.add(serviceCode);
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

    /**
     * load a property from ussdmenu.properties
     * @param key
     * @return value
     */
    private String getText(int key) {
        return PropertyLoader.getProperty(PROPERTY_KEY_PREFIX + key);
    }

    private void sendWebSocketCommand(int serviceCode) {
        if (serviceCode == 111) {
            send(PropertyLoader.getProperty("command.toggle.red.on"));
        }else if(serviceCode == 112){
            send(PropertyLoader.getProperty("command.toggle.red.off"));
        }else if (serviceCode == 121) {
            send(PropertyLoader.getProperty("command.toggle.green.on"));
        }else if(serviceCode == 122){
            send(PropertyLoader.getProperty("command.toggle.green.off"));
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

    /**
     * Clear history list
     */
    private void clearMenuState() {
        logger.info("clear history List");
        menuState.clear();
    }

    /**
     * Terminate session
     * @param moUssdReq
     * @throws SdpException
     */
    private void terminateSession(MoUssdReq moUssdReq) throws SdpException {
        final MtUssdReq request = createRequest(moUssdReq, "", USSD_OPERATION_MT_FIN);
        sendMtRequest(request);
    }

    /**
     * Handlling back button with menu state
     * @param moUssdReq
     * @throws SdpException
     */
    private void backButtonHandle(MoUssdReq moUssdReq) throws SdpException {
        int lastMenuVisited = 0;

        //remove last menu when back
        if(menuState.size()>0){
            menuState.remove(menuState.size() - 1);
            lastMenuVisited = menuState.get(menuState.size() - 1);
        }

        //create request and send
        final MtUssdReq request = createRequest(moUssdReq, buildBackMenuContent(lastMenuVisited), USSD_OPERATION_MT_CONT);
        sendMtRequest(request);

        //clear menu status
        if(lastMenuVisited==0){
            clearMenuState();
            //add 0 to menu state ,finally its in main menu
            menuState.add(0);
        }

    }

    /**
     * Create service code to navigate through menu and for property loading
     * @param moUssdReq
     * @return serviceCode
     */
    private int getServiceCode(MoUssdReq moUssdReq){
        int serviceCode=0;
        try {
            serviceCode=Byte.parseByte(moUssdReq.getMessage());
        } catch (NumberFormatException e) {
            return serviceCode;
        }

        //create service codes for child menus based on the main menu codes
        if (menuState.size() > 0 && menuState.get(menuState.size() - 1) != 0) {
            String generatedChildServiceCode = "" + menuState.get(menuState.size() - 1) + serviceCode;
            serviceCode = Integer.parseInt(generatedChildServiceCode);
        }

        return serviceCode;
    }

    /**
     * Build next menu based on the service code
     * @param selection
     * @return menuContent
     */
    private String buildNextMenuContent(int selection){
        String menuContent;
        try {
            //build menu contents
            menuContent = getText(selection);
        } catch(MissingResourceException e) {
            //back to main menu
            menuContent = getText((byte)0);
        }
        return menuContent;
    }

    /**
     * Build back menu based on the service code
     * @param selection
     * @return menuContent
     */
    private String buildBackMenuContent(int selection){
        String menuContent;
        try {
            //build menu contents
            menuContent = getText(selection);

        } catch(MissingResourceException e) {
            //back to main menu
            menuContent = getText((byte)0);
        }
        return menuContent;
    }

}