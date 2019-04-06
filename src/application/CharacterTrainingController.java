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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CharacterTrainingController {
	@FXML
	private TextField tfChar;
	@FXML
	private VBox vbImages;
	@FXML
	private Button btPrevImage;
	@FXML
	private Button btNextImage;
	@FXML
	private TextField tfImageNumber;
	@FXML
	private AnchorPane apDrawingArea;
	@FXML
	private Button btSaveConfig;
	@FXML
	private Button btTrain;
	@FXML
	private Button btClearImages;
	@FXML
	private Button btClearConfig;
	@FXML
	private VBox vbConfig;
	@FXML
	private ProgressBar pbTrainingProgress;
	@FXML
	private Button btBrowseFile;
	
	private Stage stage;
	private 	ArrayList<Image> processedImages;
	private HashMap<Character, ArrayList<Image>> mappedProcessedImages;
	private int currentImageIndex = -1;
	private List<File> filesBrowsed;
	private File fileDrawn;
	private HashMap<Character, Integer> supportedChars;
	
	public CharacterTrainingController() throws Exception {
		processedImages = new ArrayList<Image>();
		mappedProcessedImages = new HashMap<Character, ArrayList<Image>>();
		
		supportedChars = new HashMap<Character, Integer>();
		/* for(int i = 0; i < 26; i++)
			supportedChars.put((char) (i + 65), i); */
		for(int i = 0; i < 26; i++)
			supportedChars.put((char) (i + 97), i);
	}
	
	@FXML
	public void browseButtonHandler() throws IOException {
		final FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png");
		fileChooser.getExtensionFilters().add(imageFilter);
		filesBrowsed = fileChooser.showOpenMultipleDialog(stage);
		
		if(filesBrowsed == null)
			return;
		
		btBrowseFile.setText("Browse (" + filesBrowsed.size() + ")");
		
		btSaveConfig.setDisable(false);
		btClearConfig.setDisable(false);
	}
	
	@FXML
	public void imageIndexButtonHandler(ActionEvent event) {
		if(event.getSource() == btPrevImage && currentImageIndex > 0)
			currentImageIndex--;
		else if(event.getSource() == btNextImage && currentImageIndex < (processedImages.size() - 1))
			currentImageIndex++;
		else
			return;

		tfImageNumber.setText((currentImageIndex + 1) + "/" + processedImages.size());
		vbImages.getChildren().set(0, new ImageView(processedImages.get(currentImageIndex)));
	}
	
	@FXML
	public void drawingAreaMouseDraggedHandler(MouseEvent event) {
		btSaveConfig.setDisable(false);
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
	public void trainButtonHandler(ActionEvent event) throws IOException {
		btBrowseFile.setDisable(true);
		btClearConfig.setDisable(true);
		btSaveConfig.setDisable(true);
		btClearImages.setDisable(true);
		btTrain.setDisable(true);
		apDrawingArea.setDisable(true);
		btNextImage.setDisable(true);
		btPrevImage.setDisable(true);
		tfChar.setDisable(true);
		
		currentImageIndex = processedImages.size() - 1;
		vbImages.getChildren().set(0, new ImageView(processedImages.get(currentImageIndex)));
		tfImageNumber.setText((currentImageIndex + 1) + "/" + processedImages.size());
		
		Task<Void> task = new Task<Void>() {
            @Override 
            public Void call() {
            		Set<Entry<Character, ArrayList<Image>>> set = mappedProcessedImages.entrySet();
	            	set.forEach(entry -> {
	            		try {
		        			char letter = entry.getKey();
		        			ArrayList<Image> images = entry.getValue();
		        			
		        			for(Image image : images) {
		        				ZernikeMoments zm = new ZernikeMoments();
		        				ProjectionHistograms ph = new ProjectionHistograms(true);
		        				HuMoments hm = new HuMoments();
		        				
							ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File("zoning.png"));
							ZoneBasedFeature zone = new ZoneBasedFeature(new File("zoning.png"));
							zone.extract();
							ArrayList<Double> extractedZones = zone.getFeature();
							double[] zones = new double[extractedZones.size()];
							for(int i = 0; i < zones.length; i++)
								zones[i] = (double) extractedZones.get(i);
		        				
		        				double[] projectionHistograms = ph.extractFeatures(SwingFXUtils.fromFXImage(image, null));
		        				double[] zernikes = zm.extractFeatures(SwingFXUtils.fromFXImage(image, null));
		        				// double[] geometrics = hm.extract((application.HuMomentsHelper.Image) image);
		        				
		        				double[][] inputs = arrayTo2D(ArrayUtils.addAll(ArrayUtils.addAll(projectionHistograms, zernikes), zones));
		        				
		        				// double[][] inputs = imageToDouble(image);
		        				double[][] outputs = new double[supportedChars.size()][1];
		        				outputs[supportedChars.get(letter)][0] = 1;
		        				
		        				print(zernikes);
		        				print(projectionHistograms);
		        				print(zones);
		        				
		        				Perceptron perceptron = null;
		        				ArrayList<Integer> thresholds = new ArrayList<Integer>();
		        				thresholds.add((int) (inputs.length * 0.8));
		        				File weightsFile = new File("weights.ser");
		        				if(!weightsFile.exists()) {
		        					double[][][] weights = new double[1][][];
		        					weights[0] = fillRandom(inputs.length, 26);
		        					
		        					perceptron = new Perceptron(weights, thresholds, "unitFunction", "meanSquErrorFunction", 0, 100);
		        				} else {
		        					try {
	    								perceptron = new Perceptron(thresholds, "unitFunction", "meanSquErrorFunction", 0, 100);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		        				}
		        				
		        				perceptron.train(inputs, outputs);
		        				
		        				Platform.runLater(new Runnable() {
		        	                @Override
		        	                public void run() {
		        	                		if(currentImageIndex == 0)
		        	                			clearImagesButtonHandler();
		        	                		else {
		    	        						processedImages.remove(currentImageIndex);
		    	        						currentImageIndex--;
			        	                		tfImageNumber.setText((currentImageIndex + 1) + "/" + processedImages.size());
										vbImages.getChildren().set(0, new ImageView(processedImages.get(currentImageIndex)));
		        	                		}
		        	                }
		        	            });
		        			}
	            		} catch(Exception e) {
	            			e.printStackTrace();
	            		}
	        		});
	            	
	            	return null;
            }
        };
        task.setOnSucceeded(e -> {
        		pbTrainingProgress.progressProperty().unbind();
        		pbTrainingProgress.setProgress(100);
        		
        		btBrowseFile.setDisable(false);
        		apDrawingArea.setDisable(false);
        		tfChar.setDisable(false);
        		
        		clearImagesButtonHandler();
        });
        
        new Thread(task).start();
        pbTrainingProgress.progressProperty().bind(task.progressProperty());
	}
	
	@FXML
	public void saveConfigButtonHandler(ActionEvent event) throws IOException {
		try {
			if(tfChar.getText().length() != 1)
				throw new Exception("Character provided is invalid!");
			
			if(!Character.isLetter(tfChar.getText().charAt(0)))
				throw new Exception("Character provided is invalid!");
		} catch(Exception e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error Dialog");
			alert.setContentText(e.getMessage());

			alert.showAndWait();
			
			return;
		}
		
		// If user is drawing
		if(apDrawingArea.getChildren().size() > 0) {
			WritableImage image = apDrawingArea.snapshot(new SnapshotParameters(), null);
			fileDrawn = new File("./resources/images/samples/" + new Timestamp(System.currentTimeMillis()).getTime() + ".png");
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", fileDrawn);
			
			processFile(fileDrawn, tfChar.getText().charAt(0));
		}
		
		if(filesBrowsed != null) {
			for(File file : filesBrowsed)
				processFile(file, tfChar.getText().charAt(0));
		}
		
		clearConfig();
	}
	
	@FXML
	public void clearImagesButtonHandler() {
		mappedProcessedImages.clear();
		processedImages.clear();
		btPrevImage.setDisable(true);
		btNextImage.setDisable(true);
		btClearImages.setDisable(true);
		btTrain.setDisable(true);
		currentImageIndex = -1;
		tfImageNumber.setText("0");
		
		vbImages.getChildren().remove(0);
		AnchorPane ap = new AnchorPane();
		ap.setPrefHeight(180);
		ap.setPrefWidth(180);
		ap.setMaxWidth(180);
		ap.setStyle("-fx-background-color: white");
		vbImages.getChildren().add(0, ap);
	}
	
	@FXML
	public void clearConfig() {
		btSaveConfig.setDisable(true);
		btClearConfig.setDisable(true);
		
		tfChar.setText("");
		
		apDrawingArea.getChildren().clear();
		vbConfig.getChildren().set(1, apDrawingArea);
		
		fileDrawn = null;
		filesBrowsed = null;
		
		btBrowseFile.setText("Browse");
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
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	private void processFile(File file, char letter) throws IOException {
		BufferedImage img = ImageIO.read(file);
		BufferedImage BWImage = BlackWhite(img, 128);
		Image bounding = minBlobDetection(SwingFXUtils.toFXImage(BWImage, null));
		Image resized = resize(bounding, 180, 180);
		
		processedImages.add(resized);
		
		if(mappedProcessedImages.get(letter) == null)
			mappedProcessedImages.put(letter, new ArrayList<Image>());
		mappedProcessedImages.get(letter).add(resized);
		
		btPrevImage.setDisable(false);
		btNextImage.setDisable(false);
		btTrain.setDisable(false);
		btClearImages.setDisable(false);
		currentImageIndex = processedImages.size() - 1;
		tfImageNumber.setText((currentImageIndex + 1) + "/" + processedImages.size());
		
		vbImages.getChildren().set(0, new ImageView(processedImages.get(currentImageIndex)));
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
	
	public Image resize(Image image, int scaledWidth, int scaledHeight) throws IOException {
		ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File("resizing.png"));
		Image resized = new Image(new BufferedInputStream(new FileInputStream("resizing.png")), scaledWidth,
				scaledHeight, false, false);

		Files.delete(Paths.get("resizing.png"));
		
		return resized;
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
	
	private double[][] fillRandom(int width, int height) {
		Random rnd = new Random();
		double[][] output = new double[width][height];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
					output[i][j] = (double) rnd.nextInt(100);
			}
		}
		return output;
	}
	
	private void print(double[] array) {
		for(int i = 0; i < array.length; i++)
			System.out.println(array[i]);
		
		System.out.println();
	}
}
