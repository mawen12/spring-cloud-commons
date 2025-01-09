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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Spring Cloud服务发现中客户端的健康指示器的属性，
 * 从 PROPERTIES(spring.cloud.discovery.client.health-indicator) 读取
 *
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.discovery.client.health-indicator")
public class DiscoveryClientHealthIndicatorProperties {

	/**
	 * 是否开启，默认为true
	 */
	private boolean enabled = true;

	/**
	 * 是否包含描述，默认为false
	 */
	private boolean includeDescription = false;

	/**
	 * 是否使用{@link DiscoveryClient#getServices()}来检查其健康状态，默认为true。
	 * 如果为false，则代表使用{@link DiscoveryClient#probe()}来检查健康状态，因为用户可以覆盖该方法，
	 * 所以该方法的性能影响可能会更小
	 */
	private boolean useServicesQuery = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isIncludeDescription() {
		return this.includeDescription;
	}

	public void setIncludeDescription(boolean includeDescription) {
		this.includeDescription = includeDescription;
	}

	public boolean isUseServicesQuery() {
		return useServicesQuery;
	}

	public void setUseServicesQuery(boolean useServicesQuery) {
		this.useServicesQuery = useServicesQuery;
	}

	@Override
	public String toString() {
		return "DiscoveryClientHealthIndicatorProperties{" + "enabled=" + this.enabled + ", includeDescription="
				+ this.includeDescription + ", useServicesQuery=" + this.useServicesQuery + '}';
	}

}
