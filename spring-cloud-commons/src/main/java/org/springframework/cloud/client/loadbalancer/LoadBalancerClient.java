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

import java.io.IOException;
import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;

/**
 * 客户端侧的负载均衡器，隐藏了选择实例的逻辑，并提供直接向选择好的客户端发起请求，获得响应的方法
 *
 * @author Spencer Gibb
 */
public interface LoadBalancerClient extends ServiceInstanceChooser {

	/**
	 * 从负载均衡器中为指定服务选择一个实例并执行请求
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param request Allows implementations to execute pre and post actions, such as
	 * incrementing metrics.
	 * @param <T> type of the response
	 * @throws IOException in case of IO issues.
	 * @return The result of the LoadBalancerRequest callback on the selected
	 * ServiceInstance.
	 */
	<T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException;

	/**
	 * 从负载均衡器中为指定服务选择一个实例并执行请求
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param serviceInstance The service to execute the request to.
	 * @param request Allows implementations to execute pre and post actions, such as
	 * incrementing metrics.
	 * @param <T> type of the response
	 * @throws IOException in case of IO issues.
	 * @return The result of the LoadBalancerRequest callback on the selected
	 * ServiceInstance.
	 */
	<T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException;

	/**
	 * 创建一个具有真实主机和端口的适当URI以供系统使用
	 * 从 http://myservice/path/to/service -> http://host:port/path/to/service
	 * 即 myservice -> host:port，使用服务实例实现该替换
	 * @param instance service instance to reconstruct the URI
	 * @param original A URI with the host as a logical service name.
	 * @return A reconstructed URI.
	 */
	URI reconstructURI(ServiceInstance instance, URI original);

}
