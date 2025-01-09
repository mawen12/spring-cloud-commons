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

package org.springframework.cloud.loadbalancer.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;

/**
 * 允许在cookie中，传递被负载均衡器选择的服务实例ID
 *
 * A {@link LoadBalancerRequestTransformer} that allows passing the {@code} instanceId) of
 * the {@link ServiceInstance} selected by the {@link LoadBalancerClient} in a cookie.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.2
 */
public class LoadBalancerServiceInstanceCookieTransformer implements LoadBalancerRequestTransformer {

	private ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	public LoadBalancerServiceInstanceCookieTransformer(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		this.factory = factory;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		/**
		 * 如果工厂不为空，则从工厂的属性读取对应服务的粘连Session；反之则使用默认的粘连Session
		 */
		LoadBalancerProperties.StickySession stickySession = factory != null
				? factory.getProperties(instance.getServiceId()).getStickySession()
				: new LoadBalancerProperties.StickySession();
		/**
		 * 如果粘连会话未开启将新选服务实例Cookie加入的设置，则跳过
		 */
		if (!stickySession.isAddServiceInstanceCookie()) {
			return request;
		}
		/**
		 * 如果实例Id的cookie名称未设置，则跳过
		 */
		String instanceIdCookieName = stickySession.getInstanceIdCookieName();
		if (!StringUtils.hasText(instanceIdCookieName)) {
			return request;
		}
		HttpHeaders headers = request.getHeaders();
		/**
		 * 获取原来请求头中key为COOKIE的值
		 */
		List<String> cookieHeaders = new ArrayList<>(request.getHeaders().getOrEmpty(HttpHeaders.COOKIE));
		/**
		 * 使用新的实例ID会话名称和实例ID信息构造新的Cookie
		 */
		String serviceInstanceCookie = new HttpCookie(instanceIdCookieName, instance.getInstanceId()).toString();
		/**
		 * 添加到现有的头中
		 */
		cookieHeaders.add(serviceInstanceCookie);
		/**
		 * 更新COOKIE的请求头
		 */
		headers.put(HttpHeaders.COOKIE, cookieHeaders);
		return request;
	}

}
