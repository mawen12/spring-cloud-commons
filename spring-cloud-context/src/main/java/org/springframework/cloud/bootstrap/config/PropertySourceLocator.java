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

package org.springframework.cloud.bootstrap.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * 用于环境的属性源订阅的策略。除非实现意图阻止应用程序启动，否则实现不应该失败
 *
 * @author Dave Syer
 *
 */
public interface PropertySourceLocator {

	/**
	 * @param environment 当前环境
	 * @return 返回一个属性源
	 * @throws IllegalStateException if there is a fail-fast condition.
	 */
	PropertySource<?> locate(Environment environment);

	default Collection<PropertySource<?>> locateCollection(Environment environment) {
		return locateCollection(this, environment);
	}

	static Collection<PropertySource<?>> locateCollection(PropertySourceLocator locator, Environment environment) {
		// 定位到的属性源
		PropertySource<?> propertySource = locator.locate(environment);
		if (propertySource == null) {
			// 空属性源，返回空集合
			return Collections.emptyList();
		}
		// 复合属性源
		if (propertySource instanceof CompositePropertySource) {
			// 迭代出内部的属性源集合，并加入到新集合中，再返回
			Collection<PropertySource<?>> sources = ((CompositePropertySource) propertySource).getPropertySources();
			List<PropertySource<?>> filteredSources = new ArrayList<>();
			for (PropertySource<?> p : sources) {
				if (p != null) {
					filteredSources.add(p);
				}
			}
			return filteredSources;
		}
		else {
			return List.of(propertySource);
		}
	}

}
