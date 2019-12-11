package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Controller {
    private SPPClient client;
    @FXML
    private TextField commandTF;
    @FXML
    private VBox chartVB;
    @FXML
    private VBox costVB;
    @FXML
    private TextField resTF1;
    @FXML
    private TextField resTF2;
    @FXML
    private TextField resTF3;
    @FXML
    private TextField resTF4;
    @FXML
    private TextField timeTF1;
    @FXML
    private TextField timeTF2;
    @FXML
    private TextField timeTF3;
    @FXML
    private TextField timeTF4;
    @FXML
    private CheckBox resCB1;
    @FXML
    private CheckBox resCB2;
    @FXML
    private CheckBox resCB3;
    @FXML
    private CheckBox resCB4;
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
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private NumberAxis xAxisCost;
    private NumberAxis yAxisCost;
    private LineChart<Number,Number> powerChart;
    private LineChart<Number,Number> costChart;

    private final double MAX_POWER = 1.0;

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

        client = SPPClient.getInstance(new CheckBox[]{resCB1, resCB2, resCB3, resCB4});
        bluetoothRunnable = client::read;

        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        xAxisCost = new NumberAxis();
        yAxisCost = new NumberAxis();
        xAxis.setLabel("Saat");
        yAxis.setLabel("Harcanan Enerji");
        xAxisCost.setLabel("Saat");
        yAxisCost.setLabel("Gider");
        //creating the chart
        powerChart = new LineChart<>(xAxis,yAxis);
        costChart = new LineChart<>(xAxisCost,yAxisCost);


        Platform.runLater(()->chartVB.getChildren().add(powerChart));
        Platform.runLater(()->costVB.getChildren().add(costChart));

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

        Map<Integer, Integer> times = new HashMap<>();
        times.put(1, Integer.parseInt(timeTF1.getText())/1000);
        times.put(2, Integer.parseInt(timeTF2.getText())/1000);
        times.put(3, Integer.parseInt(timeTF3.getText())/1000);
        times.put(4, Integer.parseInt(timeTF4.getText())/1000);

        Map<Integer, Double> powers = new HashMap<>();
        powers.put(1, 25*Double.parseDouble(timeTF1.getText())/(1000 * Integer.parseInt(resTF1.getText())));
        powers.put(2, 25*Double.parseDouble(timeTF2.getText())/(1000 * Integer.parseInt(resTF2.getText())));
        powers.put(3, 25*Double.parseDouble(timeTF3.getText())/(1000 * Integer.parseInt(resTF3.getText())));
        powers.put(4, 25*Double.parseDouble(timeTF4.getText())/(1000 * Integer.parseInt(resTF4.getText())));
        Map<Integer, Double> sortedPowers = powers.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        Object[] sortedKeys = sortedPowers.keySet().toArray();
        System.out.println("Times: " + times);
        System.out.println("Powers: " + sortedPowers);
        Map<Integer, Integer> startTimes = new HashMap<>();
        Map<Integer, Integer> endTimes = new HashMap<>();
        int lastStartTime = 0;
        int lastEndTime = 0;
        double totalPower = 0;
        for(int i = 0; i < 4; i++){
            Double power = sortedPowers.get(sortedKeys[3 - i]);
            totalPower += power;
            int startTime = totalPower <= MAX_POWER && totalPower != power ? lastStartTime : lastEndTime;
            int endTime = startTime + times.get(sortedKeys[3 - i]);
            startTimes.put((Integer) sortedKeys[3 - i], startTime);
            endTimes.put((Integer) sortedKeys[3 - i], endTime);
            lastStartTime = startTime;
            lastEndTime = endTime;
            totalPower = totalPower >= MAX_POWER ? power : totalPower;
        }
        System.out.println("Start Times: " + startTimes);
        System.out.println("End Times: " + endTimes);;

        String command = resTF1.getText() + "#";
        command += startTimes.get(1)*1000 + "#";
        command += endTimes.get(1)*1000 + "#";
        command += "1#";
        command += resTF2.getText() + "#";
        command += startTimes.get(2)*1000 + "#";
        command += endTimes.get(2)*1000 + "#";
        command += "1#";
        command += resTF3.getText() + "#";
        command += startTimes.get(3)*1000 + "#";
        command += endTimes.get(3)*1000 + "#";
        command += "1#";
        command += resTF4.getText() + "#";
        command += startTimes.get(4)*1000 + "#";
        command += endTimes.get(4)*1000 + "#";
        command += "1";
        client.write(command);


        powerChart.setTitle("Günlük Enerji Grafiği");
        costChart.setTitle("Günlük Gider Grafiği");
        //defining a series
        XYChart.Series series = new XYChart.Series();
        series.setName("kWh");
        XYChart.Series costSeries = new XYChart.Series();
        costSeries.setName("Kuruş");
        int lastTime = 0;
        for(int i = 1; i < 5; i++){
            if(lastTime < endTimes.get(i)){
                lastTime = endTimes.get(i);
            }
        }
        double[] powerData = new double[24];
        for (int k = 1; k < 5; k++) {
            for (int i = 0; i < 24; i++) {
                if(i >= startTimes.get(k) && i < endTimes.get(k)){
                    powerData[i] += powers.get(k);

                }
            }
        }
                /*
        ► T1 (Gündüz): 21.8 kuruş
        ► T2(Puant): 37.08 kuruş
        ► T3(Gece): 10.7 kuruş
         */

        double[] costData = new double[24];
        costData[0] = 10.7;
        for (int k = 1; k < 5; k++) {
            for (int i = 0; i < 23; i++) {
                if(i >= startTimes.get(k) && i < endTimes.get(k)){
                    if(i <= 8){
                        costData[i + 1] = costData[i] + 10.7;
                    } else if(i <= 19){
                        costData[i + 1] = costData[i] + 21.8;
                    } else {
                        costData[i + 1] = costData[i] + 37.8;
                    }

                } else {
                    costData[i + 1] = costData[i];
                }
            }
        }
        for (int i = 0; i < 24; i++) {

            if(i == endTimes.get(1) || i == endTimes.get(2) || i == endTimes.get(3) || i == endTimes.get(4)){
                series.getData().add(new XYChart.Data(i - 0.001, powerData[i - 1]));
            }
            series.getData().add(new XYChart.Data(i, powerData[i]));
        }
        for (int i = 0; i < 24; i++) {
            costSeries.getData().add(new XYChart.Data(i, costData[i]));
        }
        powerChart.getData().add(series);
        costChart.getData().add(costSeries);
    }


}
