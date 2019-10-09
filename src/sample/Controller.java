package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    private SPPClient client;
    @FXML
    private TextField commandTF;
    @FXML
    private Button searchButton;
    @FXML
    private Button sendButton;
    @FXML
    private ChoiceBox intervalCB;
    @FXML
    private ImageView logo1;
    @FXML
    private ImageView logo2;
    @FXML
    private Label searchLabel;
    @FXML
    private Label priceLabel;
    private ScheduledExecutorService bluetoothScheduler;
    private Runnable bluetoothRunnable;

    @FXML
    public void initialize(){

        final ImageView imageView = new ImageView(
                new Image("file:images/search_button.png")
        );
        imageView.setFitHeight(40);
        imageView.setFitWidth(40);
        searchButton.setGraphic(imageView);
        searchButton.setContentDisplay(ContentDisplay.LEFT);
        Platform.runLater(imageView::requestFocus);

        logo1.setImage(new Image("file:images/ytulogopng.png"));
        logo2.setImage(new Image("file:images/ytulogopng.png"));


        String[] st = { "Gündüz: 06:00-17:00", "Puant: 17:00-22:00", "Gece: 22:00-06:00"};
        /*
        ► T1 (Gündüz): 21.8 kuruş
        ► T2(Puant): 37.08 kuruş
        ► T3(Gece): 10.7 kuruş
         */

        intervalCB.setItems(FXCollections.observableArrayList(st));
        intervalCB.getSelectionModel().selectFirst();
        intervalCB.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            if(number2.intValue() == 0){
                priceLabel.setText("1 kWh için 21.8 kr");
            }
            else if(number2.intValue() == 1){
                priceLabel.setText("1 kWh için 37.08 kr");
            }
            else if(number2.intValue() == 2){
                priceLabel.setText("1 kWh için 10.7 kr");
            }
        });

        client = SPPClient.getInstance();
        bluetoothRunnable = client::read;

    }

    @FXML
    public void searchBluetooth() {
        client.scanForDevices();
        bluetoothScheduler = Executors.newSingleThreadScheduledExecutor();
        bluetoothScheduler.scheduleAtFixedRate(bluetoothRunnable, 0, 10, TimeUnit.MILLISECONDS);
        searchButton.setDisable(true);
        searchLabel.setText("Bağlantı başarılı!");
    }

    @FXML
    public void sendCommand(){
        client.write(commandTF.getText());
    }


}
