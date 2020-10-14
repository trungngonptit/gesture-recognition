from pathlib import Path
import pandas as pd
from scipy import stats
import numpy as np
import matplotlib.pyplot as plt
import os
from typing import Tuple


labels = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
          'CCWCircle', 'CWCircle',
          'Clap',
          'Move_down', 'Move_left', 'Move_right', 'Move_up',
          'Select',
          'Start_gesture',
          'Start_move_down', 'Start_move_left', 'Start_move_right', 'Start_move_up',
          'Unknown']
persons = [f'S{i+1}' for i in range(20)]
labels_dict = dict(zip(labels, range(len(labels))))
persons_dict = dict(zip(persons, range(len(persons))))
column_names = ['timestamp', 'x-axis', 'y-axis', 'z-axis', 'x1-axis',
                'y1-axis', 'z1-axis', 'x2-axis', 'y2-axis', 'z2-axis', 'activity']


def read_data_txt(file_path: str) -> pd.DataFrame:
    """
      Read watch recorded file.
    """
    data = pd.read_csv(file_path, header=None, names=column_names)
    data = data.sort_values(by="timestamp")
    data['activity'] = data['activity'].str.replace(';', '')
    data['person'] = file_path.split('\\')[-3]
    data = data.replace({"activity": labels_dict})
    data = data.replace({"person": persons_dict})
    return data


def slide_window(df: pd.DataFrame, freq: int, window_duration: int, step: int) -> Tuple[np.ndarray, np.ndarray]:
    """
      Slide window over dataframe to get input sample for neural network architecture.
    """
    window_length = freq * window_duration
    step_length = freq * step
    segments = []
    labels = []
    for i in range(0, len(df)-window_length+1, step_length):
        segment = df.loc[i:i+window_length-1, 'x-axis':'person'].values
        label = stats.mode(df.loc[i:i+window_length-1, 'activity'])[0][0]
        segments.append(segment)
        labels.append(label)
    return segments, labels


def collect_dataset(dataset_folder: str, output_folder_path: str, is_save_df: bool = False) -> None:
    """
      Collect and split dataset from root folder (dataset_folder) and save slided data into output_folder_path.
    """
	# create output folder if not exist
	os.makedirs(output_folder_path, exist_ok=True)

    # distrubute data by person
    train_size, valid_size, test_size = [12, 3, 5]
    # assert len(person) equals to sum split size
    assert train_size+valid_size+test_size == len(persons)
    train_persons = persons[:train_size]
    valid_persons = persons[train_size:train_size+valid_size]
    test_persons = persons[train_size+valid_size:]

    # initialize data
    train_x, train_y = [], []
    valid_x, valid_y = [], []
    test_x, test_y = [], []

    # determine if save split data in dataframe
    if is_save_df:
        train_df = pd.DataFrame(columns=column_names)
        valid_df = pd.DataFrame(columns=column_names)
        test_df = pd.DataFrame(columns=column_names)

    # glob all .txt files
    for path in Path(dataset_folder).rglob('*.txt'):
        # read data
        data = read_data_txt(file_path=str(path))

        # slide data with window duration and hop size in second
        segments, labels = slide_window(
            df=data, freq=25, window_duration=2, step=1)

        # put the data of the current person into the corresponding set
        person = file_path.split(os.sep)[-3]
        if person in train_persons:
            train_x += segments
            train_y += labels
            if is_save_df:
                train_df = pd.concat([train_df, data], axis=0)
        elif person in valid_persons:
            valid_x += segments
            valid_y += labels
            if is_save_df:
                valid_df = pd.concat([valid_df, data], axis=0)
        else:
            test_x += segments
            test_y += labels
            if is_save_df:
                test_df = pd.concat([test_df, data], axis=0)
    
    # save data
    if is_save_df:
        train_df.to_csv(output_folder_path+'/train_df.csv',
                        header=None, index=False)
        valid_df.to_csv(output_folder_path+'/valid_df.csv',
                        header=None, index=False)
        test_df.to_csv(output_folder_path+'/test_df.csv',
                       header=None, index=False)

    train_x, train_y = np.array(train_x), np.array(train_y)
    valid_x, valid_y = np.array(valid_x), np.array(valid_y)
    test_x, test_y = np.array(test_x), np.array(test_y)

    # check data shape
    print('train', train_x.shape, train_y.shape)
    print('valid', valid_x.shape, valid_y.shape)
    print('test', test_x.shape, test_y.shape)

    np.save(file=output_folder_path+'/train_x.npy', arr=train_x)
    np.save(file=output_folder_path+'/train_y.npy', arr=train_y)
    np.save(file=output_folder_path+'/valid_x.npy', arr=valid_x)
    np.save(file=output_folder_path+'/valid_y.npy', arr=valid_y)
    np.save(file=output_folder_path+'/test_x.npy', arr=test_x)
    np.save(file=output_folder_path+'/test_y.npy', arr=test_y)


if __name__ == '__main__':
    collect_dataset(dataset_folder='../datasets/original_dataset',
                    output_folder_path='../datasets/geshome_dataset')
