# Mirai Console Minecraft Plugin

Mirai Console Minecraft Plugin



## 命令

```shell
/mc addLogin <name> <authServerUrl> <sessionServerUrl> <username> <password>    # 添加登陆信息
/mc addServer <name> <address> [port]    # 添加服务器,端口默认 25565
/mc addServerLogin <loginName> <name> <address> [port]    # 添加带登陆信息带服务器,端口默认 25565
/mc deleteLogin <name>    # 删除登陆信息
/mc deleteServer <name>    # 删除服务器
/mc loginInfo    # 查看登陆信息
```

mc addLogin url参考 [yggdrasil](https://github.com/yushijinhun/authlib-injector/wiki/Yggdrasil-%E6%9C%8D%E5%8A%A1%E7%AB%AF%E6%8A%80%E6%9C%AF%E8%A7%84%E8%8C%83#%E4%BC%9A%E8%AF%9D%E9%83%A8%E5%88%86)

- name   登陆配置名称
- authServerUrl 验证服务器地址 正版地址为:https://authserver.mojang.com/authenticate
- sessionServerUrl 会话服务器地址 正版地址为:https://sessionserver.mojang.com
- username 账号
- password 密码

```shell
# 设置触发指令
/mc setCommand <name> <command>  
```
name 可设置如下
 - PING `ping服务器`
 - LIST `查询列表`
 - TPS `查询tps`
 - PING_ALL `ping全部服务器`


## 版本支持

TPS 暂时只支持 Forge 端

