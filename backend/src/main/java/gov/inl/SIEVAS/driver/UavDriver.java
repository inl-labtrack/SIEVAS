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
package gov.inl.SIEVAS.driver;


import gov.inl.SIEVAS.common.IDriver;
import java.util.List;
import java.util.ArrayList;


import org.springframework.context.ApplicationContext;
import java.io.*;
import gov.inl.SIEVAS.entity.UavData;

import java.util.Date;
import java.util.Collection;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;


import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;
import gov.inl.SIEVAS.common.DriverOption;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 * Class to get UAV data
 * @author SZEWTG
 */
public class UavDriver  implements IDriver {
    

    private double scheduledTime = 0.0;
    private double startTime = 0.0, endTime = 5*60.0;
    String outStrLine;
    Date startDate = new Date();
    UavData nextData = new UavData();
    UavData sentData = new UavData();
    UavData initData = new UavData();    
    Boolean firstRun;
    Boolean runOnce;
    double speed;
    String path;
    
    private static final String PATH_OPTION = "path";
                

    FileInputStream fstream;
    DataInputStream in;
    BufferedReader br; 
    
    double initX = 0;
    double initY = 0;
    double initZ = 0;
    double initGRoll = 0;
    double initGPitch = 0;
    double initGYaw = 0;
    double initRoll = 0;
    double initPitch = 0;
    double initYaw = 0;

    @Override
    public List<DriverOption> getOptionList()
    {
        DriverOption option = new DriverOption(PATH_OPTION,"");
        List<DriverOption> list = new ArrayList<>();
        list.add(option);
        return list;
    }
    
    @Override
    public void init(ApplicationContext context, List<DriverOption> options)
    {
        firstRun = true;
        runOnce = true;
        
                
        //System.out.println(System.getProperty("line.separator")+System.getProperty("line.separator")+"Please enter the path for UAV text file and images:");
        //Scanner keyboard = new Scanner(System.in);
        //path = keyboard.nextLine();
        path = "";
        for(DriverOption option: options)
        {
            if (option.getOptionName().toLowerCase().equals(PATH_OPTION))
            {
                path = option.getOptionValue();
                
                System.out.println("Path " + path + System.getProperty("line.separator")+System.getProperty("line.separator"));
                
                if (!path.endsWith(File.separator))
                {   
                    path += File.separator;
                }
            }
        }
        
        Logger.getLogger(this.getClass().getName()).info("Path " + path + System.getProperty("line.separator")+System.getProperty("line.separator"));
        
        try{
            fstream = new FileInputStream(path+"new_IRC_flight_calibrated_external_camera_parameters.txt");
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));           
            
