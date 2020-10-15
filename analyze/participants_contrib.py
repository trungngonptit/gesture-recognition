import numpy as np
import matplotlib.pyplot as plt

dataset = np.load('../datasets/geshome_dataset/all_segments.npy')
participants = dataset[:, :, -1]
classes = [f'S{i+1}' for i in range(20)]
subjects = [f'S{i+1}' for i in range(len(classes))]
val, cnt = np.unique(participants, return_counts=True)
print(val)
plt.bar(val, cnt)
plt.xticks(val, subjects, rotation=45)
plt.show()