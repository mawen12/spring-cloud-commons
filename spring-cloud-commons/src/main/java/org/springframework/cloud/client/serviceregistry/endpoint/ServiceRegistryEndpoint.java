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

package org.springframework.cloud.client.serviceregistry.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * 使用{@link ServiceRegistry}显示和设置服务实例状态的端点
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("unchecked")
@Endpoint(id = "serviceregistry")
public class ServiceRegistryEndpoint {

	/**
	 * 提供服务实例注册注销、状态查询更新的功能
	 */
	private final ServiceRegistry serviceRegistry;

	/**
	 * {@link ServiceRegistry}操作对象，保存了和实例相关的信息
	 */
	private Registration registration;

	public ServiceRegistryEndpoint(ServiceRegistry<?> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	@WriteOperation
	public WebEndpointResponse<?> setStatus(String status) {
		Assert.notNull(status, "status may not by null");

		if (this.registration == null) {
			return new WebEndpointResponse<>("no registration found", HttpStatus.NOT_FOUND.value());
		}

		/**
		 * 更新实例状态，状态值：UP | DOWN
		 */
		this.serviceRegistry.setStatus(this.registration, status);
		return new WebEndpointResponse<>();
	}

	@ReadOperation
	public WebEndpointResponse getStatus() {
		if (this.registration == null) {
			return new WebEndpointResponse<>("no registration found", HttpStatus.NOT_FOUND.value());
		}

		/**
		 * 获取实例状态，状态值：UP | DOWN
		 */
		return new WebEndpointResponse<>(this.serviceRegistry.getStatus(this.registration));
	}

}
