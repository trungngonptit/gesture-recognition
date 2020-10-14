import numpy as np
import matplotlib.pyplot as plt
from sklearn.metrics import confusion_matrix


def plot_history(history) -> None:
    """
        Plot training curve includes accuracy and loss curve
    """
    plt.figure(figsize=(10, 6))
    plt.plot(history.history['accuracy'])
    plt.plot(history.history['val_accuracy'])
    plt.title('model accuracy')
    plt.ylabel('accuracy')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    plt.show()
    plt.figure(figsize=(10, 6))
    plt.plot(history.history['loss'])
    plt.plot(history.history['val_loss'])
    plt.title('model loss')
    plt.ylabel('loss')
    plt.xlabel('epoch')
    plt.legend(['train', 'test'], loc='upper left')
    plt.show()


def plot_confusion_matrix(y_true, y_pred, classes,
                          normalize=True, figure_path=None,
                          cmap=plt.cm.Blues,
                          figsize=(14, 14),
                          title="Confusion matrix") -> None:
    """
        Plot confusion matrix in recall metric (Sum row equals to 100% if it is normalized)
    """
    # Compute confusion matrix
    cm = confusion_matrix(y_true, y_pred)
    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
    fig, ax = plt.subplots(figsize=figsize)
    im = ax.imshow(cm, interpolation='nearest', cmap=cmap)
    # ax.figure.colorbar(im, ax=ax)
    # We want to show all ticks...
    ax.set(
        xticks=np.arange(len(classes)),
        yticks=np.arange(len(classes)),
        # ... and label them with the respective list entries
        xticklabels=classes,
        yticklabels=classes,
        ylabel='True labels',
        xlabel='Predicted labels'
    )
    # Rotate the tick labels and set their alignment.
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
             rotation_mode="anchor")
    # Loop over data dimensions and create text annotations.
    fmt = '.2f' if normalize else 'd'
    thresh = cm.max() / 2.
    for i in range(cm.shape[0]):
        for j in range(cm.shape[1]):
            ax.text(j, i, format(cm[i, j], fmt),
                    ha="center", va="center",
                    color="white" if cm[i, j] > thresh else "black")
    plt.ylim(len(classes) - 0.5, -0.5)
    fig.tight_layout()
    if figure_path is not None:
        fig.savefig(figure_path, bbox_inches='tight')
    plt.show()
