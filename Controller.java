package app;
 
import javafx.fxml.FXML; 
import javafx.scene.control.Label; 
 
public class Controller { 
    @FXML 
    private Label welcomeText; 
 
    @FXML 
    protected void onButtonClick() { 
        welcomeText.setText("You clicked the button!"); 
    } 
}
