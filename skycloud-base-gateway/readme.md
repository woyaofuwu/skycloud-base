## 网关

#### 入参日志与请求响应时间

#### 全局认证

#### 入参验签

#### 修改请求体

#### 全局CORS

#### 全局熔断降级

#### 动态路由

#### 多维度限流

#### 在线文档
![swagger](../docs/image/swagger.png)


docker run --name skycloud-gateway -d -p 8904:8904  -e CONFIG_ENV=dev -e consul.host='192.168.128.2' skycloud-base-gateway:latest
docker run --name skycloud-base-gateway  -p 8904:8904  -e CONFIG_ENV=dev -e consul.host='192.168.128.2' skycloud-base-gateway:latest