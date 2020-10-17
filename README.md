# Gesture Recognition Using Wearable Sensors With Convolutional Neural Bi-Long Short TermMemory Networks 
## GesHome Dataset Description
We collect the dataset, named GesHome, consisting of 18 hand gestures from 20 non-professional subjects with various ages and occupation. The participant performed 50 times for each gesture in 5 days. Thus, GesHome consists of 18000 gesture samples in total. Using embedded accelerometer and gyroscope, we take 3-axial linear acceleration and 3-axial angular velocity with frequency equals to 25Hz. The experiments have been video-recorded to label the data manually using [ELan tool](https://archive.mpi.nl/tla/elan).

The collected files include columns in the order of ```{datetime, accelerometer x, accelerometer y, accelerometer z, gyroscope x, gyroscope y, gyroscope z, magnetometer x, magnetometer y, magnetometer z, activity label}```, datetime column is set as format ```"yyyy-MM-dd;HH:mm:ss.SSS"```, while the label column format is string.

## Pre-Processing
The dataset splitted into 3 sets by personalized session, one set contains 12 subjects, used for training. The other containing 3 subjects is used for validation and the remainder is used for testing.
We slide windows on each session with 2 seconds length (50 samples) and overlap equal to 50% of the window size (slide 1 second between 2 consecutive patterns). The label of each window is the label for the majority of all the samples in corresponding window.
