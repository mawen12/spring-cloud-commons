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

package org.springframework.cloud.client.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.cloud.commons.util.SpringFactoryImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 开启服务发现客户端，负责解析{@link EnableDiscoveryClient}注解，并根据对应属性值加载不同的行为。
 * 默认下自动进行服务注册
 *
 * @author Spencer Gibb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EnableDiscoveryClientImportSelector extends SpringFactoryImportSelector<EnableDiscoveryClient> {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		String[] imports = super.selectImports(metadata);

		/**
		 * 读取注解{@link EnableDiscoveryClient}中的属性值
		 */
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(getAnnotationClass().getName(), true));

		/**
		 * 获取{@link EnableDiscoveryClient#autoRegister()}的值
		 */
		boolean autoRegister = attributes.getBoolean("autoRegister");

		if (autoRegister) {
			/**
			 * 如果开启自动注册，则将类{@link org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration}加入进来
			 */
			List<String> importsList = new ArrayList<>(Arrays.asList(imports));
			importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
			imports = importsList.toArray(new String[0]);
		}
		else {
			/**
			 * 未开启自动注册，将该属性值写入到PROPERTIES(spring.cloud.service-registry.autp-registration.enabled)=false中
			 */
			Environment env = getEnvironment();
			if (env instanceof ConfigurableEnvironment configEnv) {
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				map.put("spring.cloud.service-registry.auto-registration.enabled", false);
				MapPropertySource propertySource = new MapPropertySource("springCloudDiscoveryClient", map);
				/**
				 * 将其作为属性源加入到最末端
				 */
				configEnv.getPropertySources().addLast(propertySource);
			}

		}

		return imports;
	}

	@Override
	protected boolean isEnabled() {
		/**
		 * 解析是否开启服务发现，从 PROPERTIES(spring.cloud.discovery.enabled) -> DEFAULT(true)
		 */
		return getEnvironment().getProperty("spring.cloud.discovery.enabled", Boolean.class, Boolean.TRUE);
	}

	@Override
	protected boolean hasDefaultFactory() {
		return true;
	}

}
