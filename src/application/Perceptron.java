package application;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import Jama.Matrix;

public class Perceptron {
	private ArrayList<Matrix> weights;
	private ArrayList<Integer> thresholds;
	private String activationFunction;
	private String errorFunction;
	private double errorThreshold;
	private int maxIterations;
	
	private Matrix givenInputMatrix;
	private Matrix givenOutputMatrix;
	private ArrayList<Matrix> actualOutputs;
	private double[] actualOutputsProbabilities;
	private ArrayList<Matrix> tempWeightMatrices;
	
	public Perceptron(double[][][] weights, ArrayList<Integer> thresholds, String activationFunction, 
			String errorFunction, double errorThreshold, int maxIterations) {
		this.thresholds = thresholds;
		this.activationFunction = activationFunction;
		this.errorFunction = errorFunction;
		this.errorThreshold = errorThreshold;
		this.maxIterations = maxIterations;
		
		this.weights = new ArrayList<Matrix>();
		for(int i = 0; i < weights.length; i++)
			this.weights.add(new Matrix(weights[i]));
	}
	
	@SuppressWarnings("unchecked")
	public Perceptron(ArrayList<Integer> thresholds, String activationFunction, String errorFunction, double errorThreshold, int maxIterations) throws Exception {
		this.thresholds = thresholds;
		this.activationFunction = activationFunction;
		this.errorFunction = errorFunction;
		this.errorThreshold = errorThreshold;
		this.maxIterations = maxIterations;
		
		// Get the weights from the serializable file
		ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File("weights.ser")));	
		try {
			while(true) {
				this.weights = (ArrayList<Matrix>) input.readObject();;
			}
		} catch(EOFException e) {
			input.close();
			return;
		}
	}
	
	public void train(double[][] inputs, double[][] outputs) {
		givenInputMatrix = new Matrix(inputs);
		givenOutputMatrix = new Matrix(outputs);
		
		// Compute actual output and update weights accordingly
		for (int i = 0; i < maxIterations; i++) {
			computeActualOutputs(null, 0);

			double meanError = computeMeanError();
			if (meanError > errorThreshold)
				updateWeights(null, weights.size() - 1);
			else
				break;
		}
		
		try {
			ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File("weights.ser")));
			output.writeObject(weights);
			output.close();
		} catch(Exception e) {
			System.err.println("Saving weights failed!");
		}
	}
	
	public double[] test(double[][] inputs) {
		givenInputMatrix = new Matrix(inputs);
		computeActualOutputs(null, 0);
		
		return actualOutputsProbabilities;
	}
	
	/**
	 * Computes the actual outputs based on the training inputs provided by the user
	 * Recursive method that goes through all the layers
	 * @param matrix
	 * @param i
	 */
	private void computeActualOutputs(Matrix matrix, int i) {
		if (matrix == null || i == 0) {
			matrix = givenInputMatrix;
			actualOutputs = new ArrayList<Matrix>();
		}

		Matrix weight = weights.get(i);

		Matrix computedMatrix = weight.transpose().times(matrix);
		Matrix thresholdMatrix = new Matrix(computedMatrix.getRowDimension(), computedMatrix.getColumnDimension(), thresholds.get(i));
		computedMatrix = computedMatrix.minus(thresholdMatrix);
		double[][] computedMatrixTo2DArray = computedMatrix.getArray();

		// Compute activation function
		double[][] actualOutput = new double[computedMatrixTo2DArray.length][computedMatrixTo2DArray[0].length];
		if (activationFunction.equals("unitFunction")) {
			for (int j = 0; j < computedMatrixTo2DArray.length; j++) {
				for (int k = 0; k < computedMatrixTo2DArray[j].length; k++) {
					if (computedMatrixTo2DArray[j][k] > 0) {
						actualOutput[j][k] = 1;
						
					} else {
						actualOutput[j][k] = 0;
					}
				}
			}
		} else if (activationFunction.equals("linearFunction")) {
			for (int j = 0; j < computedMatrixTo2DArray.length; j++) {
				for (int k = 0; k < computedMatrixTo2DArray[j].length; k++) {
					if (computedMatrixTo2DArray[j][k] < -1)
						actualOutput[j][k] = 0;
					else if (computedMatrixTo2DArray[j][k] > 1)
						actualOutput[j][k] = 1;
					else
						actualOutput[j][k] = (computedMatrixTo2DArray[j][k] + 1) / 2;
				}
			}
		} else if (activationFunction.equals("identityFunction")) {
			for (int j = 0; j < computedMatrixTo2DArray.length; j++) {
				for (int k = 0; k < computedMatrixTo2DArray[j].length; k++) {
					actualOutput[j][k] = computedMatrixTo2DArray[j][k];
				}
			}
		} else if (activationFunction.equals("sigmoidFunction")) {
			for (int j = 0; j < computedMatrixTo2DArray.length; j++) {
				for (int k = 0; k < computedMatrixTo2DArray[j].length; k++) {
					actualOutput[j][k] = (Math.exp(computedMatrixTo2DArray[j][k]) - 1)
							/ (Math.exp(computedMatrixTo2DArray[j][k]) + 1);
				}
			}
		} else if (activationFunction.equals("gaussianFunction")) {
			for (int j = 0; j < computedMatrixTo2DArray.length; j++) {
				for (int k = 0; k < computedMatrixTo2DArray[j].length; k++) {
					actualOutput[j][k] = Math.exp(Math.pow(computedMatrixTo2DArray[j][k] - 1, 2));
				}
			}
		}

		actualOutputs.add(new Matrix(actualOutput)); // Save the output of each iteration
		
		// Save the probabilities of the last iteration
		if(activationFunction.equals("unitFunction") && i == (weights.size() - 1)) {
			actualOutputsProbabilities = new double[computedMatrixTo2DArray.length];
			
			// Calculate the sum of all positive values
			int sumPosVals = 0;
			for(int j = 0; j < computedMatrixTo2DArray.length; j++) {
				if(computedMatrixTo2DArray[j][0] > 0)
					sumPosVals += computedMatrixTo2DArray[j][0];
			}
			// Calculate the probabilities for all values
			for(int j = 0; j < computedMatrixTo2DArray.length; j++) {
				if(computedMatrixTo2DArray[j][0] > 0)
					actualOutputsProbabilities[j] = computedMatrixTo2DArray[j][0] / sumPosVals;
				else
					actualOutputsProbabilities[j] = 0;
			}
		}

		// Jump to next layer
		if (i + 1 < weights.size())
			computeActualOutputs(new Matrix(actualOutput), i + 1);
	}
	
	/**
	 * Computes the mean error between the given and actual outputs based on the
	 * error function selected
	 * 
	 * @return mean error
	 */
	private double computeMeanError() {
		double[][] actualOutput = actualOutputs.get(actualOutputs.size() - 1).getArray(); // Get last training output matrix as 2D array
		double[][] givenOutput = givenOutputMatrix.getArray();

		double summation = 0;
		for (int i = 0; i < actualOutput.length; i++) {
			if (errorFunction.equals("meanDifErrorFunction"))
				summation += givenOutput[i][0] - actualOutput[i][0];
			else if (errorFunction.equals("meanAbsErrorFunction"))
				summation += Math.abs(givenOutput[i][0] - actualOutput[i][0]);
			else if (errorFunction.equals("meanSquErrorFunction"))
				summation += Math.pow(givenOutput[i][0] - actualOutput[i][0], 2);
		}

		return (summation / actualOutput.length);
	}
	
	/**
	 * Updates the weights
	 * @param prevErr
	 * @param i
	 */
	private void updateWeights(Matrix prevErr, int i) {
		Matrix err;
		if (prevErr == null) { //First iteration
			tempWeightMatrices = new ArrayList<>(weights); // Holds original weights provided by the user --> Used to compute err
			err = givenOutputMatrix.minus(actualOutputs.get(actualOutputs.size() - 1));
		} else
			err = tempWeightMatrices.get(i + 1).times(prevErr);

		Matrix delta;
		if (i > 0)
			delta = err.times(actualOutputs.get(i - 1).transpose()).transpose();
		else
			delta = err.times(givenInputMatrix.transpose()).transpose();

		weights.set(i, weights.get(i).plus(delta));

		if (i > 0)
			updateWeights(err, i - 1);

		return;
	}
	
	/**
	 * Converts a matrix to a string
	 * @param matrix
	 * @return
	 */
	private String matrixToString(Matrix matrix) {
		StringBuilder matrixAsString = new StringBuilder("");
		double[][] matrixTo2DArray = matrix.getArray();
		for (int i = 0; i < matrixTo2DArray.length; i++) {
			matrixAsString.append("|\t");
			for (int j = 0; j < matrixTo2DArray[i].length; j++) {
				matrixAsString.append(String.format("%-+8.2f", matrixTo2DArray[i][j]));
			}
			matrixAsString.append("\t|\n");
		}

		return matrixAsString.toString().substring(0, matrixAsString.toString().length() - 1);
	}
	
}
