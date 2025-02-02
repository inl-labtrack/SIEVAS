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
package gov.inl.SIEVAS.service;

import gov.inl.SIEVAS.entity.SIEVASSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Spring Service to handle ActiveMQ tasks.
 * @author monejh
 */
@Service
public class ActiveMQService implements MessageListener
{
    private final BrokerService brokerService;
    private Destination mainControlDestination;
    private MessageConsumer mainControlConsumer;
    private ActiveMQConnectionFactory factory;
    private Connection connection;
    private Session session;
    private HashMap<Long,AMQSessionInfo> sessionsInfoMap = new HashMap<>();
    private DVRService dvrService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    private final ApplicationContext context;
    
    private static final String MAIN_CONTROL_TOPIC = "control";
    
    private static final int THREAD_POOL_SIZE = 10;
    private static final String CONNECTOR_URL = "tcp://localhost:61616";
    private static final String CLIENT_URL = "tcp://localhost:61616";
    
    
    /***
     * Constructor to handle startup. Starts the ActiveMQ broker and creates
     *          the server control topic.
     * @throws Exception 
     */
    @Autowired
    ActiveMQService(ApplicationContext context) throws Exception
    {
        this.context = context;
        
        brokerService = new BrokerService();
        brokerService.addConnector(CONNECTOR_URL);
        Logger.getLogger(ActiveMQService.class.getName()).log(Level.SEVERE, "Starting Message Service...");
        brokerService.start();
        Logger.getLogger(ActiveMQService.class.getName()).log(Level.SEVERE, "Done.");
        
        start();
        
    }
    
    public String getActiveMQClientUrl()
    {
        return CLIENT_URL;
    }
    
    /***
     * Starts the control topic for the server (not client).
     * @throws JMSException 
     */
    private void start() throws JMSException
    {
        factory = new ActiveMQConnectionFactory(getActiveMQClientUrl());
        connection = factory.createConnection();
        session = connection.createSession(false,  Session.AUTO_ACKNOWLEDGE);
        mainControlDestination = session.createTopic(MAIN_CONTROL_TOPIC);
        mainControlConsumer = session.createConsumer(mainControlDestination);
        mainControlConsumer.setMessageListener(this);
        connection.start();
    }

    /***
     * Handles messages on the server control topic. Prints message currently.
     * @param msg The message received.
     */
    @Override
    public void onMessage(Message msg)
    {
        Logger.getLogger(ActiveMQService.class.getName()).log(Level.INFO, "Got message " + msg);
    }
    
    /***
     * Adds a session to the list and starts the client data and control topics.
     * @param sessionId The id of the session.
     * @return The new session info with topics, producers, and consumers. 
     *              Background tasks are started.
     */
    public AMQSessionInfo addSession(SIEVASSession newSession)
    {
        
        long sessionId = newSession.getId();
        AMQSessionInfo sessionInfo = new AMQSessionInfo(sessionId);
        
        try
        {
            sessionInfo.setControlTopicName(newSession.getControlStreamName());
            sessionInfo.setControlDestination(session.createTopic(sessionInfo.getControlTopicName()));
            sessionInfo.setControlProducer(session.createProducer(sessionInfo.getControlDestination()));
            sessionInfo.setControlConsumer(session.createConsumer(sessionInfo.getControlDestination()));
            sessionInfo.setControlConsumerThread(new AMQControlMessageConsumer(sessionInfo.getControlProducer(), session));
            sessionInfo.getControlConsumer().setMessageListener(sessionInfo.getControlConsumerThread());
            scheduler.schedule(sessionInfo.getControlConsumerThread(), 0, TimeUnit.MILLISECONDS);

            sessionInfo.setDataTopicName(newSession.getDataStreamName());
            sessionInfo.setDataDestination(session.createTopic(sessionInfo.getDataTopicName()));
            sessionInfo.setDataProducer(session.createProducer(sessionInfo.getDataDestination()));
            sessionInfo.setDataConsumer(session.createConsumer(sessionInfo.getDataDestination()));
            sessionInfo.setDataConsumerThread(new AMQDataMessageConsumer(sessionInfo.getDataProducer(), session));
            sessionInfo.getDataConsumer().setMessageListener(sessionInfo.getDataConsumerThread());
            scheduler.schedule(sessionInfo.getDataConsumerThread(), 0, TimeUnit.MILLISECONDS);
            
            dvrService = new DVRService(context, newSession.getDatasources(), session, sessionInfo.getControlConsumer(), sessionInfo.getControlProducer(), sessionInfo.getDataProducer());
            dvrService.start();
        }
        catch(JMSException e)
        {
            return null;
        }
        
        sessionsInfoMap.put(sessionId, sessionInfo);
        
        return sessionInfo;
        
    }
    
    /***
     * Updates a session for data source changes
     * @param saveSession The session to apply changes from
     * @return True if success, false otherwise
     */
    public boolean updateSession(SIEVASSession saveSession)
    {
        return dvrService.updateDatasources(saveSession.getDatasources());
        
    }
    
    /***
     * Removes the session and stops the background tasks.
     * @param sessionId The session ID to remove.
     * @return True if stopped, false if it fails.
     */
    public boolean removeSession(long sessionId)
    {
        AMQSessionInfo sessionInfo = sessionsInfoMap.get(sessionId);
        if (sessionInfo!=null)
        {
            try
            {
                dvrService.stop();
                sessionInfo.getControlConsumerThread().stop();
                sessionInfo.getControlConsumer().close();
                sessionInfo.getControlProducer().close();
                sessionInfo.getDataConsumerThread().stop();
                sessionInfo.getDataConsumer().close();
                sessionInfo.getDataProducer().close();
            }
            catch(JMSException e)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Logger.getLogger(ActiveMQService.class.getName()).log(Level.SEVERE,sw.toString());
            }
            sessionsInfoMap.remove(sessionInfo.getSIEVASSessionId());
        }
        
        return true;
        
    }

    
}
