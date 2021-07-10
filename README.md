# Mirai Console Minecraft Plugin

Mirai Console Minecraft Plugin



## 命令

```shell
# 添加普通服务器，只能使用ping
/mc add <name> <address> <port> 
# 添加带登陆配置的服务器
/mc add <name> <address> <port> <loginName>
# 删除服务器
/mc delete <name>
# 服务器列表
/mc list

# 添加登陆信息 不能在群配置
/mc addLogin <name> <authServerUrl> <sessionServerUrl> <username> <password>
# 删除登陆信息
/mc deleteLogin <name>
# 登陆信息查看
/mc loginInfo
```

mc addLogin url参考 [yggdrasil](https://github.com/yushijinhun/authlib-injector/wiki/Yggdrasil-%E6%9C%8D%E5%8A%A1%E7%AB%AF%E6%8A%80%E6%9C%AF%E8%A7%84%E8%8C%83#%E4%BC%9A%E8%AF%9D%E9%83%A8%E5%88%86)

- name   登陆配置名称
- authServerUrl 验证服务器地址 正版地址为:https://authserver.mojang.com/authenticate
- sessionServerUrl 会话服务器地址 正版地址为:https://sessionserver.mojang.com
- username 账号
- password 密码



```
Minecraft 插件使用说明:
!为中英文通用,!和服务器名称之间可以有空格,和tps之间也一样
Ping服务器:!前缀加服务器名称,如!服务器名称
TPS:!前缀加服务器名称,tps结尾,如!服务器名称tps
查看服务器列表:!list 或 !列表
```

## 版本支持

TPS 暂时只支持 Forge 端

