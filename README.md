# 如何优雅关闭 Spring Boot 应用

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57nkcvtv9j31900u0e83.jpg)

## 前言

随着线上应用逐步采用 SpringBoot 构建，SpringBoot应用实例越来多，当线上某个应用需要升级部署时，常常简单粗暴地使用 kill 命令，这种停止应用的方式会让应用将所有处理中的请求丢弃，响应失败。这样的响应失败尤其是在处理重要业务逻辑时需要极力避免的，那么有什么更好的方式来平滑地关闭 SpringBoot 应用呢？那就通过本文一起来探究吧。（本文主要针对基于Spring Boot 内嵌 Tomcat 容器作为 Web 服务的应用）

> 本文示例代码可以通过下面仓库地址获取：
>
> - springboot-shutdown：https://github.com/wrcj12138aaa/springboot-shutdown
>
> 环境支持：
>
> - JDK 8
> - SpringBoot 2.1.4
> - Maven 3.6.0



## 定制 Tomcat Connector 行为

要平滑关闭 Spring Boot 应用的前提就是首先要关闭其内置的 Web 容器，不再处理外部新进入的请求。为了能让应用接受关闭事件通知的时候，保证当前 Tomcat 处理所有已经进入的请求，我们需要实现 TomcatConnectorCustomizer 接口，这个接口的源码十分简单，从注释可以看出这是实现自定义 Tomcat Connector 行为的回调接口：

![](http://ww4.sinaimg.cn/large/006tNc79ly1g57o78m925j314a0q441c.jpg)



这里如果小伙伴对 Connector 不太熟悉，我就简单描述下： Connector 属于 Tomcat 抽象组件，功能就是用来接受外部请求，以及内部传递，并返回响应内容，是Tomcat 中请求处理和响应的重要组件，具体实现有 HTTP Connector 和 AJP Connector。

通过定制 Connector 的行为，我们就可以允许在请求处理完毕后进行 Tomcat 线程池的关闭，具体实现代码如下：

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57o7yvovsj30u00utwq5.jpg)

上述代码定义的 TIMEOUT 变量为 Tomcat 线程池延时关闭的最大等待时间，一旦超过这个时间就会强制关闭线程池，也就无法处理所有请求了，我们通过控制 Tomcat 线程池的关闭时机，来实现优雅关闭 Web 应用的功能。另外需要注意的是我们的类 CustomShutdown 实现了 ApplicationListener<ContextClosedEvent> 接口，意味着监听着 Spring 容器关闭的事件，即当前的 ApplicationContext 执行 close 方法。

##内嵌 Tomcat 添加 Connector 回调

有了定制的 Connector 回调，我们需要在启动过程中添加到内嵌的 Tomcat 容器中，然后等待执行。那这一步又是如何实现的呢，可以参考下面代码：

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57o8jsqwuj31fg0hsdj6.jpg)

这里的 TomcatServletWebServerFactory 是 Spring Boot 实现内嵌 Tomcat 的工厂类，类似的其他 Web 容器，也有对应的工厂类如 JettyServletWebServerFactory，UndertowServletWebServerFactory。他们共同的特点就是继承同个抽象类 AbstractServletWebServerFactory，提供了 Web 容器默认的公共实现，如应用上下文设置，会话管理等。

如果我们需要定义Spring Boot 内嵌的 Tomcat 容器时，就可以使用 TomcatServletWebServerFactory 来进行个性化定义，例如下方为官方文档提供自定示例：

