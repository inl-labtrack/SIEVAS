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
package gov.inl.SIEVAS.adminconsole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.inl.SIEVAS.common.JsonListResult;
import gov.inl.SIEVAS.entity.SIEVASSession;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

/**
 * Handles the presenting of the session list on SIEVAS server.
 * @author monejh
 */
public class SessionListController implements Initializable
{
    public static String sessionListURL = "api/sessions/";
    
    private String baseURL = "";
    private RestController restController;
    
    @FXML
    private TableView tableView;
    
    /***
     * Loads the data once all the parameters are set. This requires baseURL
     * and RestController to be set.
     */
    public void LoadData()
    {
       
        HttpGet getRequest = new HttpGet(baseURL + sessionListURL);
        HttpClient client = restController.getClient();
        HttpResponse response = null;
        try
        {
            response = client.execute(getRequest);
        }
        catch(IOException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Data loading error");
            alert.setHeaderText("Load session error");
            alert.setContentText(e.getMessage());

            alert.showAndWait();
            return;
        }
        evaluateSessions(response);
    }
    
    /***
     * Handles the response of the session list
     * @param response  The HTTP response
     */
    private void evaluateSessions(HttpResponse response)
    {
        try
        {
            ObjectMapper objMapper = new ObjectMapper();
            //read result as a JsonListResult<SIEVASSession>
            //NOTE: users and groups are missing in the local object since it is not needed
            JsonListResult<SIEVASSession> result = objMapper.readValue(response.getEntity().getContent(), new TypeReference<JsonListResult<SIEVASSession> >(){});
            tableView.setItems(FXCollections.observableArrayList(result.getData()));
        }
        catch (IOException ex)
        {
            Logger.getLogger(SessionListController.class.getName()).log(Level.SEVERE, null, ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Data loading error");
            alert.setHeaderText("Load session error");
            alert.setContentText(ex.getMessage());

            alert.showAndWait();
        }
    }
    
    /***
     * Handles the mouse click on a session object by loading DVR controls.
     * @param session The session object
     * @throws IOException 
     */
    private void handleMouseClick(SIEVASSession session) throws IOException
    {
        if (session!=null)
        {
            System.out.println("Clicked " + session.getId() + ":" + session.getName());
            //success of session selection, show DVR Controls
            Stage stage = (Stage) tableView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DVR.fxml"));
            Parent root = loader.load();
            DVRController controller = loader.getController();

            controller.setRestController(restController);
            controller.setBaseURL(baseURL);
            controller.setSession(session);
            controller.Load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add("/styles/Styles.css");

            stage.setTitle("SIEVAS DVR Controls - Session " + session.getName());
            stage.setScene(scene);
            stage.show();
        }
            
        
    }
    
    /***
     * Sets the base URL for the server
     * @param url 
     */
    public void setBaseURL(String url)
    {
        this.baseURL = url;
    }
    
    /***
     * Sets the REST Controller for sharing login context
     * @param rest 
     */
    public void setRestController(RestController rest)
    {
        this.restController = rest;
    }
    
    /***
     * Handles init of view. Handles a double clock on a row.
     * @param location
     * @param resources 
     */
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        tableView.setRowFactory(tv -> {
            TableRow row = new TableRow();
            row.setOnMouseClicked(event -> {
                if ((event.getClickCount() == 2) && (!row.isEmpty()))
                {
                    try
                    {
                        this.handleMouseClick((SIEVASSession)row.getItem());
                    }
                    catch(IOException e)
                    {
                        Logger.getLogger(DVRController.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            });
            return row;
        });
        
    }
    
}
