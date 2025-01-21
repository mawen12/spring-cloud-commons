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

package org.springframework.cloud.loadbalancer.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 用于创建客户端、负载均衡器和客户端配置实例的工厂，它为每个客户端名称创建一个{@link org.springframework.context.ApplicationContext}，
 * 并从其获取需要的Bean
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerClientFactory extends NamedContextFactory<LoadBalancerClientSpecification>
		implements ReactiveLoadBalancer.Factory<ServiceInstance> {

	private static final Log log = LogFactory.getLog(LoadBalancerClientFactory.class);

	/**
	 * 负载均衡器的属性源名称
	 */
	public static final String NAMESPACE = "loadbalancer";

	/**
	 * 负载均衡器命名空间内的客户端名称的属性
	 */
	public static final String PROPERTY_NAME = NAMESPACE + ".client.name";

	/**
	 * 负载均衡器客户端相关属性
	 */
	private final LoadBalancerClientsProperties properties;

	public LoadBalancerClientFactory(LoadBalancerClientsProperties properties) {
		super(LoadBalancerClientConfiguration.class, NAMESPACE, PROPERTY_NAME, new HashMap<>());
		this.properties = properties;
	}

	public LoadBalancerClientFactory(LoadBalancerClientsProperties properties, Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
		super(LoadBalancerClientConfiguration.class, NAMESPACE, PROPERTY_NAME, applicationContextInitializers);
		this.properties = properties;
	}

	/**
	 * @param environment
	 * @return 返回客户端名称，此处为服务Id
	 */
	public static String getName(Environment environment) {
		return environment.getProperty(PROPERTY_NAME);
	}

	/**
	 * 从{@link org.springframework.context.ApplicationContext}获取该服务名称的{@link ReactorServiceInstanceLoadBalancer}
	 *
	 * @param serviceId 服务名称
	 * @return
	 */
	@Override
	public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
		return getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class);
	}

	/**
	 * 获取对应服务名称的属性
	 *
	 * @param serviceId 服务名称
	 * @return
	 */
	@Override
	public LoadBalancerProperties getProperties(String serviceId) {
		if (properties == null) {
			if (log.isWarnEnabled()) {
				log.warn("LoadBalancerClientsProperties is null. Please use the new constructor.");
			}
			// 未设置属性时，返回空
			return null;
		}
		// 服务名称为空，或者服务名称不存在，返回默认的属性
		if (serviceId == null || !properties.getClients().containsKey(serviceId)) {
			// no specific client properties, return default
			return properties;
		}
		// 返回归属该服务的属性，该属性是从默认属性扩展而来的，如果未指定部分属性，则返回默认值
		return properties.getClients().get(serviceId);
	}

	@SuppressWarnings("unchecked")
	public LoadBalancerClientFactory withApplicationContextInitializers(Map<String, Object> applicationContextInitializers) {
		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
		applicationContextInitializers.keySet().forEach(contextId -> convertedInitializers.put(contextId, (ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers.get(contextId)));
		return new LoadBalancerClientFactory(properties, convertedInitializers);
	}

}
