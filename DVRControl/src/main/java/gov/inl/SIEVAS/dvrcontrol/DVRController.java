/* 
 * Copyright 2017 Idaho National Laboratory.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.inl.SIEVAS.dvrcontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.inl.SIEVAS.entity.DVRCommandMessage;
import gov.inl.SIEVAS.entity.DVRCommandMessageReply;
import gov.inl.SIEVAS.entity.DVRCommandType;
import gov.inl.SIEVAS.entity.DVRPlayMode;
import gov.inl.SIEVAS.entity.SIEVASSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * @author monejh
 */
public class DVRController implements Initializable
{
    public static String sessionListURL = "api/sessions/";
    
    private String baseURL = "";
    private RestController restController;
    private ObjectMapper objMapper = new ObjectMapper();
    
    @FXML
    private Button btnStop;
    
    @FXML
    private Button btnStart;
    
    @FXML
    private TextArea txtInfo;
    
    @FXML
    private Label lblPlaySpeed;
    
    @FXML
    private Label lblPlaying;
    
    private SIEVASSession session;
    private ActiveMQConnectionFactory factory;
    private Connection connection;
    private Session amqSession;
    private Destination dataDestination;
    private MessageConsumer dataConsumer;
    private Destination controlDestination;
    private MessageConsumer controlConsumer;
    private MessageProducer controlProducer;
    
    
    
    public void setSession(SIEVASSession session)
    {
        this.session = session;
    }
    
