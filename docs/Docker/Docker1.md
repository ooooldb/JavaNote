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

`启动容器 docker run`:

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
# 退出后关闭容器
docker attach containerName /bin/bash

# 退出后容器继续运行
docker exec -it containerName /bin/bash

# 退出容器
exit
```

打印日志存储位置

```
/var/lib/docker/containers
```

开启远程访问（不推荐，安全性低)

```
vi /usr/lib/systemd/system/docker.service
ExecStart=/usr/bin/dockerd -H tcp://0.0.0.0:5492 -H unix://var/run/docker.sock
重启
systemctl daemon-reload
service docker restart
测试
curl http://localhost:5492/version
```

### 查看容器

```
# 正在运行
docker ps
# 查看所有
docker ps -a
```

### 查看打印日志

```
docker logs containerId
```