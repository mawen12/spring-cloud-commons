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

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

/**
 * 用于服务发现，即从服务管理组件读取服务的操作
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Chris Bono
 */
public interface DiscoveryClient extends Ordered {

	/**
	 * Default order of the discovery client.
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * 人类可读描述实现
	 * @return The description.
	 */
	String description();

	/**
	 * 返回特定服务ID的所有服务实例
	 * @param serviceId The serviceId to query.
	 * @return A List of ServiceInstance.
	 */
	List<ServiceInstance> getInstances(String serviceId);

	/**
	 * 返回所有的服务ID
	 * @return All known service IDs.
	 */
	List<String> getServices();

	/**
	 * 用于验证客户端是否合法，是否能够发起调用
	 * <p>
	 * 验证的方式就是获取所有的服务ID，并且没有报错
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} - client
	 * implementations can override with a lighter weight operation if they choose to.
	 */
	default void probe() {
		getServices();
	}

	/**
	 * Default implementation for getting order of discovery clients.
	 * @return order
	 */
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
