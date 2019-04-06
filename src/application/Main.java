package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Home.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root,600,400);
			scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
			primaryStage.setScene(scene);
			
			HomeController controller = loader.getController();
			controller.setStage(primaryStage);
			
			primaryStage.setTitle("Hand Written Character Recognition");
			primaryStage.setResizable(false);
			primaryStage.show();
			
			/*double[][][] weights = new double[2][][];
			weights[0] = new double[3][2];
			weights[0][0][0] = 1;
			weights[0][0][1] = 2;
			weights[0][1][0] = -1;
			weights[0][1][1] = 1;
			weights[0][2][0] = -1;
			weights[0][2][1] = 3;
			
			weights[1] = new double[2][3];
			weights[1][0][0] = 1;
			weights[1][0][1] = 2;
			weights[1][0][2] = 1;
			weights[1][1][0] = -1;
			weights[1][1][1] = 2;
			weights[1][1][2] = 2;
			ArrayList<Integer> thresholds = new ArrayList<Integer>();
			thresholds.add(0);
			thresholds.add(0);
			Perceptron perceptron = new Perceptron(weights, thresholds, "unitFunction", "meanAbsErrorFunction", 0.2, 100);
			double[][] inputs = new double[3][1];
			inputs[0][0] = 1;
			inputs[1][0] = 1;
			inputs[2][0] = 1;
			perceptron.train(inputs, inputs);
			perceptron.test(inputs);*/
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
