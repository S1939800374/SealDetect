import math
import cv2
import numpy as np
import imutils
import fitz
import json

class SealDetect():

    def __init__(self) -> None:
        self.rate = 1
        self.r_learn = 60*self.rate #参数需要学习得知最小值为多少比较合适，下同
        self.e_learn = 0.8
        self.point_learn = 5000*self.rate
        self.wh_learn = 2.1
        self.w_learn = 100*self.rate
        self.img_width=int(800*self.rate)
        self.debug = False
        self.img_root = "./data/img/"
        self.pdf_root = "./data/pdf/"
        self.json_root = './data/json/'

    def update(self,rate):
        '''
        参数：rate:放大倍数
        输出：None
        函数功能：更新参数
        ''' 
        self.rate = rate
        self.r_learn = 60*self.rate #参数需要学习得知最小值为多少比较合适，下同
        self.e_learn = 0.8
        self.point_learn = 5000*self.rate
        self.wh_learn = 2.1
        self.w_learn = 100*self.rate
        self.img_width=int(800*self.rate)


    #处理pdf变为png文件
    def convet_pdf(self,pdf_name):
        '''
        参数：pdf_name：pdf文件名，根目录已指定
        输出：img_name_list：经过转换的文件名列表
        函数功能：将pdf转化为图片
        '''
        tem = pdf_name.split(".")
        file_name = tem[0]
        doc = fitz.open(self.pdf_root+pdf_name)
        i = 0
        img_name_list = []
        for page in doc:
            # 每个尺寸的缩放系数为1.3，这将为我们生成分辨率提高2.6的图像。
            # 此处若是不做设置，默认图片大小为：792X612, dpi=72
            zoom_x = 1.33333333 #(1.33333333-->1056x816)   (2-->1584x1224)
            zoom_y = 1.33333333
            mat = fitz.Matrix(zoom_x, zoom_y).prerotate(int(0))
            pix = page.get_pixmap(matrix=mat, alpha=False)
            img_name = "file_"+file_name+"_"+str(i)+".png"
            pix.save(self.img_root+img_name)#将图片写入指定的文件夹内
            img_name_list.append(img_name)
            i+=1
        return img_name_list

    def cv_show(self,name, img):
        '''
        参数：name：展示弹窗名，img：cv2图片对象
        输出：None
        函数功能：展示cv2图片对象效果
        '''
        # cv2.namedWindow(neme, cv2.WINDOW_NORMAL)
        if self.debug:
            cv2.imshow(name, img)
            cv2.waitKey(0)
            cv2.destroyAllWindows()

    def get_is_seal(self,json_name):
        '''
        参数：json_name：存储画印章json文件的文件名（根目录已给出）
        输出：bool
        函数功能：判断画面是否有印章
        '''
        with open(self.json_root+json_name, "r") as f:
            json_str = json.load(f)
        num = json_str["number"]
        if num==0:
            return False
        else:
            return True


    def draw_seal(self,json_name,img_name):
        '''
        参数：json_name：存储画印章json文件的文件名（根目录已给出），img_name：存储图片文件的文件名（根目录已给出）
        输出：None
        函数功能：绘制印章
        '''
        img = cv2.imread(self.img_root+img_name)
        #cv_show("demo0",img)
        #处理文档大小
        final_width = self.img_width
        scale = final_width/img.shape[1]
        final_height = int(img.shape[0]*scale)
        dim = (final_width,final_height)
        img = cv2.resize(img,dim,interpolation=cv2.INTER_AREA)

        with open(self.json_root+json_name, "r") as f:
            json_str = json.load(f)
            assert(img_name == json_str['file_name'])
        
        info_list = json_str['info']
        for info in info_list:
            if info['type']=="elipse":
                cv2.ellipse(
                    img=img,
                    center=info["center"],
                    axes=info["axes"],
                    angle=info["angle"],
                    startAngle=0,
                    endAngle=360,
                    color=(255,0,0),
                    thickness=3
                )
            elif info['type']=='rectangle':
                rect = info["center"],info["shape"],info["angle"]
                box = cv2.boxPoints(rect) # 获取最小外接矩形的4个顶点坐标(ps: cv2.boxPoints(rect) for OpenCV 3.x)
                box = np.int0(box)
                # 画出来
                cv2.drawContours(img, [box], 0, (255, 0, 0), 5)
                
        self.cv_show("demo",img)
        #绘制图片
        tem = img_name.split(".")
        file_name = tem[0]
        img_out = file_name+"_after.png"
        cv2.imwrite(self.img_root+img_out, img)



    def check_seal(self,img_name):
        '''
        参数：img_name：图片文件名，根目录已经指定
        输出：None
        函数功能：读取图片文件，寻找印章信息，并且返回印章信息标注json
        '''
        mask_list = []
        img = cv2.imread(self.img_root+img_name)
        #cv_show("demo0",img)
        #处理文档大小
        if int(img.shape[1]*0.8)>img.shape[0]:
            self.update(2)
        else:
            self.update(1)
        final_width = self.img_width
        scale = final_width/img.shape[1]
        final_height = int(img.shape[0]*scale)
        dim = (final_width,final_height)
        img = cv2.resize(img,dim,interpolation=cv2.INTER_AREA)
        # 处理红色章文件
        hsv_image = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        #降噪
        hsv_image = cv2.GaussianBlur(hsv_image,(5,5),1.5)
        high_range = np.array([180, 255, 255])
        low_range = np.array([156, 43, 46])
        high_range1 = np.array([10, 255, 255])
        low_range1 = np.array([0, 43, 46])
        th = cv2.inRange(hsv_image, low_range, high_range)
        th1 = cv2.inRange(hsv_image, low_range1, high_range1)
        mask_fa = None
        mask_fa = cv2.bitwise_or(th,th1,mask_fa)
        self.cv_show("demo",mask_fa)

        kernel1 = np.ones((70, 70), np.uint8)
        mask_fa = cv2.dilate(mask_fa,kernel1,1)
        kernel2 = np.ones((60, 60), np.uint8)
        mask_fa = cv2.erode(mask_fa,kernel2,1)
        # 调小膨胀腐蚀

        # 或者回头看一看原图的情况

        # 原图回头看一看
        
        #cv_show("demo0",mask_fa)
        mask_list.append(mask_fa)
        # 处理黑白文件
        # 非红色文件的识别成功率目测是比较低的

        #对于黑白照片，寻找到一个合适的hsv区间
        img_hsv = cv2.cvtColor(img, cv2.COLOR_RGB2HSV)
        #img_hsv = cv2.blur(img_hsv,(9,9),1.5)
        lower = np.array([0, 0, 100])
        upper = np.array([0, 255, 230])
        mask_t = cv2.inRange(img_hsv.copy(), lower, upper)
        kernel = np.ones((9, 9), np.uint8)
        mask_t = cv2.dilate(mask_t,kernel,1)
        mask_list.append(mask_t)
        # 需要对每个mask分别处理

        # 将所有的识别信息写入json
        info_list = []
        for mask in mask_list:
            self.cv_show("demo",mask)
            cnts = cv2.findContours(mask.copy(),
            cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE)
            cnts = imutils.grab_contours(cnts)
            for cnt in cnts:
                if cv2.contourArea(cnt) > self.point_learn:
                    epsilon = 0.01 * cv2.arcLength(cnt, True)
                    approx = cv2.approxPolyDP(cnt, epsilon, True) 
                    # 分析几何形状
                    corners = len(approx)
                    shape_type = ""
                    if corners == 3:
                        shape_type = "三角形"
                    elif corners == 4:
                        shape_type = "矩形"
                    elif 4 < corners:
                        shape_type = "圆"
                    else:
                        shape_type = "未知"
                
                    # 计算面积与周长
                    p = cv2.arcLength(cnt, True)
                    area = cv2.contourArea(cnt)
                    print("周长: %.3f, 面积: %.3f 形状: %s "% (p, area, shape_type))

                    # 对类型分别判断处理
                    if shape_type == "圆":
                        (x,y),(a,b),ang = cv2.fitEllipse(cnt)
                        e = math.sqrt(1-(min(a/b,b/a)**2))
                        r = min(a,b) #圆章
                        rm = max(a,b)
                        if r>=self.r_learn and e <= self.e_learn and rm <min(final_width,final_height)/3:
                            print(e)
                            info = {
                                'type':'elipse',
                                'center':(int(x),int(y)),
                                'axes':(int(a/2)+1,int(b/2)+1),
                                'angle':int(ang),
                                }
                            info_list.append(info)
                        else:
                            # 如果判断为圆形（实际上也可以是不规则形状，尝试使用矩形的方法将其框选出来）
                            rect= cv2.minAreaRect(cnt)
                            (x,y),(w,h),ang =rect
                            print(rect)
                            if max(w,h)/min(w,h) <self.wh_learn and max(w,h) >self.w_learn and max(w,h) <max(final_width,final_height)/3:
                                info = {
                                    'type':"rectangle",
                                    'center':(int(x),int(y)),
                                    'shape':(int(w),int(h)),
                                    'angle':int(ang),
                                }
                                info_list.append(info)
                    elif shape_type == "矩形":
                        rect= cv2.minAreaRect(cnt)
                        (x,y),(w,h),ang =rect
                        print(rect)
                        if max(w,h)/min(w,h) and max(w,h) >self.w_learn and max(w,h) <max(final_width,final_height)/3:
                            info = {
                                'type':"rectangle",
                                'center':(int(x),int(y)),
                                'shape':(int(w),int(h)),
                                'angle':int(ang),
                            }
                            info_list.append(info)
        
        # 写入json文件
        json_dict = {}
        json_dict["file_name"]=img_name
        json_dict["number"]=len(info_list)
        json_dict["info"]=info_list
        json_str = json.dumps(json_dict)
        with open(self.json_root+img_name+'.json', 'w') as json_file:
            json_file.write(json_str)

        self.draw_seal(img_name+'.json',img_name)

if __name__ == '__main__':
    sealdetect = SealDetect()
    sealdetect.debug = True
    for i in range(8):
        sealdetect.check_seal(str(i+1)+".png")
    sealdetect.check_seal("file_1_0.png")
    # 已经证明无法解决的问题：
    # 圈出来的部分和实际不匹配