    public void Load()
    {
       try
       {
            connectToSIEVAS(this.session);
       }
       catch(InterruptedException | JMSException e)
       {
           Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, e);
       }
    }
    
    
    
    @FXML
    private void handleStop(ActionEvent event) throws JMSException
    {
        //set stop message
        DVRCommandMessage dvrMsg = new DVRCommandMessage(DVRCommandType.Stop, (long)Math.floor(Math.random()*Long.MAX_VALUE));
        try
        {
            TextMessage msg = amqSession.createTextMessage(objMapper.writeValueAsString(dvrMsg));
            msg.setStringProperty("ObjectName", dvrMsg.getClass().getSimpleName());
            controlProducer.send(msg);
        }
        catch(JMSException|JsonProcessingException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback error");
            alert.setHeaderText("Error stopping playback");
            alert.setContentText(e.getMessage());

            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleStart(ActionEvent event)
    {
        //set play message
        DVRCommandMessage dvrMsg = new DVRCommandMessage(DVRCommandType.Start, (long)Math.floor(Math.random()*Long.MAX_VALUE));
        try
        {
            TextMessage msg = amqSession.createTextMessage(objMapper.writeValueAsString(dvrMsg));
            msg.setStringProperty("ObjectName", dvrMsg.getClass().getSimpleName());
            controlProducer.send(msg);
        }
        catch(JMSException|JsonProcessingException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback error");
            alert.setHeaderText("Error starting playback");
            alert.setContentText(e.getMessage());

            alert.showAndWait();
        }
        
    }
    
    @FXML
    private void handleQuit(ActionEvent event) throws JMSException
    {
        //set stop message
        DVRCommandMessage dvrMsg = new DVRCommandMessage(DVRCommandType.Stop, (long)Math.floor(Math.random()*Long.MAX_VALUE));
        try
        {
            TextMessage msg = amqSession.createTextMessage(objMapper.writeValueAsString(dvrMsg));
            msg.setStringProperty("ObjectName", dvrMsg.getClass().getSimpleName());
            controlProducer.send(msg);
        }
        catch(JMSException|JsonProcessingException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback error");
            alert.setHeaderText("Error stopping playback");
            alert.setContentText(e.getMessage());

            alert.showAndWait();
        }
        System.exit(0);
    }
    
    
    private void appendText(String txt)
    {
        if (txtInfo!=null)
            Platform.runLater(() -> {
                String currentTxt = txtInfo.getText();
                if (currentTxt.length()>4000)
                    currentTxt = currentTxt.substring(currentTxt.length()-4000, currentTxt.length()-1);
                currentTxt += txt;
                txtInfo.setText(currentTxt);
            });
        
    }
    
    private void appendException(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(e.getMessage());
        pw.println();
        e.printStackTrace(pw);
        pw.println();
        
        if (txtInfo!=null)
            Platform.runLater(() -> {
                String currentTxt = txtInfo.getText();
                if (currentTxt.length()>4000)
                    currentTxt = currentTxt.substring(currentTxt.length()-4000, currentTxt.length()-1);
                currentTxt += pw.toString();
                txtInfo.setText(currentTxt);
            });
        
    }
    
    /***
     * Connects to the ActiveMQ streams for display
     * @param session The SIEVASSession object to connect to.
     * @throws JMSException
     * @throws InterruptedException 
     */
    private void connectToSIEVAS(SIEVASSession session) throws JMSException, InterruptedException
    {
        //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"Connecting");
        appendText("Connecting...\n");
        
        //connect to AMQ first
        factory = new ActiveMQConnectionFactory(session.getActivemqUrl());
        connection = factory.createConnection();
        amqSession = connection.createSession(false,  Session.AUTO_ACKNOWLEDGE);
        
        //create data stream topic
        dataDestination = amqSession.createTopic(session.getDataStreamName());
        dataConsumer = amqSession.createConsumer(dataDestination);
        dataConsumer.setMessageListener(new MessageListener()
        {
            @Override
            public void onMessage(Message msg)
            {
                if (msg instanceof TextMessage)
                {
                    TextMessage txtMsg = (TextMessage)msg;
                    String objName = null;
                    try
                    {
                        objName = txtMsg.getStringProperty("ObjectName");
                    }
                    catch(JMSException e)
                    {
                        appendException(e);
                        //Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, e);
                    }
                    if ((objName!=null) && objName.equals("Nbody"))
                    {
                        try
                        {
                            //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"NBODY:" + txtMsg.getText());
                            appendText("NBody:" + txtMsg.getText() + "\n");
                        }
                        catch (JMSException ex)
                        {
                            //Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, ex);
                            appendException(ex);
                        }
                    }
                    else
                        appendText("Data Message:" + msg);
                        //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"Data Message:" + msg);
                }
                else
                    appendText("Data Message:" + msg);
                    //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"Data Message:" + msg);
            }
        });
        
        //create control stream topic
        controlDestination = amqSession.createTopic(session.getControlStreamName());
        controlConsumer = amqSession.createConsumer(controlDestination);
        controlConsumer.setMessageListener(new MessageListener()
        {
            @Override
            public void onMessage(Message msg)
            {
                TextMessage txtMsg = (TextMessage)msg;
                String objName = null;
                try
                {
                    objName = txtMsg.getStringProperty("ObjectName");
                }
                catch(JMSException e)
                {
                    appendException(e);
                    //Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, e);
                }
                if ((objName!=null) && objName.equals(DVRCommandMessageReply.class.getSimpleName()))
                {
                    try
                    {
                        System.out.println("Got control message");
                        DVRCommandMessageReply dvrMsg = objMapper.readValue(txtMsg.getText(), DVRCommandMessageReply.class);
                        switch(dvrMsg.getCommandType())
                        {
                            case Start:
                            case Stop:
                            case GetStatus:
                                appendText("DVR Now " + dvrMsg.getPlayMode() + ", speed = " + dvrMsg.getPlaySpeed() + "\n");
                                if (dvrMsg.getPlayMode() == DVRPlayMode.Started)
                                {
                                    Platform.runLater(()->{
                                        btnStop.setDisable(false);
                                        btnStart.setDisable(true);
                                        lblPlaySpeed.setText("" + dvrMsg.getPlaySpeed());
                                        lblPlaying.setText("Playing");
                                    });
                                }
                                else if (dvrMsg.getPlayMode() == DVRPlayMode.Stopped)
                                {
                                    System.out.println("Enabling START");
                                    Platform.runLater(()->{
                                        btnStop.setDisable(true);
                                        btnStart.setDisable(false);
                                        lblPlaySpeed.setText("" + dvrMsg.getPlaySpeed());
                                        lblPlaying.setText("Stopped");
                                    });
                                }
                                break;
                        }
                    } 
                    catch (IOException | JMSException ex)
                    {
                        Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else
                    appendText("Control Message:" + msg);
                //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"Control Message:" + msg);
            }
        });
        controlProducer = amqSession.createProducer(controlDestination);
        //start!
        connection.start();
        //Logger.getLogger(DVRController.class.getName()).log(Level.INFO,"Done");
        appendText("Done\n");
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
           @Override
           public void run()
           {
               try
               {
                   TextMessage msg = amqSession.createTextMessage("Stop");
                   msg.setStringProperty("ObjectName", "DVRControl");
                   controlProducer.send(msg);
               }
               catch (JMSException ex)
               {
                   //Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, ex);
                   appendException(ex);
               }
           }
        });
        
        //request status
        DVRCommandMessage dvrMsg = new DVRCommandMessage(DVRCommandType.GetStatus, (long)Math.floor(Math.random()*Long.MAX_VALUE));
        TextMessage txtMsg;
        try
        {
            txtMsg = amqSession.createTextMessage(objMapper.writeValueAsString(dvrMsg));
            txtMsg.setStringProperty("ObjectName", dvrMsg.getClass().getSimpleName());
            controlProducer.send(txtMsg);
        }
        catch (JsonProcessingException ex)
        {
            Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        //wait for a long time.
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.shutdown();
        
    }

    public void setBaseURL(String url)
    {
        this.baseURL = url;
    }
    
    
    public void setRestController(RestController rest)
    {
        this.restController = rest;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        txtInfo.setScrollLeft(0);
        txtInfo.setScrollTop(Double.MAX_VALUE);
        
    }
    
}