            // skip the header in the file
            br.readLine();

        }
        catch (Exception e)
        {
            Logger.getLogger(this.getClass().getName()).error("Error: " + e.getMessage());
            //init(context,options);
        }
    }

    // startTime - the time the data should start at
    // timestep - how much to skip of the data/how fast should the play back be
    // resolution - ???
    // maxResults - ???
    @Override
    public List getData(double startTime, double timestep, double resolution, long maxResults)
    {
        System.out.println("RUNNING PARSER");  
        speed = timestep;
        double currentSystemTime = System.currentTimeMillis();
        List<UavData> list = new ArrayList<>();
        System.out.println(new Date((long)getScheduledTime()).getTime()/1000);
        System.out.println(new Date((long)currentSystemTime).getTime()/1000);
        
        // if we haven't reached the scheduled time, return an empty list
        if ((getScheduledTime() > currentSystemTime))
        {
            // return empty list
            return list;
        }

        try{

            updateQueue();
            list.add(getQueuedData());
            setScheduledTime(System.currentTimeMillis() + sentData.getTtp()*1000);
            updateSentData();

        } catch (Exception y){

        }        
        
        System.out.println("Time "+" "+getQueuedData().getTime()); 
        System.out.println("Ttp "+" "+getQueuedData().getTtp());  
        return list;
    }
    
    public UavData getQueuedData()
    {
        return nextData;
    }
    
    public UavData getSentData()
    {
        return sentData;
    }
    
    public void updateSentData()
    {
        sentData = nextData;
    }
    
    private void updateQueue()
    {
        try{

            String str ;
            
            if ((str = br.readLine()) == null)
            {
                fstream = new FileInputStream(path+"new_IRC_flight_calibrated_external_camera_parameters.txt");
                in = new DataInputStream(fstream);
                br = new BufferedReader(new InputStreamReader(in));

                // skip the header in the file
                str = br.readLine();
		str = br.readLine();
            }
            
            String[] tokens = str.split(" ");

            nextData.setStep(Long.parseLong(tokens[0].replaceAll("[^0-9]", "")));

            double posX = Double.parseDouble(tokens[1]);
            double posY = Double.parseDouble(tokens[2]);
            double posZ = Double.parseDouble(tokens[3]);
            double gRoll = Double.parseDouble(tokens[4]);
            double gPitch = Double.parseDouble(tokens[5]);
            double gYaw = Double.parseDouble(tokens[6]);
            
	    if (nextData.getStep() == 666)
	    {
	        nextData.setType("init");
                initX = posX;
                initY = posY;
                initZ = posZ;
                initGRoll = gRoll;
                initGPitch = gPitch;
                initGYaw = gYaw;
	    }
	    else
	    {
		nextData.setType("data");
	    }

            nextData.setX(posX-initX);
            nextData.setY(posY-initY);
            nextData.setGRoll(gRoll-initGRoll);
            nextData.setGPitch(gPitch-initGPitch);
            nextData.setGYaw(gYaw - initGYaw);
            
            System.out.println(path + tokens[0]); 
            File file = new File(path + tokens[0]);
 
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            // Iterate through any XMP directories we may have received
            for (XmpDirectory xmpDirectory : metadata.getDirectoriesOfType(XmpDirectory.class)) {

                // Usually with metadata-extractor, you iterate a directory's tags. However XMP has
                // a complex structure with many potentially unknown properties. This doesn't map
                // well to metadata-extractor's directory-and-tag model.
                //
                // If you need to use XMP data, access the XMPMeta object directly.
                XMPMeta xmpMeta = xmpDirectory.getXMPMeta();

                XMPIterator itr = xmpMeta.iterator();

                // Iterate XMP properties
                while (itr.hasNext()) {

                    XMPPropertyInfo property = (XMPPropertyInfo) itr.next();
                    
                    if ((property.getPath() == null))
                    {
                        continue;
                    }
                                                   
                    if (property.getPath().contains("AbsoluteAltitude"))
                    {
                        nextData.setZ(Double.parseDouble(property.getValue()));
                        
                    }
                    
                    if (property.getPath().contains("FlightPitch"))
                    {
                        nextData.setPitch(Double.parseDouble(property.getValue()));
                        
                    }
                    
                    if (property.getPath().contains("FlightRoll"))
                    {
                        nextData.setRoll(Double.parseDouble(property.getValue()));
                        
                    }
                                        
                    if (property.getPath().contains("FlightYaw"))
                    {
                        nextData.setYaw(-Double.parseDouble(property.getValue()));
                        
                    }
               
                    if (property.getPath().contains("GimbalPitch"))
                    {
                        nextData.setGPitch(Double.parseDouble(property.getValue()));
                        
                    }
                    
                    if (property.getPath().contains("GimbalRoll"))
                    {
                        nextData.setGRoll(Double.parseDouble(property.getValue()));
                        
                    }
                                        
                    if (property.getPath().contains("GimbalYaw"))
                    {
                        nextData.setGYaw(Double.parseDouble(property.getValue()));
                        
                    }
                       // Print details of the property
                    System.out.println(property.getPath() + ": " + property.getValue());
                    
                    
                }
            }
          
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date date = directory.getDateOriginal();

            // See whether it has GPS data
            Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
            for (GpsDirectory gpsDirectory : gpsDirectories) {
                
                // Try to read out the location, making sure it's non-zero
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
               
                if (geoLocation != null && !geoLocation.isZero()) {
                    
                    // Add to our collection for use below
                    Deg2UTM deg = new Deg2UTM(geoLocation.getLatitude(), geoLocation.getLongitude());
                    nextData.setX(deg.Easting);
                    nextData.setY(deg.Northing);

                    break;
                }
            }

            if (runOnce || "init".equals(nextData.getType()))
            {
                startDate = date;
                nextData.setTtp(0.0);
                
                initX = nextData.getX();
                initY = nextData.getY();
                initZ = nextData.getZ();
                initGRoll = nextData.getGRoll();
                initGPitch = nextData.getGPitch();
                initGYaw = nextData.getGYaw();
                initRoll = nextData.getRoll();
                initPitch = nextData.getPitch();
                initYaw = nextData.getYaw();
            }
            else
            {
                nextData.setTtp((getDateDiff(startDate, date)/1000-sentData.getTime())/speed);
            }

	    if (nextData.getTtp() > 10.0)
	    {
		nextData.setTtp(5.0/speed);
 	    }

            
            nextData.setX(nextData.getX() - initX);
            nextData.setY(nextData.getY() - initY);         
            nextData.setZ(nextData.getZ() - initZ);
            nextData.setGRoll(nextData.getGRoll() - initGRoll);
            nextData.setGPitch(nextData.getGPitch() - initGPitch);
            nextData.setGYaw(nextData.getGYaw() - initGYaw);
            nextData.setRoll(nextData.getRoll() - initRoll);
            nextData.setPitch(nextData.getPitch() - initPitch);
            nextData.setYaw(nextData.getYaw() - initYaw);
            
            nextData.setTime((getDateDiff(startDate, date))/(1000));
            runOnce = false;
            System.out.println("initXYZ " + initX + " " + initY + " " + initZ);
            System.out.println("XYZ " + nextData.getX() + " " + nextData.getY() + " " + nextData.getZ());  
            
        } catch(Exception a){
            System.err.println("Error: " + a.getMessage());
        }          
    }
    
    
