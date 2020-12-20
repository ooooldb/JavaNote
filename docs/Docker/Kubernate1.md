[toc]

# 资料
[中文文档][1]
# 简介
Kubernetes的名字来自希腊语，意思是“舵手” 或 “领航员”。K8s是将8个字母“ubernete”替换为“8”的缩写。由Google在2014年创建管理的，是内部使用Borg的开源版本。

Kubernetes是容器集群管理系统，是一个开源的平台，可以实现容器集群的自动化部署、自动扩缩容、维护等功能。

通过Kubernetes可以：

- 快速部署应用
- 快速扩展应用
- 无缝对接新的应用功能
- 节省资源，优化硬件资源的使用

k8s的目标是促进完善组件和工具的生态系统，以减轻应用程序在公有云或私有云中运行的负担。

## 特点
- 可移植: 支持公有云，私有云，混合云，多重云（multi-cloud）
- 可扩展: 模块化, 插件化, 可挂载, 可组合
- 自动化: 自动部署，自动重启，自动复制，自动伸缩/扩展

## Why containers?

传统的应用部署方式让应用的运行、配置、管理、所有生存周期将与当前操作系统绑定，不利于应用的升级更新/回滚等操作，而虚拟机则非常重，并不利于可移植性。

新的方式是通过部署容器方式实现，每个容器之间互相隔离，每个容器有自己的文件系统 ，容器之间进程不会相互影响，能区分计算资源。

容器优势总结：

- **快速创建/部署应用**：与VM虚拟机相比，容器镜像的创建更加容易。
- **持续开发、集成和部署**：提供可靠且频繁的容器镜像构建/部署，并使用快速和简单的回滚(由于镜像不可变性)。
- **开发和运行相分离**：在build或者release阶段创建容器镜像，使得应用和基础设施解耦。
- **开发，测试和生产环境一致性**：在本地或外网（生产环境）运行的一致性。
- **云平台或其他操作系统**：可以在 Ubuntu、RHEL、 CoreOS、on-prem、Google Container Engine或其它任何环境中运行。
- **Loosely coupled**，分布式，弹性，微服务化：应用程序分为更小的、独立的部件，可以动态部署和管理。
- **资源隔离**
- **资源利用：**更高效

## 使用Kubernetes能做什么？

可以在物理或虚拟机的Kubernetes集群上运行容器化应用，Kubernetes能提供一个以“**容器为中心的基础架构**”，满足在生产环境中运行应用的一些常见需求，如：

- 多个进程（作为容器运行）协同工作。（Pod）
- 存储系统挂载
- Distributing secrets
- 应用健康检测
- 应用实例的复制
- Pod自动伸缩/扩展
- Naming and discovering
- 负载均衡
- 滚动更新
- 资源监控
- 日志访问
- 调试应用程序
- 提供认证和授权

## 组件

K8s集群所需的一些二进制组件。

这些组件是：

- Master组件
    - kube-apiserver（暴露Kubernetes API）
    - ETCD（保存集群数据）
    - kube-controller-manager（集群中处理常规任务的后台线程）
    - cloud-controller-manager（负责与底层云提供商的平台交互）
    - kube-scheduler（为新创建Pod选择Node）
    - 插件 addons（实现集群pod和Services功能）
    - DNS（为Kubernetes services提供 DNS记录）
    - 用户界面（集群状态查看）
    - 容器资源监测（监控数据查看）
    - Cluster-level Logging（容器日志）
- Node组件
    - kubelet（主要的节点代理）
    - kube-proxy（维护网络规则并执行连接转发）
    - docker（运行容器）
    - RKT（docker工具的替代方案）
    - supervisord（监控系统）
    - fluentd（守护进程）

## 对象

Kubernetes对象是Kubernetes系统中的持久实体。Kubernetes使用这些实体来表示集群的状态。

具体来说，他们可以描述：

- 容器化应用正在运行(以及在哪些节点上)
- 这些应用可用的资源
- 关于这些应用如何运行的策略，如重新策略，升级和容错


    要使用Kubernetes对象（无论是创建，修改还是删除），都需要使用Kubernetes API。例如，当使用kubectl命令管理工具时，CLI会为提供Kubernetes API调用。

### 描述K8s对象

在Kubernetes中创建对象时，必须提供描述其所需Status的对象Spec，以及关于对象（如name）的一些基本信息。

通常，可以将信息提供给kubectl .yaml文件，在进行API请求时，kubectl将信息转换为JSON。

### 必填字段

- apiVersion - 创建对象的Kubernetes API 版本
- kind - 要创建什么样的对象？
- metadata- 具有唯一标示对象的数据，包括 name（字符串）、UID和Namespace（可选项）
- 对象Spec字段，对象Spec的精确格式（对于每个Kubernetes 对象都是不同的），以及容器内嵌套的特定于该对象的字段。Kubernetes API reference可以查找所有可创建Kubernetes对象的Spec格式。

## K8s Node

Node是Kubernetes中的工作节点，最开始被称为minion。一个Node可以是VM或物理机。每个Node（节点）具有运行pod的一些必要服务，并由Master组件进行管理，Node节点上的服务包括Docker、kubelet和kube-proxy。


## K8s Pod

Pod是Kubernetes创建或部署的最小/最简单的基本单位，一个Pod代表集群上正在运行的一个进程。

Pod代表部署的一个单位：Kubernetes中单个应用的实例，它可能由单个容器或多个容器共享组成的资源。

> Docker是Kubernetes Pod中最常见的runtime ，Pods也支持其他容器runtimes。

Kubernetes中的Pod使用可分两种主要方式：

- Pod中运行一个容器。“one-container-per-Pod”模式是Kubernetes最常见的用法; 在这种情况下，你可以将Pod视为单个封装的容器，但是Kubernetes是直接管理Pod而不是容器。
- Pods中运行多个需要一起工作的容器。Pod可以封装紧密耦合的应用，它们需要由多个容器组成，它们之间能够共享资源，这些容器可以形成一个单一的内部service单位 - 一个容器共享文件，另一个“sidecar”容器来更新这些文件。Pod将这些容器的存储资源作为一个实体来管理。

每个Pod都是运行应用的单个实例，如果需要水平扩展应用（例如，运行多个实例），则应该使用多个Pods，每个实例一个Pod。在Kubernetes中，这样通常称为Replication。Replication的Pod通常由Controller创建和管理。

## K8s Volume

默认情况下容器中的磁盘文件是非持久化的，对于运行在容器中的应用来说面临两个**问题**:
1. 当容器挂掉kubelet将重启启动它时，文件将会丢失。
2. 当Pod中同时运行多个容器，容器之间需要共享文件时。

### Volume类型
- emptyDir（默认，生命周期跟随Pod）
- hostPath（Node级别的文件系统挂载）
- gcePersistentDisk
- awsElasticBlockStore
- nfs（网络文件系统，可以永久保存）
- iscsi
- fc (fibre channel)
- flocker
- glusterfs
- rbd
- cephfs（CephFS Volume挂载）
- gitRepo（git代码下拉到指定的容器路径中）
- secret
- persistentVolumeClaim
- downwardAPI
- projected
- azureFileVolume
- azureDisk
- vsphereVolume
- Quobyte
- PortworxVolume
- ScaleIO
- StorageOS
- local（每个节点的本地存储）

[1]: http://docs.kubernetes.org.cn/227.html      "kubernetes文档"