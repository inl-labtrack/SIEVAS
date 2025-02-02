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

import java.util.Objects;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;

/**
 * Basic session info for ActiveMQ.
 * @author monejh
 */
public class AMQSessionInfo
{
    private Long SIEVASSessionId;
    private String controlTopicName;
    private Destination controlDestination;
    private MessageConsumer controlConsumer;
    private MessageProducer controlProducer;
    private AMQControlMessageConsumer controlConsumerThread;
    
    private String dataTopicName;
    private Destination dataDestination;
    private MessageConsumer dataConsumer;
    private MessageProducer dataProducer;
    private AMQDataMessageConsumer dataConsumerThread;

    public AMQSessionInfo(Long SIEVASSessionId)
    {
        this.SIEVASSessionId = SIEVASSessionId;
    }

    

    public Long getSIEVASSessionId()
    {
        return SIEVASSessionId;
    }

    public void setSIEVASSessionId(Long SIEVASSessionId)
    {
        this.SIEVASSessionId = SIEVASSessionId;
    }

    
    
    
    public String getControlTopicName()
    {
        return controlTopicName;
    }

    public void setControlTopicName(String controlTopicName)
    {
        this.controlTopicName = controlTopicName;
    }

    public Destination getControlDestination()
    {
        return controlDestination;
    }

    public void setControlDestination(Destination controlDestination)
    {
        this.controlDestination = controlDestination;
    }

    public MessageConsumer getControlConsumer()
    {
        return controlConsumer;
    }

    public void setControlConsumer(MessageConsumer controlConsumer)
    {
        this.controlConsumer = controlConsumer;
    }

    public MessageProducer getControlProducer()
    {
        return controlProducer;
    }

    public void setControlProducer(MessageProducer controlProducer)
    {
        this.controlProducer = controlProducer;
    }

    public String getDataTopicName()
    {
        return dataTopicName;
    }

    public void setDataTopicName(String dataTopicName)
    {
        this.dataTopicName = dataTopicName;
    }

    public Destination getDataDestination()
    {
        return dataDestination;
    }

    public void setDataDestination(Destination dataDestination)
    {
        this.dataDestination = dataDestination;
    }

    public MessageConsumer getDataConsumer()
    {
        return dataConsumer;
    }

    public void setDataConsumer(MessageConsumer dataConsumer)
    {
        this.dataConsumer = dataConsumer;
    }

    public MessageProducer getDataProducer()
    {
        return dataProducer;
    }

    public void setDataProducer(MessageProducer dataProducer)
    {
        this.dataProducer = dataProducer;
    }

    public AMQControlMessageConsumer getControlConsumerThread()
    {
        return controlConsumerThread;
    }

    public void setControlConsumerThread(AMQControlMessageConsumer controlConsumerThread)
    {
        this.controlConsumerThread = controlConsumerThread;
    }

    public AMQDataMessageConsumer getDataConsumerThread()
    {
        return dataConsumerThread;
    }

    public void setDataConsumerThread(AMQDataMessageConsumer dataConsumerThread)
    {
        this.dataConsumerThread = dataConsumerThread;
    }
    
    

    @Override
    public int hashCode()
    {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final AMQSessionInfo other = (AMQSessionInfo) obj;
        if (!Objects.equals(this.SIEVASSessionId, other.SIEVASSessionId))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "AMQSessionInfo{" + "SIEVASSessionId=" + SIEVASSessionId + ", controlTopicName=" + controlTopicName + ", controlDestination=" + controlDestination + ", controlConsumer=" + controlConsumer + ", controlProducer=" + controlProducer + ", dataTopicName=" + dataTopicName + ", dataDestination=" + dataDestination + ", dataConsumer=" + dataConsumer + ", dataProducer=" + dataProducer + '}';
    }
    
    
    
    
    
    
}
