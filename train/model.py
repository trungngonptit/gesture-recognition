import tensorflow as tf
import tensorflow.keras as keras


class CNNLSTM:
    def __init__(self, n_classes=24, l2=1e-4, dropout=0.5, use_bidirectional=False, use_conv2d=False):
        self.n_classes = n_classes
        self.l2 = l2
        self.dropout = dropout
        self.use_bidirectional = use_bidirectional
        self.use_conv2d = use_conv2d

    def __call__(self, input_shape):
        sequence = keras.layers.Input(shape=input_shape)
        l2_reg = keras.regularizers.l2(self.l2)
        if not self.use_conv2d:
            x = keras.layers.Conv1D(64, 5, activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(sequence)
            x = keras.layers.Conv1D(64, 5, activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
            x = keras.layers.Conv1D(64, 5, activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
        else:
            x = keras.layers.Conv2D(64, (5, 1), strides=(1, 1), activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(sequnce)
            x = keras.layers.Conv2D(64, (5, 1), strides=(1, 1), activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
            x = keras.layers.Conv2D(64, (5, 1), strides=(1, 1), activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
            x = keras.layers.Conv2D(64, (5, 1), strides=(1, 1), activation=tf.nn.relu, kernel_regularizer=l2_reg,
                                    bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
        if not self.use_conv2d:
            x = keras.layers.Dropout(self.dropout)(x)
        if self.use_conv2d:
            temporal_shape = (x.shape[1], x.shape[2]*x.shape[3])
            x = keras.layers.Reshape(temporal_shape)(x)
        if not self.use_bidirectional:
            x = keras.layers.LSTM(128, return_sequences=True, kernel_regularizer=l2_reg,
                                  bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
        else:
            x = keras.layers.Bidirectional(keras.layers.LSTM(128, return_sequences=True, kernel_regularizer=l2_reg,
                                                             bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal()))(x)

        x = keras.layers.Dropout(self.dropout)(x)

        if not self.use_bidirectional:
            x = keras.layers.LSTM(128, kernel_regularizer=l2_reg, bias_regularizer=l2_reg,
                                  kernel_initializer=keras.initializers.Orthogonal())(x)
        else:
            x = keras.layers.Bidirectional(keras.layers.LSTM(
                128, kernel_regularizer=l2_reg, bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal()))(x)

        x = keras.layers.Dropout(self.dropout)(x)

        model_output = keras.layers.Dense(self.n_classes, activation=tf.nn.softmax, kernel_regularizer=l2_reg,
                                          bias_regularizer=l2_reg, kernel_initializer=keras.initializers.Orthogonal())(x)
        return keras.Model(inputs=sequence, outputs=model_output)


if __name__ == '__main__':
    model = CNNLSTM(n_classes=24, l2=0., dropout=0.,
                    use_bidirectional=False, use_conv2d=True)((50, 6, 1))
    print(model.summary())
