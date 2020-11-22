[toc]

# 简介

## 什么是虚拟化

在计算机中，虚拟化是一种资源管理技术，是将计算机的各种实体资源，如服务器、网络、内存及存储等，予以抽象，转换后呈现。这些资源的新虚拟部分是不受现有资源的架构方式，地域或物理组态所限制。一般所指的虚拟化资源包括计算能力和资料存储。

## 什么是Docker

一个开源的、由Go语言编写的容器技术。

### 为什么选择Docker

1.**上手快**。只需几分钟，就可以将自己的程序Docker化。Docker依赖于`copy-on-write`模型，使修改应用程序也变得非常迅速。

随后，就可以创建容器来运行应用程序了，大多数Docker容器只需不到1秒即可启动，由于去除了管理程序的开销，Docker容器拥有很高的性能，同时一台宿主机中也可以运行更多的容器，使用户尽可能充分的利用系统资源。

2.**职责的逻辑分类**。加强了开发环境和测试、生产环境的一致性。

3.**快速高效的开发生命周期**。缩短项目周期，并让程序具备可移植性、易于构建、易于协作。

4.**鼓励使用面向服务的架构**。Docker推荐单个容器只运行一个应用程序或进程，这样就形成了一个分布式的应用程序模型。

## 容器与虚拟化

容器是在操作系统层面上实现虚拟化，直接复用本地主机的操作系统。

而虚拟机则是在硬件层面实现。

与传统虚拟机相比，Docker优势体现为启动速度快、占用体积小。

## Docker组件

### Docker服务器与客户端

Docker是一个CS架构程序。Docker客户端通过守护进程与Docker容器通信（守护进程和容器会在同一台机器上运行，但不一定跟客户端在同一台机器上。

### Docker镜像与容器

用面向对象的思想来理解就是：

镜像——类

容器——对象

**定义**：

`镜像`:容器运行的模板，由一些文件通过层式结构堆叠而成。

`容器`:由镜像启动的具体的运行时。
### Registry（注册中心）

分公有和私有两种。Docker公司运行公共的Registry叫做Docker Hub。

可以在公共云上下载和分享镜像。

# 安装

## CentOS

1.脚本安装

```bash
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
```
2.手动安装
```bash
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
sudo yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2
sudo yum-config-manager \
    --add-repo \
    http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io    
yum list docker-ce --showduplicates | sort -r
```

## 添加镜像

对于使用`systemd`的系统,`/etc/docker/daemon.json` 中写入如下内容（如果文件不存在请新建该文件）：
```
{"registry-mirrors":["https://reg-mirror.qiniu.com/"]}
sudo systemctl daemon-reload
sudo systemctl restart docker
```

`确认`:
```
docker info
```

# 常用命令

## Docker级别

`启动`:
```
systemctl start docker
```

`停止`:
```
systemctl stop docker
```

`重启`:
```
systemctl restart docker
```

`查看启动状态`:
```
systemctl status docker
```

`查看版本`:
```
docker -v
```

`设置开机自启`:
```
systemctl enable docker
```

`查看概要信息`:
```
docker info
```

`查看docker的所有命令`:
```
docker --help
```

## 镜像级别

`搜索镜像`:
```
docker search image_name
```

NAME | DESCRIPTION | STARS | OFFICIAL | AOTOMATED
---|---|---|---|---
仓库名称 | 镜像描述 | 评价 | 是否官方 | 自动构建，表示该镜像由Docker Hub自动构建流程创建的

`镜像的获取`:
```
docker pull image_name:version
# 不带version默认拉取最新版本的镜像
```

`查看本地镜像`:
```
docker images
```

REPOSITORY | TAG | IMAGE ID | CREATED | SIZE
---|---|---|---|---
镜像名称 | 镜像标签 | 镜像ID | 镜像的创建日期 | 镜像大小

`删除镜像`:
```
docker rmi 镜像ID|名称

# 删除所有
docker rmi `docker images -q`
```

## 容器级别

### 容器的启动、停止、重启

`查看容器`:
```
# 正在运行
docker ps
# 查看所有
docker ps -a
```

`run容器 docker run`:

（这里的run一般是通过镜像创建容器并启动）

参数
- `-l`:表示运行容器
- `-t`:表示容器启动后会进入其命令行。加入这两个参数后，容器创建就能登录进去。即分配一个伪终端。
- `-name`:为创建的容器命名
- `-v`:添加宿主机与容器目录映射关系（前者是宿主机目录，后者是容器目录），可以使用多个`-v`添加多个目录或文件映射。推荐使用该方法共享文件。
- `-d`:创建守护式容器在后台运行（这样不会自动登录容器，在加了`-t` `-d`之后也是）
- `-p`:添加端口映射（前者宿主机端口，后者容器端口），可以使用多个`-p`添加多个端口映射。
```
# 交互式方式创建容器(自动进入容器)
docker run -it --name= containerName image_name:tag /bin/bash

# 后台守护方式创建容器
docker run -id --name= containerName image_name:tag
```

`start容器`:

（这里的启动指容器已经创建，处于停止状态，运行`docker start`命令去启动它）

```
docker start containerId
```

`停止容器`:
```
docker stop name|containerId
```

`重启容器`:
```
docker restart containerId
```

`启动已停止容器`:

```
docker start containerId
```

`进入容器内部`：
```
# 退出后容器继续运行
docker exec -it containerName /bin/bash

# 退出容器
exit
```

`文件拷贝`:
```
# 文件拷贝到容器内
docker cp file|dir containName:dir
# 容器内文件拷贝出来
docker cp containName:dir file|dir
```

