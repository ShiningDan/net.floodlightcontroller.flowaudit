
# net.floodlightcontroller.flowaudit

## 项目介绍

本项目实现的是在 Floodlight 下，对第三方请求的流表进行：

1. 主动环路检测
2. 主动黑洞检测
3. 被动黑洞检测

## 安装方法

首先，下载本项目的代码：

```
git clone https://github.com/ShiningDan/net.floodlightcontroller.flowaudit.git
```

将 GitHub 下载的外部库导入到 Refrenced Libraeies 中：

目前涉及到的外部库有 `org.json.jar`，将外部库拷贝到 `<floodlight-root>/lib/` 下面

在 eclipse 下面的  Refrenced Libraeies 右键，选择 Build Path，然后点击 Configure Build Path。

在打开的窗口中，点击 Add JARs，选择 `floodlight/lib/org.json.jar` ，点击 OK，如下图：

![](http://ojt6zsxg2.bkt.clouddn.com/b89d9a53721831475b33d89f5d8ad809.png)

然后，在 floodlight 中创建一个 `flowaudit` 的 package。

然后将项目中的代码拷贝进去即可，如下图所示。

![](http://ojt6zsxg2.bkt.clouddn.com/7c8ead08dfb7866e7493a6ac99523215.png)

接着，修改 `src/main/resource` 目录下的 `floodlightdefault.properties`，添加 `flowaudit`：

```
floodlight.modules=\
net.floodlightcontroller.jython.JythonDebugInterface,\
net.floodlightcontroller.storage.memory.MemoryStorageSource,\
net.floodlightcontroller.core.internal.FloodlightProvider,\
net.floodlightcontroller.threadpool.ThreadPool,\
net.floodlightcontroller.debugcounter.DebugCounterServiceImpl,\
net.floodlightcontroller.perfmon.PktInProcessingTime,\
net.floodlightcontroller.debugevent.DebugEventService,\
net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher,\
net.floodlightcontroller.restserv![](http://ojt6zsxg2.bkt.clouddn.com/7c8ead08dfb7866e7493a6ac99523215.png)er.RestApiServer,\
net.floodlightcontroller.topology.TopologyManager,\
net.floodlightcontroller.forwarding.Forwarding,\
net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager,\
net.floodlightcontroller.ui.web.StaticWebRoutable,\
net.floodlightcontroller.loadbalancer.LoadBalancer,\
net.floodlightcontroller.firewall.Firewall,\
net.floodlightcontroller.devicemanager.internal.DeviceManagerImpl,\
net.floodlightcontroller.accesscontrollist.ACL,\
net.floodlightcontroller.statistics.StatisticsCollector,\
net.floodlightcontroller.flowaudit.StartFlowAudit
```

最后，修改 `src/main/resource/META-INF/services` 下的 `net.floodlightcontroller.core.module.IFloodlightModule`，将 `flowaudit` 中的 `StartFlowAudit` 文件添加到项目模块启动列表中。

```
net.floodlightcontroller.flowaudit.StartFlowAudit
```

`StartFlowAudit.java` 的作用是将封装好的主动环路审计、主动黑洞审计和被动黑洞审计的 REST API 在 floodlight 启动时提供访问。

在做完了以上的步骤以后，重新启动 floodlight，就可以提供审计的 REST API 接口了。

## 使用方法

### 模块代码介绍

在介绍使用方法以前，我们先介绍模块相关的代码。

之前，在安装模块的时候，我将 `StartFlowAudit.java` 模块设置为启动 floodlight 时启动，在 `StartFlowAudit.java` 中其实只做了一项工作，就是启动 `FlowAuditRoutable.java`，来提供审计功能的 REST API。

我们可以看一下 `FlowAuditRoutable.java` 中的内容：

```
@Override
public Restlet getRestlet(Context context) {
	Router router = new Router(context);
	router.attach("/backhole/active/json", BlackholeActiveAudit.class);
	router.attach("/backhole/passive/json", BlackholePassiveDetect.class);
	router.attach("/loop/active/json", LoopActiveAudit.class);
	return router;
}

@Override
public String basePath() {
	return "/flow/audit";
}
```

可以看到，`FlowAuditRoutable.java` 调用了三个 Java 文件来实现三种功能，其分别为：

1. `BlackholeActiveAudit.class`：路由黑洞的主动检测，其请求 URL 为 `http://localhost:8080/flow/audit/backhole/active/json`
2. `BlackholePassiveDetect.class`：路由黑洞的被动检测，其请求 URL 为 `http://localhost:8080/flow/audit/backhole/passive/json`
3. `LoopActiveAudit.class`：路由环路的主动检测，其请求 URL 为 `http://localhost:8080/flow/audit/loop/active/json`

### 如何发送主动请求

我们涉及到的主动检测有：路由环路的主动检测和路由黑洞的主动检测，这两种检测方法都需要发送 JSON 请求。

在 Ubuntu 中发送 HTTP 请求，可以使用 FireFox 的 HttpRequester 工具，当然，我个人觉得使用 Chrome 的 Postman 更好用。

在设置 HttpRequester 的时候，只需要设置 URL，Method(POST、GET)、Content type: `application/json`，最后将 JSON 格式的填写在 Content 里面，如：

```
{
    flowentries: [
        {
             dpid: "00:00:00:00:00:00:00:01", 
             match: {
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=2"
        },
        {
             dpid: "00:00:00:00:00:00:00:02", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
        {
             dpid: "00:00:00:00:00:00:00:03", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
        {
             dpid: "00:00:00:00:00:00:00:04", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        }
    ]
}
```

### 环路的主动审计

环流的主动审计，需要发送 REST 请求到 `http://localhost:8080/flow/audit/loop/active/json`，其余的设置方法参照上一节。

环路的主动审计，可能包含多种情况：

1. 发送的请求本身就包含环路
2. 发送的请求和网络中已有的流表组成了一条环路

在第二种情况（发送的请求和网络中已有的流表组成了一条环路）中，可能是已有的流表优先级比较高，匹配到了第三方请求带来的数据流；也有可能是第三方请求的优先级比较高，影响到了交换机已有的流量，当然也有可能是两种情况的混杂。下面，我针对几种简单的情况设计一下：

#### 发送的请求本身包含环路

首先，我们启动 floodlight，然后使用 mininet 创建一个网络。

```
 sudo mn --custom /home/floodlight/mininet/topo/4-sw-4-h-loop.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6653 --mac --switch=ovsk,protocols=OpenFlow13 --link tc
```

`4-sw-4-h-loop.py` 这个脚本创建了一个简单的 4 交换机 4 主机的网络，每个交换机上有一个主机，四个交换机以 `1-2-3-4-1` 的形式成环。

因为请求本身包含环路，所以我们不用预先给交换机下发流表，而是使用 HTTPRequest 来发送 JSON 格式的第三方请求：

首先设置 HTTPRequest：

设置 URL：

```
http://localhost:8080/flow/audit/loop/active/json
```

设置 Content type：

```
application/json
```

最后设置第三方请求的内容：

```
{
    flowentries: [
        {
             dpid: "00:00:00:00:00:00:00:01", 
             match: {
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=2"
        },
        {
             dpid: "00:00:00:00:00:00:00:02", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
        {
             dpid: "00:00:00:00:00:00:00:03", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
        {
             dpid: "00:00:00:00:00:00:00:04", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        }
    ]
}
```

这个第三方请求本身就是一个环路，使用 POST 方法发送数据后，可以通过 Console 和 返回的数据得到环路的信息。

#### 发送的请求和已有的流表组成环路

首先，我们启动 floodlight，然后使用 mininet 创建一个网络。

```
 sudo mn --custom /home/floodlight/mininet/topo/4-sw-4-h-loop.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6653 --mac --switch=ovsk,protocols=OpenFlow13 --link tc
```

然后在交换机中生成已有的一些流表，这些流表我写在了脚本文件 `before-loop2.sh` 中，我们可以查看其中的流表内容：

```
ovs-ofctl -O OpenFlow13 add-flow s3 in_port=*,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s4 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s1 in_port=3,ip,nw_src=10.0.0.0/24,priority:65535,action=output:2
```

这个流表大致做的事情，是满足匹配域的流量，发送路径为：`3->4->1`

使用命令 

```
sudo sh ./before-loop2.sh 
```

运行该脚本，给交换机添加流表。

然后设置 HTTPRequest：

设置 URL：

```
http://localhost:8080/flow/audit/loop/active/json
```

设置 Content type：

```
application/json
```

然后我们可以在 HTTPRequest 中下发请求：

```
{
    flowentries: [
        {
             dpid: "00:00:00:00:00:00:00:01", 
             match: {
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=2"
        },
        {
             dpid: "00:00:00:00:00:00:00:02", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
        {
             dpid: "00:00:00:00:00:00:00:03", 
             match: {
                 in_port: "2",
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        }
    ]
}
```

HTTPRequest 下发的流表做的事情，就是满足匹配域的流量，按照 `1->2->3` 的方向进行转发。

再加上之前交换机已有的 `3->4->1` 的转发规则，在网络中形成了一个环形的转发路线，所以 `1->2->3` 这个请求就不能被下发。可以通过 HTTPRequest 的返回值得到环路的消息。

### 黑洞的主动审计

路由黑洞的主动审计，主要的方法是得到数据流的转发规则，判断在路线上有没有 `action:drop` 或者没有 `action` 规则。

首先，我们启动 floodlight，然后使用 mininet 创建一个网络。

```
sudo mn --custom /home/floodlight/mininet/topo/3-sw-3-h-blackhole.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6653 --mac --switch=ovsk,protocols=OpenFlow13
```

然后在交换机中生成已有的一些流表，这些流表我写在了脚本文件 `before-loop2.sh` 中，我们可以查看其中的流表内容：

```
ovs-ofctl -O OpenFlow13 add-flow s1 in_port=1,ip,nw_src=10.0.0.0/24,priority:65535,action=output:2
ovs-ofctl -O OpenFlow13 add-flow s2 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=output:3
ovs-ofctl -O OpenFlow13 add-flow s3 in_port=2,ip,nw_src=10.0.0.0/24,priority:65535,action=drop
```

这个流表规则大致做的事情，就是满足匹配域的数据流的转发规则为 `1->2->3-> drop`。

使用命令 

```
sudo sh ./before-blackhole.sh 
```

运行该脚本，给交换机添加流表。

然后设置 HTTPRequest：

设置 URL：

```
http://localhost:8080/flow/audit/backhole/json
```

设置 Content type：

```
application/json
```

然后我们可以在 HTTPRequest 中下发请求：

```
{
    flowentries: [
        {
             dpid: "00:00:00:00:00:00:00:01",
             match: {
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=2"
        },
        {
             dpid: "00:00:00:00:00:00:00:02",
             match: {
                 ipv4_src: "10.0.0.1",
                 ipv4_dst: "10.0.0.3"
             },
             priority: "65529",
             action: "output=3"
        },
    ]
}
```

然后改流量会在 switch3 中被 `action:drop` 捕获到。下发这个请求的时候，可以从 Console 和 返回的数据得到相关的消息。

### 黑洞的被动审计

黑洞的被动审计，使用的是 OpenFlow 自带的 Counter 来计算。所以，发送 GET 请求，或者在浏览器输入 URL 即可：

```
http://localhost:8080/flow/audit/backhole/passive/json
```

