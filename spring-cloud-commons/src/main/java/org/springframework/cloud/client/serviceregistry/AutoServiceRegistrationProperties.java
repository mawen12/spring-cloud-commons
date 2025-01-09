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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自动服务注册的属性，其读取 PROPERTIES(spring.cloud.service-registry.auto-registration)
 *
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.service-registry.auto-registration")
public class AutoServiceRegistrationProperties {

	/**
	 * 服务自动注册是否启用，默认为true
	 */
	private boolean enabled = true;

	/**
	 * 是否将管理注册为一个服务，默认为true
	 */
	private boolean registerManagement = true;

	/**
	 * 如果没有使用自动服务注册，是否启动失败，默认为false
	 */
	private boolean failFast = false;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRegisterManagement() {
		return this.registerManagement;
	}

	public void setRegisterManagement(boolean registerManagement) {
		this.registerManagement = registerManagement;
	}

	@Deprecated
	public boolean shouldRegisterManagement() {
		return this.registerManagement;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

}