private class Deg2UTM
{
    double Easting;
    double Northing;
    int Zone;
    char Letter;
    private  Deg2UTM(double Lat,double Lon)
    {
        Zone= (int) Math.floor(Lon/6+31);
        if (Lat<-72) 
            Letter='C';
        else if (Lat<-64) 
            Letter='D';
        else if (Lat<-56)
            Letter='E';
        else if (Lat<-48)
            Letter='F';
        else if (Lat<-40)
            Letter='G';
        else if (Lat<-32)
            Letter='H';
        else if (Lat<-24)
            Letter='J';
        else if (Lat<-16)
            Letter='K';
        else if (Lat<-8) 
            Letter='L';
        else if (Lat<0)
            Letter='M';
        else if (Lat<8)  
            Letter='N';
        else if (Lat<16) 
            Letter='P';
        else if (Lat<24) 
            Letter='Q';
        else if (Lat<32) 
            Letter='R';
        else if (Lat<40) 
            Letter='S';
        else if (Lat<48) 
            Letter='T';
        else if (Lat<56) 
            Letter='U';
        else if (Lat<64) 
            Letter='V';
        else if (Lat<72) 
            Letter='W';
        else
            Letter='X';
        Easting=0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))*0.9996*6399593.62/Math.pow((1+Math.pow(0.0820944379, 2)*Math.pow(Math.cos(Lat*Math.PI/180), 2)), 0.5)*(1+ Math.pow(0.0820944379,2)/2*Math.pow((0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2)/3)+500000;
        Easting=Math.round(Easting*100)*0.01;
        Northing = (Math.atan(Math.tan(Lat*Math.PI/180)/Math.cos((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))-Lat*Math.PI/180)*0.9996*6399593.625/Math.sqrt(1+0.006739496742*Math.pow(Math.cos(Lat*Math.PI/180),2))*(1+0.006739496742/2*Math.pow(0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))/(1-Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))+0.9996*6399593.625*(Lat*Math.PI/180-0.005054622556*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+4.258201531e-05*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4-1.674057895e-07*(5*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))/3);
        if (Letter<'M')
            Northing = Northing + 10000000;
        Northing=Math.round(Northing*100)*0.01;
    }
}
/**
 * Get a diff between two dates
 * @param date1 the oldest date
 * @param date2 the newest date
 * @return the diff value, in milli
 */
    public static double getDateDiff(Date date1, Date date2) {

        return date2.getTime() - date1.getTime();
    }
    
    // get the time the next data should go out
    private double getScheduledTime()
    {
        return scheduledTime;
    }
    
    // set the next time data should go out
    private void setScheduledTime(double time)
    {
        scheduledTime = time;
    }
    
    @Override
    public double getStartTime()
    {
        return 0.0;
    }
    
    @Override
    public double getEndTime()
    {
        return endTime;
    }
    
    @Override
    public void shutdown()
    {
        
    }
    
    
}
