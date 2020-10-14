import numpy as np
import matplotlib.pyplot as plt

dataset = np.load('D:/hand_gesture_project/hand_gesture/scripts/datasets/all_segments.npy')
activities = dataset[:, :, -2]
classes = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
          'CCWCircle', 'CWCircle', 
          'Clap', 
          'Move_down', 'Move_left', 'Move_right', 'Move_up', 
          'Select', 
          'Start_gesture', 
          'Start_move_down', 'Start_move_left', 'Start_move_right', 'Start_move_up', 
          'Unknown']
val, cnt = np.unique(activities, return_counts=True)
plt.bar(val, cnt)
plt.xticks(val, classes, rotation=45)
plt.show()