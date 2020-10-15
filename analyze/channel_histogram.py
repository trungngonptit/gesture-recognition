import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

df = pd.read_csv('../datasets/geshome_dataset/all_df.csv')
print(df.shape)

titles = ['acc_x', 'acc_y', 'acc_z', 'gyr_x', 'gyr_y', 'gyr_z']
# plt.figure(figsize=(15, 15))
for index in range(3):
  plt.subplot(2, 3, index+1)
  values = df.values[:, index+1]
  plt.title(titles[index])
  plt.hist(values.astype(float))
for index in range(3, 6):
  plt.subplot(2, 3, index+1)
  values = df.values[:, index+1]
  plt.title(titles[index])
  plt.hist(values.astype(float))
plt.show()