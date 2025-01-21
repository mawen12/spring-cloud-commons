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

package org.springframework.cloud.context.refresh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * 支持上下文刷新
 *
 * @author Dave Syer
 * @author Venil Noronha
 */
public abstract class ContextRefresher {

	protected final Log logger = LogFactory.getLog(getClass());

	protected static final String REFRESH_ARGS_PROPERTY_SOURCE = "refreshArgs";

	protected static final String[] DEFAULT_PROPERTY_SOURCES = new String[] {
			// order matters, if cli args aren't first, things get messy
			CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME, "defaultProperties" };

	/**
	 * 标准属性源名称集合
	 *
	 * <ul>
	 *     <li>systemProperties</li>
	 *     <li>systemEnvironment</li>
	 *     <li>jndiProperties</li>
	 *     <li>servletConfigInitParams</li>
	 *     <li>servletContextInitParams</li>
	 *     <li>configurationProperties</li>
	 * </ul>
	 */
	protected Set<String> standardSources = new HashSet<>(
			Arrays.asList(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
					StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME, "configurationProperties"));

	protected final List<String> additionalPropertySourcesToRetain;

	private ConfigurableApplicationContext context;

	private RefreshScope scope;

	@Deprecated
	protected ContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
		this(context, scope, new RefreshAutoConfiguration.RefreshProperties());
	}

	@SuppressWarnings("unchecked")
	protected ContextRefresher(ConfigurableApplicationContext context, RefreshScope scope, RefreshAutoConfiguration.RefreshProperties properties) {
		this.context = context;
		this.scope = scope;
		additionalPropertySourcesToRetain = properties.getAdditionalPropertySourcesToRetain();
	}

	protected ConfigurableApplicationContext getContext() {
		return this.context;
	}

	protected RefreshScope getScope() {
		return this.scope;
	}

	/**
	 * 刷新上下文
	 *
	 * @return
	 */
	public synchronized Set<String> refresh() {
		//
		Set<String> keys = refreshEnvironment();
		this.scope.refreshAll();
		return keys;
	}

	public synchronized Set<String> refreshEnvironment() {
		// 将所有的属性源展开平铺
		Map<String, Object> before = extract(this.context.getEnvironment().getPropertySources());
		// 更新环境
		updateEnvironment();
		// 将前后发生变化的key提取出来
		Set<String> keys = changes(before, extract(this.context.getEnvironment().getPropertySources())).keySet();
		// 发送环境变化事件，其中仅包含发生变化的key
		this.context.publishEvent(new EnvironmentChangeEvent(this.context, keys));
		return keys;
	}

	protected abstract void updateEnvironment();

	// Don't use ConfigurableEnvironment.merge() in case there are clashes with property
	// source names

	/**
	 * 从输入的环境中复制覆盖了默认属性，以及profile
	 *
	 * @param input
	 * @return
	 */
	protected StandardEnvironment copyEnvironment(ConfigurableEnvironment input) {
		// 构造一个标准环境
		StandardEnvironment environment = new StandardEnvironment();
		// 获取新环境的属性源，用于保存来自输入的属性源信息
		MutablePropertySources capturedPropertySources = environment.getPropertySources();
		// 仅从主环境复制默认属性源和配置文件，其他的属性是可以更改的
		List<String> propertySourcesToRetain = new ArrayList<>(Arrays.asList(DEFAULT_PROPERTY_SOURCES));
		if (!CollectionUtils.isEmpty(additionalPropertySourcesToRetain)) {
			propertySourcesToRetain.addAll(additionalPropertySourcesToRetain);
		}

		for (String name : propertySourcesToRetain) {
			if (input.getPropertySources().contains(name)) {
				if (capturedPropertySources.contains(name)) {
					// 对于用户已提供的默认数据，进行替换
					capturedPropertySources.replace(name, input.getPropertySources().get(name));
				}
				else {
					// 用户未提供，将该属性作为最低优先级
					capturedPropertySources.addLast(input.getPropertySources().get(name));
				}
			}
		}
		// 复制激活的profile
		environment.setActiveProfiles(input.getActiveProfiles());
		// 复制默认的profile
		environment.setDefaultProfiles(input.getDefaultProfiles());
		return environment;
	}

	private Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
		Map<String, Object> result = new HashMap<>();
		for (String key : before.keySet()) {
			if (!after.containsKey(key)) {
				result.put(key, null);
			}
			else if (!equal(before.get(key), after.get(key))) {
				result.put(key, after.get(key));
			}
		}
		for (String key : after.keySet()) {
			if (!before.containsKey(key)) {
				result.put(key, after.get(key));
			}
		}
		return result;
	}

	private boolean equal(Object one, Object two) {
		if (one == null && two == null) {
			return true;
		}
		if (one == null || two == null) {
			return false;
		}
		return one.equals(two);
	}

	private Map<String, Object> extract(MutablePropertySources propertySources) {
		// 倒序处理主要是最先加入的值会被覆盖掉
		Map<String, Object> result = new HashMap<>();
		List<PropertySource<?>> sources = new ArrayList<>();
		for (PropertySource<?> source : propertySources) {
			// 倒序添加
			sources.add(0, source);
		}
		for (PropertySource<?> source : sources) {
			if (!this.standardSources.contains(source.getName())) {
				// 仅处理非标准属性源
				extract(source, result);
			}
		}
		return result;
	}

	private void extract(PropertySource<?> parent, Map<String, Object> result) {
		if (parent instanceof CompositePropertySource) {
			try {
				List<PropertySource<?>> sources = new ArrayList<>();
				// 对于复合的属性源，将其进行迭代，并倒序添加
				for (PropertySource<?> source : ((CompositePropertySource) parent).getPropertySources()) {
					sources.add(0, source);
				}
				for (PropertySource<?> source : sources) {
					// 处理可能复合的一个数据源本身也是复合的场景
					extract(source, result);
				}
			}
			catch (Exception e) {
				return;
			}
		}
		else if (parent instanceof EnumerablePropertySource) {
			// 对于可枚举的属性源，直接保存到哈希表中
			for (String key : ((EnumerablePropertySource<?>) parent).getPropertyNames()) {
				result.put(key, parent.getProperty(key));
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class Empty {

	}

}
