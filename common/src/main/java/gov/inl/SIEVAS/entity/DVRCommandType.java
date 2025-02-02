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
package gov.inl.SIEVAS.entity;

/**
 * Enum for commands for DVR controls
 * @author monejh
 */
public enum DVRCommandType
{
    GetStatus(1), Start(2), Stop(3), SetStart(4), SetPlaySpeed(5);

    private int value;
    
    private DVRCommandType(int value)
    {
        this.value = value;
    }
    
    public int getValue()
    {
        return this.value;
    }
    
    public static DVRCommandType getById(int value)
    {
        for(DVRCommandType type: values())
        {
            if (type.value == value)
                return type;
        }
        return null;
    }
    
    
}
