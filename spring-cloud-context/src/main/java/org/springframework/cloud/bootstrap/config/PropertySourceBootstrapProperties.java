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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用于Spring Cloud Config的启动属性类
 *
 * @author Dave Syer
 */
@ConfigurationProperties("spring.cloud.config")
public class PropertySourceBootstrapProperties {

	/**
	 * 指明外部属性是否可以覆盖系统属性的标识
	 */
	private boolean overrideSystemProperties = true;

	/**
	 * 指明{@link #overrideSystemProperties}是否去可以起效的标识，默认为true，代表用户可以覆盖默认配置。
	 */
	private boolean allowOverride = true;

	/**
	 * Flag to indicate that when {@link #setAllowOverride(boolean) allowOverride} is
	 * true, external properties should take lowest priority and should not override any
	 * existing property sources (including local config files). Default false. This will
	 * only have an effect when using config first bootstrap.
	 *
	 * 指明当{@link #allowOverride}为true时，外部属性应该是最低优先级，并不允许覆盖任何已经存在的属性源，包括本地配置文件。默认为false
	 */
	private boolean overrideNone = false;

	/**
	 * 是否在接收到{@link org.springframework.context.event.ContextRefreshedEvent}时初始化启动配置的标识，默认为false
	 */
	private boolean initializeOnContextRefresh = false;

	public boolean isInitializeOnContextRefresh() {
		return initializeOnContextRefresh;
	}

	public void setInitializeOnContextRefresh(boolean initializeOnContextRefresh) {
		this.initializeOnContextRefresh = initializeOnContextRefresh;
	}

	public boolean isOverrideNone() {
		return this.overrideNone;
	}

	public void setOverrideNone(boolean overrideNone) {
		this.overrideNone = overrideNone;
	}

	public boolean isOverrideSystemProperties() {
		return this.overrideSystemProperties;
	}

	public void setOverrideSystemProperties(boolean overrideSystemProperties) {
		this.overrideSystemProperties = overrideSystemProperties;
	}

	public boolean isAllowOverride() {
		return this.allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

}
