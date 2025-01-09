/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 同步阻塞的{@link LoadBalancerRequest}实现，用于对{@link HttpRequest}进行增强。
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.1.2
 */
class BlockingLoadBalancerRequest implements HttpRequestLoadBalancerRequest<ClientHttpResponse> {

	/**
	 * 客户端侧的负载均衡器，提供了服务实例选择，请求远程服务的方法
	 */
	private final LoadBalancerClient loadBalancer;

	/**
	 * 用于扩展负载均衡器请求的转换器集合
	 */
	private final List<LoadBalancerRequestTransformer> transformers;

	/**
	 * 保存了Http请求、请求体和执行请求的方法
	 */
	private final ClientHttpRequestData clientHttpRequestData;

	BlockingLoadBalancerRequest(LoadBalancerClient loadBalancer, List<LoadBalancerRequestTransformer> transformers,
			ClientHttpRequestData clientHttpRequestData) {
		this.loadBalancer = loadBalancer;
		this.transformers = transformers;
		this.clientHttpRequestData = clientHttpRequestData;
	}

	/**
	 * 向特定服务实例发起请求，并返回响应
	 *
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	@Override
	public ClientHttpResponse apply(ServiceInstance instance) throws Exception {
		/**
		 * 将请求、负载均衡器和实例 -> HttpRequest
		 */
		HttpRequest serviceRequest = new ServiceRequestWrapper(clientHttpRequestData.request, instance, loadBalancer);
		if (this.transformers != null) {
			/**
			 * 使用转换器对请求进行处理
			 */
			for (LoadBalancerRequestTransformer transformer : this.transformers) {
				serviceRequest = transformer.transformRequest(serviceRequest, instance);
			}
		}
		/**
		 * 执行请求，并返回响应
		 */
		return clientHttpRequestData.execution.execute(serviceRequest, clientHttpRequestData.body);
	}

	@Override
	public HttpRequest getHttpRequest() {
		return clientHttpRequestData.request;
	}

	/**
	 * 客户端Http请求数据
	 */
	static class ClientHttpRequestData {

		/**
		 * 原始的Http请求
		 */
		private final HttpRequest request;

		/**
		 * 请求体
		 */
		private final byte[] body;

		/**
		 * 客户端Http请求执行器
		 */
		private final ClientHttpRequestExecution execution;

		ClientHttpRequestData(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
			this.request = request;
			this.body = body;
			this.execution = execution;
		}

	}

}
