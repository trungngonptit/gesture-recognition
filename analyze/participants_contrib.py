import numpy as np
import matplotlib.pyplot as plt

dataset = np.load('D:/hand_gesture_project/hand_gesture/datasets/ptit/all_segments.npy')
participants = dataset[:, :, -1]
classes = ['LeThiTuyet', 
          'NguyenCongHau', 
          'NguyenDucAnh', 
          'NguyenDucNam', 
          'NguyenDucThinh', 
          'NguyenDuyAnh', 
          'NguyenMinhHieu', 
          'NguyenThanhTung', 
          'NguyenVanSon', 
          'NguyenVanTinh', 
          'NguyenVietAnh', 
          'PhamHoangAnh', 
          'PhamSonHa', 
          'PhanKhanhThien', 
          'PhungMinhHoang', 
          'TranQuangKhai', 
          'TranTuanAnh', 
          'TruongQuangThai', 
          'VuVanChinh', 
          'VuVanDuc']
subjects = [f'S{i+1}' for i in range(len(classes))]
val, cnt = np.unique(participants, return_counts=True)
print(val)
plt.bar(val, cnt)
plt.xticks(val, subjects, rotation=45)
plt.show()