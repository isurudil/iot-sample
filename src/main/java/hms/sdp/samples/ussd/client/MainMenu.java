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
package hms.sdp.samples.ussd.client;

import hms.kite.samples.api.SdpException;
import hms.kite.samples.api.StatusCodes;
import hms.kite.samples.api.ussd.MoUssdListener;
import hms.kite.samples.api.ussd.UssdRequestSender;
import hms.kite.samples.api.ussd.messages.MoUssdReq;
import hms.kite.samples.api.ussd.messages.MtUssdReq;
import hms.kite.samples.api.ussd.messages.MtUssdResp;
import hms.sdp.samples.ussd.utils.PropertyLoader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainMenu implements MoUssdListener {

    private final static Logger LOGGER = Logger.getLogger(MainMenu.class
            .getName());

    //hardcoded values
    private static final String EXIT_SERVICE_CODE = "000";
    private static final String BACK_SERVICE_CODE = "999";
    private static final String INIT_SERVICE_CODE = "#678*";
    private static final String REQUEST_SENDER_SERVICE = "http://localhost:7000/ussd/send";
    private static final String PROPERTY_KEY_PREFIX = "menu.level.";
    private static final String USSD_OPERATION_MT_CONT="mt-cont";
    private static final String USSD_OPERATION_MT_FIN="mt-fin";

    //menu state saving for back button
    private List<Byte> menuState = new ArrayList<Byte>();

    //service to send the request
    private UssdRequestSender ussdMtSender;

    @Override
    public void init() {
        //create and initialize service
        try {
            ussdMtSender = new UssdRequestSender(new URL(REQUEST_SENDER_SERVICE));
        } catch (MalformedURLException e) {
            LOGGER.log(Level.INFO, "Unexpected error occurred", e);
        }
    }

    /**
     * Receive requests
     * @param moUssdReq
     */
    @Override
    public void onReceivedUssd(MoUssdReq moUssdReq) {
        try {
            //start processing request
            processRequest(moUssdReq);
        } catch (SdpException e) {
            LOGGER.log(Level.INFO, "Unexpected error occurred", e);
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
        byte serviceCode;
        if (moUssdReq.getMessage().equals(INIT_SERVICE_CODE)) {
            serviceCode=0;
            clearMenuState();
        }else{
            serviceCode=getServiceCode(moUssdReq);
        }
        //create request to display user
        final MtUssdReq request = createRequest(moUssdReq, buildNextMenuContent(serviceCode),USSD_OPERATION_MT_CONT);
        sendRequest(request);
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
    private String getText(byte key) {
        return PropertyLoader.getInstance().getText(PROPERTY_KEY_PREFIX + key);
    }

    /**
     * Request sender
     * @param request
     * @return MtUssdResp
     */
    private MtUssdResp sendRequest(MtUssdReq request) throws SdpException {
        //sending request to service
        MtUssdResp response = null;
        try {
            response = ussdMtSender.sendUssdRequest(request);
        } catch (SdpException e) {
            LOGGER.log(Level.INFO, "Unable to send request", e);
            throw e;
        }

        //response status
        String statusCode = response.getStatusCode();
        String statusDetails = response.getStatusDetail();
        if (StatusCodes.SuccessK.equals(statusCode)) {
            LOGGER.info("MT USSD message successfully sent");
        } else {
            LOGGER.info("MT USSD message sending failed with status code ["
                    + statusCode + "] " + statusDetails);
        }
        return response;
    }

    /**
     * Clear history list
     */
    private void clearMenuState() {
        LOGGER.info("clear history List");
        menuState.clear();
    }

    /**
     * Terminate session
     * @param moUssdReq
     * @throws SdpException
     */
    private void terminateSession(MoUssdReq moUssdReq) throws SdpException {
        final MtUssdReq request = createRequest(moUssdReq, "", USSD_OPERATION_MT_FIN);
        sendRequest(request);
    }

    /**
     * Handlling back button with menu state
     * @param moUssdReq
     * @throws SdpException
     */
    private void backButtonHandle(MoUssdReq moUssdReq) throws SdpException {
        byte lastMenuVisited = 0;

        //remove last menu when back
        if(menuState.size()>0){
            menuState.remove(menuState.size() - 1);
            lastMenuVisited = menuState.get(menuState.size() - 1);
        }

        //create request and send
        final MtUssdReq request = createRequest(moUssdReq, buildBackMenuContent(lastMenuVisited), USSD_OPERATION_MT_CONT);
        sendRequest(request);

        //clear menu status
        if(lastMenuVisited==0){
            clearMenuState();
            //add 0 to menu state ,finally its in main menu
            menuState.add((byte)0);
        }

    }

    /**
     * Create service code to navigate through menu and for property loading
     * @param moUssdReq
     * @return serviceCode
     */
    private byte getServiceCode(MoUssdReq moUssdReq){
        byte serviceCode=0;
        try {
            serviceCode=Byte.parseByte(moUssdReq.getMessage());
        } catch (NumberFormatException e) {
            return serviceCode;
        }

        //create service codes for child menus based on the main menu codes
        if (menuState.size() > 0 && menuState.get(menuState.size() - 1) != 0) {
            String generatedChildServiceCode = "" + menuState.get(menuState.size() - 1) + serviceCode;
            serviceCode = Byte.parseByte(generatedChildServiceCode);
        }

        return serviceCode;
    }

    /**
     * Build next menu based on the service code
     * @param selection
     * @return menuContent
     */
    private String buildNextMenuContent(byte selection){
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
    private String buildBackMenuContent(byte selection){
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