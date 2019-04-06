# Hand-Written Character Recognition
A JAVA application that implements an Artificial Neural Network agent to recognize hand-written characters.

The ANN agent is a multilayer perceptron that takes as input an image of a hand-written character and identifies to which letter of the english alphabet it resembles the most.

## How it works
Once a hand-written character image is submitted, it needs some pre-processing before it is fed to the perceptron.

* It is first converted to black&white. This is done by iterating through each pixel of the image and calculated the average of the RGB values. If it happens to be be above 128, the pixel is considered as white; othewise, it is considered as black.

* Minimum bounded rectangle and blob detection

* Resizing the image to 180x180

* Feature extraction: Feeding the perceptron with pixels RGB values as input is not accurate at all. Instead, we combine multiple features together and use them as input. The features extracted are the following: Zernike moments, geometric moment invariants, zoning and histogram projection.

### Training phase
During the training phase, the user has to provide a training set which is made of the following:

* A handwritten character representing an english letter (input). The handwritten character can be either provided as an image or can be directly drawn from the GUI.
* The english letter the handwritten character represents (output)

As many training sets as needed can be provided.

### Testing phase
During the testing phase, the user has to provide a handwritten character and the program will output for each character of the alphabet the probability that it matched with the handwritten character.

## Demo
