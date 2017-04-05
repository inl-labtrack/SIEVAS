/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.inl.SIEVAS.entity;

import java.util.Objects;

/**
 * A command message for issuing DVR related commands
 * @author monejh
 */
public class DVRCommandMessage
{
    private DVRCommandType commandType;
    private long id;
    public double startTime;
    public double speed;

    
    /***
     * Default constructor
     */
    public DVRCommandMessage()
    {
        
    }
    
    /***
     * Constructor for command and unique id.
     * @param commandType THe type of the command
     * @param id  The ID of the request.
     */
    public DVRCommandMessage(DVRCommandType commandType, long id)
    {
        this.commandType = commandType;
        this.id = id;
    }

    
    public long getId()
    {
        return id;
    }
    
    public double getStartTime()
    {
        return startTime;
    }
    
    public void setStartTime(double startTime)
    {
        this.startTime = startTime;
    }
    
    public void setId(long id)
    {
        this.id = id;
    }

    public double getPlaySpeed()
    {
        return speed;
    }

    public void setPlaySpeed(double playSpeed)
    {
        this.speed = playSpeed;
    }
    
    public DVRCommandType getCommandType()
    {
        return commandType;
    }

    public void setCommandType(DVRCommandType commandType)
    {
        this.commandType = commandType;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.commandType);
        hash = 47 * hash + (int) (this.id ^ (this.id >>> 32));
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
        final DVRCommandMessage other = (DVRCommandMessage) obj;
        if (this.id != other.id)
        {
            return false;
        }
        if (this.commandType != other.commandType)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "DVRCommandMessage{" + "commandType=" + commandType + ", id=" + id + '}';
    }
    
    
    
}