![](http://ww1.sinaimg.cn/large/006tNc79ly1g57o9d9motj314s0c8q4z.jpg)

好了说回正题，我们这里使用 `addConnectorCustomizers` 方法将自定义的 Connector 行为添加到内嵌的Tomcat 之上，为了查看加载效果，我们可以在 Spring Boot 程序启动后从容器中获取下webServerFactory  对象，然后观察，在它的 tomcatConnectorCustomizers 属性中可以看到已经有了 CustomeShutdown 对象。

![](http://ww4.sinaimg.cn/large/006tNc79ly1g57hk8irurj31ha0mawjk.jpg)



## 开启 Shutdown Endpoint

到目前让内嵌 Tomcat 容器平稳关闭的操作已经完成，接下来要做的就是如何关闭主动关闭 Spring 容器了，除了常规Linux 命令 Kill，我们可以利用 Spring Boot Actuator 来实现Spring 容器的远程关闭，怎么实现继续看

Spring Boot Actuator 是 Spring Boot 的一大特性，它提供了丰富的功能来帮助我们监控和管理生产环境中运行的 Spring Boot 应用。我们可以通过 HTTP 或者 JMX 方式来对我们应用进行管理，除此之外，它为我们的应用提供了审计，健康状态和度量信息收集的功能，能帮助我们更全面地了解运行中的应用。

> **Actuator**， ['æktʃʊˌeɪtə]  中文翻译过来就是**制动器**，这是一个制造业的术语，指的是用于控制某物的机械装置。

在 Spring Boot Actuator 中也提供控制应用关闭的功能，所以我们要为应用引入 Spring Boot Actuator，具体方式就是要将对应的 starter 依赖添加到当前项目中，以 Maven 项目为例：

![](http://ww1.sinaimg.cn/large/006tNc79ly1g57o9wwo4bj311s09gwfg.jpg)

Spring Boot Actuator 采用向外部暴露 Endpoint (端点)的方式来让我们与应用进行监控和管理，引入 `spring-boot-starter-actuator` 之后，我们就需要启用我们需要的 Shutdown Endpoint，在配置文件 application.properties 中，设置如下

![](http://ww1.sinaimg.cn/large/006tNc79ly1g57oe2ap7lj311w04qaad.jpg)

第一行表示启用 Shutdown Endpoint ，第二行表示向外部以 HTTP 方式暴露所有 Endpoint，默认情况下除了 Shutdown Endpoint 之外，其他 Endpoint 都是启用的。

> 除了  Shutdown Endpoint，Actuator Endpoint 还有十余种，有的是特定操作，比如 `heapdump` 转储内存日志；有的是信息展示，比如 `health` 显示应用健康状态。具体所有 Endpoint 信息可以参见[官方文档-53. Endpoints](https://docs.spring.io/spring-boot/docs/2.1.4.RELEASE/reference/htmlsingle/#production-ready-endpoints) 一节。

到这里我们的前期配置工作就算完成了。当启动应用后，就可以通过POST 方式请求对应路径的 `http://host:port/actuator/shutdown` 来实现Spring Boot 应用远程关闭，是不是很简单呢。

## 模拟测试

这里为了模拟测试，我们首先模拟实现长达10s 时间处理业务的请求控制器 BusinessController，具体实现如下：

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57oa8akklj311s0j6q4l.jpg)

用 `Thread.sleep` 来阻塞当前请求线程，模拟业务处理，在此同时用 HTTP 方式访问 Shutdown Endpoint 试图关闭应用，可以通过观察控制台日志看是否应用是否会完成请求的处理后才真正进行关闭。

首先用 curl 命令模拟发送业务请求：

![](http://ww4.sinaimg.cn/large/006tNc79ly1g57oahnj30j311s03wmxb.jpg)

然后在业务处理中，直接发送请求 `actuator/shutdown`,尝试关闭应用，同样采用 curl 方式：

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57oatp4cuj311s03wwem.jpg)

`actuator/shutdown` 请求发送后会立即返回响应结果，但应用并不会停止：

![](http://ww1.sinaimg.cn/large/006tNc79ly1g57ob52cqgj311s03wwen.jpg)

最后看下控制台的日志输出顺序：

![](http://ww1.sinaimg.cn/large/006tNc79ly1g57ivv2lrsj31xm08kad4.jpg)

可以看出在发送业务请求之后立刻发送关闭应用的请求，并不会立即将应用停止，而是在请求处理完毕之后，就是阻塞的 10s 后应用开始退出，这样可以保证已经接收到的请求能返回正常响应, 而关闭请求之后再进入的请求都不会被处理，到这里我们优雅关闭 Spring Boot 程序的操作就此实现了。

## 实现自动化

由于 Spring Boot 提供内嵌 Web 容器的便利性，我们经常将程序打包成 jar 然后发布。通常应用的启动和关闭操作流程是固定且重复的，本着 Don't Repeat Yourself 原则，我们有必要将这个操作过程自动化，将关闭和启用的 SpringBoot应用的操作写成 shell 脚本，以避免出现人为的差错，并且方便使用，提高操作效率。下面是我针对示例程序所写的程序启动脚本：(具体脚本可在示例项目查看)

![](http://ww4.sinaimg.cn/large/006tNc79ly1g57obu8valj30vn0u0diz.jpg)

有了脚本，我们可以直接通过命令行方式平滑地更新部署 Spring Boot 程序，效果如下：

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57kxoe72fj31wm0o4wzi.jpg)

## 总结

本文主要探究了如何对基于Spring Boot 内嵌 Tomcat 的 Web 应用进行平滑关闭的实现，如果采用其他 Web 容器也类似方式，希望这边文章有所帮助，若有错误或者不当之处，还请大家批评指正，一起学习交流。

![](http://ww3.sinaimg.cn/large/006tNc79ly1g57n4roxy6j30p00dwaim.jpg)

## 参考

- Graceful Shutdown Spring Boot Applications：https://blog.marcosbarbero.com/graceful-shutdown-spring-boot-apps/
- Shutdown a Spring Boot Application：https://www.baeldung.com/spring-boot-shutdown
- 官方文档-53. Endpoints：https://docs.spring.io/spring-boot/docs/2.1.4.RELEASE/reference/htmlsingle/#production-ready-endpoints
- The HTTP Connector：https://tomcat.apache.org/tomcat-8.5-doc/config/http.html
- Customizing ConfigurableServletWebServerFactory Directly：https://docs.spring.io/spring-boot/docs/2.1.4.RELEASE/reference/htmlsingle/#boot-features-customizing-configurableservletwebserverfactory-directly
