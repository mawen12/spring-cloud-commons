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

package org.springframework.cloud.loadbalancer.annotation;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * 扫描{@link LoadBalancerClients}注解的类，并进行注册
 *
 * @author Dave Syer
 * @see LoadBalancerClient
 * @see LoadBalancerClients
 */
public class LoadBalancerClientConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * @param client
	 * @return 解析 {@link LoadBalancerClient#value()}或{@link LoadBalancerClient#name()}的值
	 */
	private static String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("value");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}
		throw new IllegalStateException("Either 'name' or 'value' must be provided in @LoadBalancerClient");
	}

	private static void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LoadBalancerClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		// 注册Bean定义
		registry.registerBeanDefinition(name + ".LoadBalancerClientSpecification", builder.getBeanDefinition());
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		// 读取LoadBalancerClients注解属性
		Map<String, Object> attrs = metadata.getAnnotationAttributes(LoadBalancerClients.class.getName());
		if (attrs != null && attrs.containsKey("value")) {// 处理属性值value
			AnnotationAttributes[] clients = (AnnotationAttributes[]) attrs.get("value");
			for (AnnotationAttributes client : clients) {
				// 注册LoadBalancerClientSpecification
				registerClientConfiguration(registry, getClientName(client), client.get("configuration"));
			}
		}
		if (attrs != null && attrs.containsKey("defaultConfiguration")) {// 处理属性值defaultConfiguration
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			} else {
				name = "default." + metadata.getClassName();
			}
			// 注册默认的配置类
			registerClientConfiguration(registry, name, attrs.get("defaultConfiguration"));
		}
		// 读取LoadBalancerClient注解属性
		Map<String, Object> client = metadata.getAnnotationAttributes(LoadBalancerClient.class.getName());
		String name = getClientName(client);
		if (name != null) {
			registerClientConfiguration(registry, name, client.get("configuration"));
		}
	}

}
