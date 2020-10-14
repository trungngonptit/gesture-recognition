import utils
from model import CNNLSTM
from tensorflow.keras.utils import plot_model
from sklearn.metrics import f1_score, precision_score, recall_score, confusion_matrix, classification_report
from sklearn.utils import class_weight
import itertools
import matplotlib.pyplot as plt
import numpy as np
import tensorflow as tf
keras = tf.keras


# Load data
dataset_folder = '../datasets/geshome_dataset'
train_x = np.load(file=dataset_folder + '/train_x.npy')
train_y = np.load(file=dataset_folder + '/train_y.npy')

valid_x = np.load(file=dataset_folder + '/valid_x.npy')
valid_y = np.load(file=dataset_folder + '/valid_y.npy')
test_x = np.load(file=dataset_folder + '/test_x.npy')
test_y = np.load(file=dataset_folder + '/test_y.npy')

train_y = keras.utils.to_categorical(train_y, 24)
valid_y = keras.utils.to_categorical(valid_y, 24)
test_y = keras.utils.to_categorical(test_y, 24)

print('train', train_x.shape, train_y.shape)
print('valid', valid_x.shape, valid_y.shape)
print('test', test_x.shape, test_y.shape)

# Define and start training model
model = CNNLSTM(n_classes=24, use_bidirectional=True)(input_shape=(50, 6))
print(model.summary())
model.compile(loss=keras.losses.categorical_crossentropy,
              optimizer=keras.optimizers.Adam(learning_rate=1e-4), metrics=['accuracy'])

history = model.fit(x=train_x, y=train_y, batch_size=128,
                    epochs=20, shuffle=True, validation_data=(valid_x, valid_y))
utils.plot_history(history)

# Evaluate model on test set
classes = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
           'CCWCircle', 'CWCircle',
           'Clap',
           'Move_down', 'Move_left', 'Move_right', 'Move_up',
           'Select',
           'Start_gesture',
           'Start_move_down', 'Start_move_left', 'Start_move_right', 'Start_move_up',
           'Unknown'
           ]
test_z = np.argmax(model.predict(test_x), axis=1)
print(f1_score(np.argmax(test_y, axis=1), test_z, average="weighted"))
print(f1_score(np.argmax(test_y, axis=1), test_z, average="macro"))
print(classification_report(np.argmax(test_y, axis=1), test_z, target_names=classes))
utils.plot_confusion_matrix(y_true=np.argmax(test_y, axis=1), y_pred=test_z,
                            classes=classes, normalize=False, title='unnormalized Confusion matrix')
utils.plot_confusion_matrix(y_true=np.argmax(test_y, axis=1), y_pred=test_z,
                            classes=classes, normalize=True, title='normalized Confusion matrix')


# Save keras model in HDF5 format
keras.models.save_model(model, 'model.h5')

# converter = tf.lite.TFLiteConverter.from_keras_model_file(keras_model) # TF 1.x
model = keras.models.load_model("model.h5")
converter = tf.lite.TFLiteConverter.from_keras_model(model)  # TF 2.x
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.EXPERIMENTAL_TFLITE_BUILTINS_ACTIVATIONS_INT16_WEIGHTS_INT8]
tflite_model = converter.convert()
open('model.tflite', "wb").write(tflite_model)

tflite = tf.lite.Interpreter("model.tflite")
tflite.allocate_tensors()
input_details = tflite.get_input_details()
output_details = tflite.get_output_details()
print(input_details)
print(output_details)
