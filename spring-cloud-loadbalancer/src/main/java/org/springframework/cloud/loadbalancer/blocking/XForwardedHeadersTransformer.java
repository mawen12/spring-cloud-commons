/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.loadbalancer.blocking;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

/**
 * XForward请求头转换器，用于向{@link HttpRequest}添加以下请求头：
 * <ul>
 *     <li>X-Forwarded-Host</li>
 *     <li>X-Forwarded-Proto</li>
 * </ul>
 * <p>
 * XForward是一种标准请求头，用于标识客户端通过代理连接到服务器的原始IP地址
 * X-Forwarded-Host是一种标准请求头，用于标识客户端发出请求的原始主机
 * X-Forwarded-Proto是一种标准请求头，用于标识客户端连接到代理或负载均衡器时的协议（HTTP/HTTPS）
 *
 * @author Gandhimathi Velusamy
 * @author Olga Maciaszek-Sharma
 * @author junjie shen(沈俊杰)
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For">X-Forwareded-For</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host">X-Forwared-Host</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Proto">X-Forwared-Proto</a>
 * @since 3.1.0
 */

public class XForwardedHeadersTransformer implements LoadBalancerRequestTransformer {

	private final ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	public XForwardedHeadersTransformer(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		this.factory = factory;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		/**
		 * 读取该服务下的 PROPERTIES(xForwarded)
		 */
		LoadBalancerProperties.XForwarded xForwarded = factory.getProperties(instance.getServiceId()).getXForwarded();
		if (xForwarded.isEnabled()) {
			/**
			 * 如果启用了xForwared,则获取主机与协议，并分别写入X-Forwarded-Host和X-Forwarded-Proto请求头
			 */
			HttpHeaders headers = request.getHeaders();
			String xForwardedHost = request.getURI().getHost();
			String xForwardedProto = request.getURI().getScheme();
			headers.add("X-Forwarded-Host", xForwardedHost);
			headers.add("X-Forwarded-Proto", xForwardedProto);
		}
		return request;
	}

}
