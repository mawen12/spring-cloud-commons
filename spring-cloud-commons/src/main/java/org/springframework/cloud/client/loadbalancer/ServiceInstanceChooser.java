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

package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * 配合负载均衡的实例选择器
 *
 * @author Ryan Baxter
 * @author Olga Maciaszek-Sharma
 */
public interface ServiceInstanceChooser {

	/**
	 * 从负载均衡器中为指定服务中选择一个实例
	 *
	 * Chooses a ServiceInstance from the LoadBalancer for the specified service.
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @return A ServiceInstance that matches the serviceId.
	 */
	ServiceInstance choose(String serviceId);

	/**
	 * 从负载均衡器中为指定服务和指定负载均衡请求选择一个实例
	 *
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param request The request to pass on to the LoadBalancer
	 * @param <T> The type of the request context.
	 * @return A ServiceInstance that matches the serviceId.
	 */
	<T> ServiceInstance choose(String serviceId, Request<T> request);

}
