# -*- coding:utf-8

import socket
import datetime


def gen_photo_name():
    dt_now = datetime.datetime.now()
    
    name = str(dt_now.year) + str(dt_now.month) + str(dt_now.day) + str(dt_now.hour) + str(dt_now.minute) + str(dt_now.second) + '.bmp'
    return name


def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("192.168.0.9", 80))
        s.listen(10)
        while True:
            """
            if input() == 'bye':
                s.close()
            """
            data_all =b''
            #client, _ = s.accept() #原因
            #with client:
               # クライアントの接続受付
            sock_cl, addr = s.accept()
            
            while True:
                # ソケットから byte 形式でデータ受信
                data = sock_cl.recv(4096)
                if len(data) <= 0:
                    break
                #print('受信中')
                data_all += data
            
            #画像ファイル名生成
            photo = gen_photo_name()
            with open('bmp/'+photo, 'wb') as f:
                # ファイルにデータ書込
                f.write(data_all)
            print('受信完了')
            # クライアントのソケットを閉じる
            sock_cl.close()
            print('正常終了')
            


if __name__ == "__main__":
    main()