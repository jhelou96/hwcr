package application;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CharacterTestingController {
	@FXML
	private Button btClearConfig;
	@FXML
	private Button btTest;
	@FXML
	private VBox vbConfig;
	@FXML
	private AnchorPane apDrawingArea;
	@FXML
	private ScrollPane spTestingResults;
	
	private Stage stage;
	private File fileToBeProcessed;
	
	public CharacterTestingController() throws Exception {
	}
	
	@FXML
	public void browseButtonHandler(ActionEvent event) throws IOException {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png");
		fileChooser.getExtensionFilters().add(imageFilter);
		fileToBeProcessed = fileChooser.showOpenDialog(stage);
		
		if(fileToBeProcessed == null)
			return;
		
		// Replace drawing area with loaded image
		vbConfig.getChildren().remove(1);
		vbConfig.getChildren().add(1, new ImageView(resize(new Image(fileToBeProcessed.toURI().toString()), 335, 300)));

		btTest.setDisable(false);
		btClearConfig.setDisable(false);
	}
	
	@FXML
	public void drawingAreaMouseDraggedHandler(MouseEvent event) {
		btTest.setDisable(false);
		btClearConfig.setDisable(false);
		
		double x = event.getX();
		double y = event.getY();
		
		Bounds bounds = apDrawingArea.getLayoutBounds();
		
		if(bounds.contains(x, y)) {
			Circle newCircle = new Circle(event.getX(), event.getY(), 6);
			apDrawingArea.getChildren().add(newCircle);
		}
	}
	
	@FXML
	public void clearConfig() {
		btTest.setDisable(true);
		btClearConfig.setDisable(true);
		
		apDrawingArea.getChildren().clear();
		vbConfig.getChildren().set(1, apDrawingArea);
		
		fileToBeProcessed = null;
		
		spTestingResults.setContent(null);
	}
	
	@FXML
	public void goBackButtonHandler(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Home.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root,600,400);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		stage.setScene(scene);
		
		HomeController controller = loader.getController();
		controller.setStage(stage);
		
		stage.setResizable(false);
		stage.show();
	}
	
	@FXML 
	public void testButtonHandler(ActionEvent event) throws Exception {
		if(vbConfig.getChildren().get(1) == apDrawingArea) {
			WritableImage image = apDrawingArea.snapshot(new SnapshotParameters(), null);
			fileToBeProcessed = new File("./resources/images/samples/" + new Timestamp(System.currentTimeMillis()).getTime() + ".png");
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", fileToBeProcessed);
		}
		
		Image processedImage = processFile(fileToBeProcessed);
		
		ZernikeMoments zm = new ZernikeMoments();
		ProjectionHistograms ph = new ProjectionHistograms(true);
		HuMoments hm = new HuMoments();
		double[] projectionHistograms = ph.extractFeatures(SwingFXUtils.fromFXImage(processedImage, null));
		double[] zernikes = zm.extractFeatures(SwingFXUtils.fromFXImage(processedImage, null));
		//double[] geometrics = hm.extract((application.HuMomentsHelper.Image) processedImage);
		
		ImageIO.write(SwingFXUtils.fromFXImage(processedImage, null), "png", new File("zoning.png"));
		ZoneBasedFeature zone = new ZoneBasedFeature(new File("zoning.png"));
		zone.extract();
		ArrayList<Double> extractedZones = zone.getFeature();
		double[] zones = new double[extractedZones.size()];
		for(int i = 0; i < zones.length; i++)
			zones[i] = (double) extractedZones.get(i);
		
		double[][] inputs = arrayTo2D(ArrayUtils.addAll(ArrayUtils.addAll(projectionHistograms, zernikes), zones));
		
		print(zernikes);
		print(projectionHistograms);
		print(zones);
		
		//double[][] inputs = imageToDouble(processedImage);

		ArrayList<Integer> thresholds = new ArrayList<Integer>();
		thresholds.add((int) (inputs.length * 0.8));
		Perceptron perceptron = new Perceptron(thresholds, "unitFunction", "meanSquErrorFunction", 0, 100);
		
		double[] outputsProbabilities = perceptron.test(inputs);
		
		GridPane gpOutputsProbabilities = new GridPane();
		gpOutputsProbabilities.setVgap(10);
		gpOutputsProbabilities.setHgap(10);
		gpOutputsProbabilities.setAlignment(Pos.CENTER);
		gpOutputsProbabilities.setPadding(new Insets(10, 10, 10, 10));
		gpOutputsProbabilities.setPrefWidth(spTestingResults.getWidth());
		
		// Get max probability to identify the character
		double maxProb = 0;
		for(int i = 0; i < outputsProbabilities.length; i++) {
			if(outputsProbabilities[i] > maxProb)
				maxProb = outputsProbabilities[i];
		}
		
		// Display results
		for(int i = 0; i < outputsProbabilities.length; i++) {
			// gpOutputsProbabilities.add(new Text((char) (65 + (i < 26 ? i : i + 6)) + ""), 0, i);
			gpOutputsProbabilities.add(new Text((char) (97 + i) + ""), 0, i);
			
			ProgressBar pbOutputProbability = new ProgressBar();
			pbOutputProbability.setPrefWidth(gpOutputsProbabilities.getPrefWidth());
			pbOutputProbability.setProgress(outputsProbabilities[i]);
			
			gpOutputsProbabilities.add(pbOutputProbability, 1, i);
			
			if(maxProb == outputsProbabilities[i]) {
				Image check = new Image("/images/check.png");
				gpOutputsProbabilities.add(new ImageView(check), 2, i);
			}
		}
		spTestingResults.setContent(gpOutputsProbabilities);
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	private Image resize(Image image, int scaledWidth, int scaledHeight) throws IOException {
		ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File("resizing.png"));
		Image resized = new Image(new BufferedInputStream(new FileInputStream("resizing.png")), scaledWidth,
				scaledHeight, false, false);
		
		Files.delete(Paths.get("resizing.png"));

		return resized;
	}
	
	private void print(double[] array) {
		for(int i = 0; i < array.length; i++)
			System.out.println(array[i]);
		
		System.out.println();
	}
	
	private Image processFile(File file) throws IOException {
		BufferedImage img = ImageIO.read(file);
		BufferedImage BWImage = BlackWhite(img, 128);
		Image bounding = minBlobDetection(SwingFXUtils.toFXImage(BWImage, null));
		
		return resize(bounding, 180, 180);
	}
	
	private BufferedImage BlackWhite(BufferedImage image, int threshold) {
	    BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
	    result.getGraphics().drawImage(image, 0, 0, null);
	    WritableRaster raster = result.getRaster();
	    int[] pixels = new int[image.getWidth()];
	    for (int y = 0; y < image.getHeight(); y++) {
	        raster.getPixels(0, y, image.getWidth(), 1, pixels);
	        for (int i = 0; i < pixels.length; i++) {
	            if (pixels[i] < threshold) pixels[i] = 0;
	            else pixels[i] = 255;
	        }
	        raster.setPixels(0, y, image.getWidth(), 1, pixels);
	    }
	    return result;
	}

	private Image minBlobDetection(Image original) {
		// last update: fix the distance threshold
		PixelReader pixelReader = original.getPixelReader();
		WritableImage writableImage = new WritableImage(original.getPixelReader(), (int) original.getWidth(),
				(int) original.getHeight());
		PixelWriter pixelWriter = writableImage.getPixelWriter();
		
		ArrayList<Blob> blobs = new ArrayList<Blob>();
		ArrayList<Blob> blobs_to_remove = new ArrayList<Blob>();
		ArrayList<Blob> blobs_min = new ArrayList<Blob>();
		Blob chosen = null;
		
		// First eliminate small blobs (noise)
		for (int x = 0; x < original.getWidth(); x++) {
			for (int y = 0; y < original.getHeight(); y++) {
				// Current pixel color
				double r1 = pixelReader.getColor(x, y).getRed();
				double g1 = pixelReader.getColor(x, y).getGreen();
				double b1 = pixelReader.getColor(x, y).getBlue();

				// Only if a black pixel is detected
				if ((r1 + g1 + b1) / 3 == 0.0) {
					boolean found = false;
					for (Blob b : blobs) {
						if (b.isNear(x, y)) {
							b.add(x, y);
							found = true;
							break;
						}
					}
					if (!found) {
						Blob b = new Blob(x, y);
						// Threshold to remove small dots
						b.setDistThreshold(20);
						//System.out.println("width/2: " + b.getDistThreshold());
						blobs.add(b);
					}

				}

			}
		}
		
		//blobs with area less than 15
		for (Blob b : blobs) {
			if(b.size()<=15)
				blobs_to_remove.add(b);
		}
		
		
		//Removing small blobs with white pixels
		for (Blob b : blobs_to_remove) {
			//System.out.println("blob size: " + b.size());
			for (int x = 0; x < original.getWidth(); x++) {
				for (int y = 0; y < original.getHeight(); y++) {
					if(x>=b.minx && x<=b.maxx && y>=b.miny && y<=b.maxy)
						pixelWriter.setColor(x, y, Color.WHITE);
				}
			}
		}
		
		for (int x = 0; x < original.getWidth(); x++) {
			for (int y = 0; y < original.getHeight(); y++) {
				// Current pixel color
				double r1 = pixelReader.getColor(x, y).getRed();
				double g1 = pixelReader.getColor(x, y).getGreen();
				double b1 = pixelReader.getColor(x, y).getBlue();

				// Only if a black pixel is detected
				if ((r1 + g1 + b1) / 3 == 0.0) {
					boolean found = false;
					for (Blob b : blobs_min) {
						if (b.isNear(x, y)) {
							b.add(x, y);
							found = true;
							break;
						}
					}
					if (!found) {
						Blob b = new Blob(x, y);
						// Threshold to detect signature bounds dynamically
						b.setDistThreshold(Math.sqrt(Math.pow(original.getWidth()/2, 2) + Math.pow(original.getHeight()/2, 2))/2);
						//System.out.println("width/2: " + b.getDistThreshold());
						blobs_min.add(b);
					}
				}
			}
		}
		double maxSize = 0;
		for (Blob b : blobs_min) {
			if(b.size()>maxSize) {
				maxSize = b.size();
				chosen = b;
			}
		}
		
		int minx = (int) chosen.minx, miny = (int) chosen.miny, maxx = (int) chosen.maxx, maxy = (int) chosen.maxy;
		Image result = new WritableImage(writableImage.getPixelReader(), minx, miny, maxx - minx, maxy - miny);
		return result;
	}
	
	private double[][] imageToDouble(Image image) {
		int imageWidth = (int) image.getWidth();
		int imageHeight = (int) image.getHeight();
		
		WritableImage writableImage = new WritableImage(image.getPixelReader(), imageWidth, imageHeight);
		PixelReader pixelReader = writableImage.getPixelReader();
		
		double[][] result = new double[imageWidth*imageHeight][1];
		int pos = 0;
		for (int i = 0; i < imageWidth; i++) {
			for (int j = 0; j < imageHeight; j++) {
				double r1 = pixelReader.getColor(i, j).getRed();
				double g1 = pixelReader.getColor(i, j).getGreen();
				double b1 = pixelReader.getColor(i, j).getBlue();

				//result has 1 if black, and 0 if white
				if ((r1 + g1 + b1) / 3 == 0.0) { 
					result[pos][0] = 1.0;
				} else {
					result[pos][0] = 0.0;
				}
				pos++;
			}
		}
		return result;
	}
	
	private double[][] arrayTo2D(double[] array) {
		double[][] array2D = new double[array.length][1];
		for(int i = 0; i < array.length; i++)
			array2D[i][0] = array[i];
		
		return array2D;
	}
}
