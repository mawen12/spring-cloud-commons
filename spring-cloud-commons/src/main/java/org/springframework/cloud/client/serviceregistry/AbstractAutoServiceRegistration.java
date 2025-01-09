/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.serviceregistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.client.discovery.ManagementServerPortUtils;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * 自动服务注册的总入口，该类负责服务注册、服务注销
 * <p>
 * 其实现了{@link ApplicationListener<WebServerInitializedEvent>}，用于监听实例启动，并获取实例的端口
 *
 * @param <R> Registration type passed to the {@link ServiceRegistry}.
 * @author Spencer Gibb
 * @author Zen Huifer
 */
public abstract class AbstractAutoServiceRegistration<R extends Registration>
		implements AutoServiceRegistration, ApplicationContextAware, ApplicationListener<WebServerInitializedEvent> {

	private static final Log logger = LogFactory.getLog(AbstractAutoServiceRegistration.class);

	/**
	 * 提供服务实例注册注销、状态查询更新的功能
	 */
	private final ServiceRegistry<R> serviceRegistry;

	/**
	 * 默认自启动
	 */
	private final boolean autoStartup = true;

	/**
	 * 是否运行的状态标识，默认false
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final int order = 0;

	/**
	 * 实例端口
	 */
	private final AtomicInteger port = new AtomicInteger(0);

	/**
	 * Spring的应用上下文
	 */
	private ApplicationContext context;

	/**
	 * Spring的环境
	 */
	private Environment environment;

	/**
	 * 自动服务注册属性，影响自动服务注册的行为
	 */
	private AutoServiceRegistrationProperties properties;

	private List<RegistrationManagementLifecycle<R>> registrationManagementLifecycles = new ArrayList<>();

	/**
	 * 管理实例注册的生命周期
	 */
	private List<RegistrationLifecycle<R>> registrationLifecycles = new ArrayList<>();

	protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry,
			AutoServiceRegistrationProperties properties) {
		this.serviceRegistry = serviceRegistry;
		this.properties = properties;
	}

	protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry,
			AutoServiceRegistrationProperties properties,
			List<RegistrationManagementLifecycle<R>> registrationManagementLifecycles,
			List<RegistrationLifecycle<R>> registrationLifecycles) {
		this.serviceRegistry = serviceRegistry;
		this.properties = properties;
		this.registrationManagementLifecycles = registrationManagementLifecycles;
		this.registrationLifecycles = registrationLifecycles;
	}

	protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry,
			AutoServiceRegistrationProperties properties, List<RegistrationLifecycle<R>> registrationLifecycles) {
		this.serviceRegistry = serviceRegistry;
		this.properties = properties;
		this.registrationLifecycles = registrationLifecycles;
	}

	public void addRegistrationManagementLifecycle(RegistrationManagementLifecycle<R> registrationManagementLifecycle) {
		this.registrationManagementLifecycles.add(registrationManagementLifecycle);
	}

	public void addRegistrationLifecycle(RegistrationLifecycle<R> registrationLifecycle) {
		this.registrationLifecycles.add(registrationLifecycle);
	}

	protected ApplicationContext getContext() {
		return this.context;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onApplicationEvent(WebServerInitializedEvent event) {
		ApplicationContext context = event.getApplicationContext();
		if (context instanceof ConfigurableWebServerApplicationContext) {
			if ("management".equals(((ConfigurableWebServerApplicationContext) context).getServerNamespace())) {
				return;
			}
		}
		/**
		 * 获取服务的端口
		 */
		this.port.compareAndSet(0, event.getWebServer().getPort());
		/**
		 * 启动自动服务注册
		 */
		this.start();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
		this.environment = this.context.getEnvironment();
	}

	@Deprecated
	protected Environment getEnvironment() {
		return this.environment;
	}

	@Deprecated
	protected AtomicInteger getPort() {
		return this.port;
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 执行自动服务注册
	 */
	public void start() {
		/**
		 * 如果未开启了服务注侧，则直接退出
		 */
		if (!isEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Discovery Lifecycle disabled. Not starting");
			}
			return;
		}

		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		/**
		 * 如果尚未启动，则直接，否则不再重新启动
		 */
		if (!this.running.get()) {
			/**
			 * 发布实例预注册事件，事件中携带要注册的实例信息
			 */
			this.context.publishEvent(new InstancePreRegisteredEvent(this, getRegistration()));
			/**
			 * 触发实例注册前的生命周期函数，回调{@link RegistrationLifecycle#postProcessBeforeStartRegister(Registration)}
			 */
			registrationLifecycles.forEach(registrationLifecycle -> registrationLifecycle.postProcessBeforeStartRegister(getRegistration()));
			/**
			 * 执行服务注册
			 */
			register();
			/**
			 * 触发实例注册后的生命周期函数，回调{@link RegistrationLifecycle#postProcessAfterStartRegister(Registration)}
			 */
			this.registrationLifecycles.forEach(registrationLifecycle -> registrationLifecycle.postProcessAfterStartRegister(getRegistration()));

			if (shouldRegisterManagement()) {
				/**
				 * 触发管理注册后的生命周期函数，回调{@link RegistrationManagementLifecycle#postProcessBeforeStartRegisterManagement(Registration)}
				 */
				this.registrationManagementLifecycles.forEach(registrationManagementLifecycle -> registrationManagementLifecycle.postProcessBeforeStartRegisterManagement(getManagementRegistration()));
				/**
				 * 执行管理注册
				 */
				this.registerManagement();
				/**
				 * 触发管理注册的生命周期函数，回调{@link RegistrationManagementLifecycle#postProcessAfterStartRegisterManagement(Registration)}
				 */
				registrationManagementLifecycles.forEach(registrationManagementLifecycle -> registrationManagementLifecycle.postProcessAfterStartRegisterManagement(getManagementRegistration()));
			}
			/**
			 * 发布实例注册完成事件，事件中携带了服务注册的配置信息
			 * Nacos的解决方案是返回NacosDiscoveryProperties
			 */
			this.context.publishEvent(new InstanceRegisteredEvent<>(this, getConfiguration()));
			/**
			 * 更新运行状态
			 */
			this.running.compareAndSet(false, true);
		}

	}

	/**
	 * @return Whether the management service should be registered with the
	 * {@link ServiceRegistry}.
	 */
	protected boolean shouldRegisterManagement() {
		if (this.properties == null || this.properties.isRegisterManagement()) {
			return getManagementPort() != null && ManagementServerPortUtils.isDifferent(this.context);
		}
		return false;
	}

	/**
	 * @return The object used to configure the registration.
	 */
	@Deprecated
	protected abstract Object getConfiguration();

	/**
	 * Nacos中使用PROPERTIES(spring.cloud.nacos.discovery.enabled)=true表示开启，默认为true
	 *
	 * @return 是否开启服务注册
	 */
	protected abstract boolean isEnabled();

	/**
	 * @return The serviceId of the Management Service.
	 */
	@Deprecated
	protected String getManagementServiceId() {
		// TODO: configurable management suffix
		return this.context.getId() + ":management";
	}

	/**
	 * @return The service name of the Management Service.
	 */
	@Deprecated
	protected String getManagementServiceName() {
		// TODO: configurable management suffix
		return getAppName() + ":management";
	}

	/**
	 * @return The management server port.
	 */
	@Deprecated
	protected Integer getManagementPort() {
		return ManagementServerPortUtils.getPort(this.context);
	}

	/**
	 * @return The app name (currently the spring.application.name property).
	 */
	@Deprecated
	protected String getAppName() {
		return this.environment.getProperty("spring.application.name", "application");
	}

	@PreDestroy
	public void destroy() {
		stop();
	}

	public boolean isRunning() {
		return this.running.get();
	}

	protected AtomicBoolean getRunning() {
		return this.running;
	}

	public int getOrder() {
		return this.order;
	}

	public int getPhase() {
		return 0;
	}

	protected ServiceRegistry<R> getServiceRegistry() {
		return this.serviceRegistry;
	}

	protected abstract R getRegistration();

	protected abstract R getManagementRegistration();

	/**
	 * Register the local service with the {@link ServiceRegistry}.
	 */
	protected void register() {
		/**
		 * 使用{@link ServiceRegistry}执行实例注册
		 */
		this.serviceRegistry.register(getRegistration());
	}

	/**
	 * Register the local management service with the {@link ServiceRegistry}.
	 */
	protected void registerManagement() {
		/**
		 * 获取管理相关实例
		 */
		R registration = getManagementRegistration();
		if (registration != null) {
			/**
			 * 注册实例
			 */
			this.serviceRegistry.register(registration);
		}
	}

	/**
	 * De-register the local service with the {@link ServiceRegistry}.
	 */
	protected void deregister() {
		/**
		 * 使用{@link ServiceRegistry}注销实例
		 */
		this.serviceRegistry.deregister(getRegistration());
	}

	/**
	 * De-register the local management service with the {@link ServiceRegistry}.
	 */
	protected void deregisterManagement() {
		/**
		 * 获取管理相关的实例
		 */
		R registration = getManagementRegistration();
		if (registration != null) {
			/**
			 * 使用{@link ServiceRegistry}注销管理实例
			 */
			this.serviceRegistry.deregister(registration);
		}
	}

	public void stop() {
		/**
		 * 更新运行状态
		 */
		if (this.getRunning().compareAndSet(true, false) && isEnabled()) {
			/**
			 * 触发实例注销前的生命周期函数，回调{@link RegistrationLifecycle#postProcessBeforeStopRegister(Registration)}
			 */
			this.registrationLifecycles.forEach(registrationLifecycle -> registrationLifecycle.postProcessBeforeStopRegister(getRegistration()));
			/**
			 * 执行服务注销
			 */
			deregister();
			/**
			 * 触发实例注册后的生命周期函数，回调{@link RegistrationLifecycle#postProcessAfterStopRegister(Registration)}
			 */
			this.registrationLifecycles.forEach(registrationLifecycle -> registrationLifecycle.postProcessAfterStopRegister(getRegistration()));
			if (shouldRegisterManagement()) {
				/**
				 * 触发管理注销前的生命周期函数，回调{@link RegistrationManagementLifecycle#postProcessBeforeStopRegisterManagement(Registration)}
				 */
				this.registrationManagementLifecycles.forEach(registrationManagementLifecycle -> registrationManagementLifecycle.postProcessBeforeStopRegisterManagement(getManagementRegistration()));
				/**
				 * 执行管理注销
				 */
				deregisterManagement();
				/**
				 * 触发管理注销后的生命周期函数，回调{@link RegistrationManagementLifecycle#postProcessAfterStopRegisterManagement(Registration)}
				 */
				this.registrationManagementLifecycles.forEach(registrationManagementLifecycle -> registrationManagementLifecycle.postProcessAfterStopRegisterManagement(getManagementRegistration()));
			}
			/**
			 * 停止服务注册
			 */
			this.serviceRegistry.close();
		}
	}

}