`查看容器信息`:
```
docker inspect containerName|containerId

# 查看IP
docker inspect --format='{{.NetworkSettings.IPAddress}}' containerName|containerId
```

`目录挂载`:
```
docker run -id -v /userData/rabbitmq:/usr/local/userData --name=myrabbitmq rabbitmq:3-management
```

`移除容器`:
```
docker rm containerName|containerId
```

### 容器其他操作

`默认打印日志存储位置`:

```
/var/lib/docker/containers
```

### 查看打印日志

```
docker logs containerId
```
## Docker其他操作

### 迁移与备份

#### 容器保存为镜像

```
docker commit containerName image_name
```
#### 镜像备份

```
docker save -o xxx.tar image_name
```

#### 镜像恢复/迁移

```
docker load -i xxx.tar
```

### Dockerfile
由一系列命令和参数所构成的脚本，这些命令应用于基础镜像并创建一个新的镜像。


命令 | 作用
---|---
FROM image_name:tag | 定义了使用哪个基础镜像启动构建流程
MAINTAINER user_name | 声明镜像的创建者
ENV key value | 设置环境变量
RUN command | 核心部分
ADD source_dir/file dest_file/dir | 将宿主机的文件复制到容器，如果是一个压缩文件，将会在复制后自动解压
COPY source_dir/file dest_dir/file | 和ADD相似，但不自动解压压缩文件
WORKDIR path_dir | 设置工作目录

#### 步骤
1.编写Dockerfile
2.运行命令
```
docker build -t='imageName' dir_path
```

### Docker私有仓库

`作用`：公司内部使用，让内部网络环境可以共享镜像。

#### 制作步骤
1.拉取私有仓库镜像
```
docker pull registry
```
2.启动私有仓库容器
```
docker run -di --name=registry -p 5000:5000 registry
```
3.验证

访问`http://localhost:5000/v2/_catalog`,如果输出`{"repositories":[]}`表示私有仓库搭建成功并且内容为空

4.添加docker信任
```
vi /etc/docker/daemon.json
```
添加以下内容，保存退出并重启docker
```json
{"insecure-registries":["localhost:5000"]}
```

#### 镜像上传至私有仓库

```
# 给镜像打标签
docker tag image_name docker_address/image_name

#上传到私有仓库
docker push docker_address/image_name
```

## 开启远程访问(SSL)
### 制作证书
1.创建根证书RSA私钥
```
openssl genrsa -aes256 -out ca-key.pem 4096

输入秘钥的密码，两次密码一致在当前目录生成CA秘钥文件ca-key.pem
```
2.以此秘钥创建CA证书，自己给自己签发证书,自己就是CA机构,也可以交给第三方机构去签发
```
openssl req -new -x509 -days 1000 -key ca-key.pem -sha256 -subj "/CN=*" -out ca.pem

生成的ca.pem文件是CA证书
```
3.创建服务端私钥
```
openssl genrsa -out server-key.pem 4096

生成的server-key.pem文件是服务端私钥
```

4.生成服务端证书签名请求(csr即certificate signing request，里面包含公钥与服务端信息)
```
openssl req -subj "/CN=*" -sha256 -new -key server-key.pem -out server.csr

生成的server.csr文件是服务端证书
```

5.生成签名过的服务端证书
```
openssl x509 -req -days 1000 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem

生成的server.csr文件是服务端证书
```

6.生成客户私钥
```
openssl genrsa -out key.pem 4096

生成的key.pem文件就是客户私钥
```
7.生成客户端证书签名请求
```
openssl req -subj "/CN=client" -new -key key.pem -out client.csr
```
8.生成名为extfile.cnf的配置文件
```
echo extendedKeyUsage=clientAuth > extfile.cnf

生成的server.csr文件是服务端证书
```
9.生成签名过的客户端证书（期间会要求输入密码1234）：
```
openssl x509 -req -days 1000 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile.cnf
```
10.将多余的文件删除：
```
rm -rf ca.srl client.csr extfile.cnf server.csr
```
此时剩余文件
```
ca.pem	CA机构证书
ca-key.pem	根证书RSA私钥
cert.pem	客户端证书
key.pem	客户私钥
server-cert.pem	服务端证书
server-key.pem	服务端私钥
```
#### docker开启tls
```
修改/lib/systemd/system/docker.service

ExecStart=/usr/bin/dockerd --tlsverify --tlscacert=/etc/docker/ca.pem --tlscert=/etc/docker/server-cert.pem --tlskey=/etc/docker/server-key.pem -H tcp://0.0.0.0:2376 -H unix:///var/run/docker.sock

重启
systemctl daemon-reload && systemctl restart docker
```
### 客户端开启远程连接
#### windows

将ca.pem、cert.pem、key.pem放入一个文件夹中

hosts添加一行
```
192.168.121.138 docker-daemon

打开密钥所在文件夹的命令行 测试连接
docker --tlsverify --tlscacert=ca.pem --tlscert=cert.pem --tlskey=key.pem -H tcp://docker-daemon:2376 version
```
打印的消息有Client和Server，成功。

### IDEA docker插件的使用
测试成功后，使用docker插件

1.Settings -> Docker -> 新建 -> TCP Socket  -> https://docker-daemon:2376  -> 验证文件夹:刚才三个文件所在文件夹  -> 查看连接是否成功

2.Edit Configurations -> 添加Docker配置 -> DockerFile 
配置映射端口和maven命令
clean package -U -DskipTests
