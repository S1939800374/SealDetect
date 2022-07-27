from flask import Flask, jsonify,request, send_file
import seal
from werkzeug.utils import secure_filename
import os

sealdetect = seal.SealDetect()
sealdetect.debug = False

app = Flask(__name__)
@app.route('/download/<filename>',methods=['GET'])
def download(filename):
    return send_file(sealdetect.img_root+filename)

@app.route('/upload',methods=["post"])
def upload():
    img_name_list = []
    for filename in request.files:
        print(filename)
        file = request.files.get(filename)
        savename = secure_filename(file.filename)
        print(savename)
        if filename.startswith("img"):
            file.save(os.path.join(sealdetect.img_root, savename))
            img_name_list.append(savename)
        elif filename.startswith("pdf"):
            pdfpath = os.path.join(sealdetect.pdf_root, savename)
            file.save(pdfpath)
            #同时进行文件向图片的转化
            img_name_list+=sealdetect.convet_pdf(savename)
    # 处理图片
    res_list = []
    for name in img_name_list:
        info = {}
        sealdetect.check_seal(name)
        tem = name.split(".")
        file_name = tem[0]
        img_out = file_name+"_after.png"
        info['name'] = img_out
        info['hasseal']=sealdetect.get_is_seal(name+'.json')
        res_list.append(info)
        
    return jsonify({"data":res_list})


if __name__ == '__main__':
    app.run(host="0.0.0.0",port=8000)