package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class HomeController {
	@FXML
	private Button btReset;
	
	private Stage stage;
	
	public void initialize() {
		File file = new File("weights.ser");
		
		if(!file.exists())
			btReset.setDisable(true);
	}
	
	@FXML
	public void trainingButtonHandler(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("CharacterTraining.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root,600,400);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		stage.setScene(scene);
		
		CharacterTrainingController controller = loader.getController();
		controller.setStage(stage);
		
		stage.setResizable(false);
		stage.show();
	}
	
	@FXML
	public void testingButtonHandler(ActionEvent event) throws FileNotFoundException, IOException, ClassNotFoundException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("CharacterTesting.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root,600,400);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		stage.setScene(scene);
		
		CharacterTestingController controller = loader.getController();
		controller.setStage(stage);
		
		stage.setResizable(false);
		stage.show();
	}
	
	@FXML
	public void resetButtonHandler(ActionEvent event) {
		File file = new File("weights.ser");
		
		if(file.exists())
			file.delete();
		
		btReset.setDisable(true);
	}
	
	@FXML
	public void loadButtonHandler(ActionEvent event) throws IOException {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Serial Files", "*.ser");
		fileChooser.getExtensionFilters().add(imageFilter);
		File weights = fileChooser.showOpenDialog(stage);
		
		if(weights != null) {
			Files.copy(weights.toPath(), Paths.get("weights.ser"), StandardCopyOption.REPLACE_EXISTING);
			btReset.setDisable(false);
		}
	}
	
	@FXML
	public void exitButtonHandler(ActionEvent e) {
		System.exit(0);
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
