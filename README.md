<div align="center">

[![](https://img.shields.io/github/v/release/limbang/mirai-console-minecraft-plugin?include_prereleases)](https://github.com/limbang/mirai-console-minecraft-plugin/releases)
![](https://img.shields.io/github/downloads/limbang/mirai-console-minecraft-plugin/total)
[![](https://img.shields.io/github/license/limbang/mirai-console-minecraft-plugin)](https://github.com/limbang/mirai-console-minecraft-plugin/blob/master/LICENSE)
[![](https://img.shields.io/badge/mirai-2.16.0-69c1b9)](https://github.com/mamoe/mirai)

本项目是基于 Mirai Console 编写的插件
<p>用于 ping 服务器状态</p>
<p>戳一戳机器人头像可以获取帮助</p>
</div>

可选前置插件[mirai-plugin-general-interface](https://github.com/limbang/mirai-plugin-general-interface)用来支持事件

## 命令

```shell
/mc addServer <name> <address> [port]    # 添加服务器,端口默认 25565
/mc deleteServer <name>    # 删除服务器
/mc setAllToImg <value>    # 设置All消息转换为图片功能是否启动
```

```shell
# 设置触发指令
/mc setCommand <name> <command>  
```
name 可设置如下
 - PING `ping服务器`
 - LIST `查询列表`
 - PING_ALL `ping全部服务器`


## 功能展示

戳一戳功能：
![](img/Screenshot_20220319_195629.jpg)

直 ping 地址功能：
![](img/ABCBBD85-E183-41FE-BA3A-9D88853F43B3.png)

ping 全部添加的服务器功能：
![](img/B12FD04B-B159-4D4A-BE62-EA39510D9106.png)
