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

package org.springframework.cloud.client.discovery.health;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * 服务发现客户端的健康指示器，{@link DiscoveryHealthIndicator}的客户端侧实现
 * <p>
 * 其实现了{@link ApplicationListener<InstanceRegisteredEvent<?>>}，监听实例注册完成的事件，
 *
 * @author Spencer Gibb
 * @author Chris Bono
 */
public class DiscoveryClientHealthIndicator implements DiscoveryHealthIndicator, Ordered, ApplicationListener<InstanceRegisteredEvent<?>> {

	/**
	 * 封装了服务发现客户端
	 */
	private final ObjectProvider<DiscoveryClient> discoveryClient;

	/**
	 * 保存了Spring Cloud客户端健康指示器的配置
	 */
	private final DiscoveryClientHealthIndicatorProperties properties;

	private final Log log = LogFactory.getLog(DiscoveryClientHealthIndicator.class);

	/**
	 * 客户端是否初始化的标识位
	 */
	private AtomicBoolean discoveryInitialized = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE;

	public DiscoveryClientHealthIndicator(ObjectProvider<DiscoveryClient> discoveryClient,
			DiscoveryClientHealthIndicatorProperties properties) {
		this.discoveryClient = discoveryClient;
		this.properties = properties;
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent<?> event) {
		/**
		 * 当监听到实例注册成功的事件后，代表该服务发现初始化完成
		 */
		if (this.discoveryInitialized.compareAndSet(false, true)) {
			this.log.debug("Discovery Client has been initialized");
		}
	}

	@Override
	public Health health() {
		Health.Builder builder = new Health.Builder();

		if (this.discoveryInitialized.get()) {
			/**
			 * 已经初始化，则获取服务发现客户端，查找服务信息
			 */
			try {
				DiscoveryClient client = this.discoveryClient.getIfAvailable();
				String description = (this.properties.isIncludeDescription()) ? client.description() : "";

				if (properties.isUseServicesQuery()) {
					/**
					 * 使用服务发现获取所有的服务名称
					 */
					List<String> services = client.getServices();
					/**
					 * 写入UP状态，并将服务名称写入
					 */
					builder.status(new Status("UP", description)).withDetail("services", services);
				}
				else {
					/**
					 * 进行轻量级的健康检查，但是默认情况下还是调用{@link DiscoveryClient#getServices()}，
					 * 因为用户可以覆盖该方法，因此可能是轻量级的
					 */
					client.probe();
					/**
					 * 写入UP状态
					 */
					builder.status(new Status("UP", description));
				}
			}
			catch (Exception e) {
				this.log.error("Error", e);
				/**
				 * 出现异常时，写入DOWN状态和异常信息
				 */
				builder.down(e);
			}
		}
		else {
			/**
			 * 此时服务发现尚未初始化，写入UNKNOWN状态
			 */
			builder.status(new Status(Status.UNKNOWN.getCode(), "Discovery Client not initialized"));
		}
		return builder.build();
	}

	@Override
	public String getName() {
		return "discoveryClient";
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
