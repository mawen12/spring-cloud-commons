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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;

/**
 * 反应式负载均衡器
 *
 * @param <T> 响应内容
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public interface ReactiveLoadBalancer<T> {

	/**
	 * 请求的默认实现
	 */
	Request<DefaultRequestContext> REQUEST = new DefaultRequest<>();

	/**
	 * 基于负载均衡算法选择下一个服务器
	 *
	 * @param request - 到来的请求
	 * @return publisher for the response
	 */
	@SuppressWarnings("rawtypes")
	Publisher<Response<T>> choose(Request request);

	default Publisher<Response<T>> choose() { // conflicting name
		return choose(REQUEST);
	}

	/**
	 * 用于创建{@link ReactiveLoadBalancer}的工厂
	 *
	 * <p>每个服务都可以指定一个负载均衡器，背后的设计思路是每种服务都可以选择不同的负载行为：
	 * <ul>
	 *     <li>重试机制</li>
	 *     <li>健康检查机制</li>
	 *     <li>负载均衡算法</li>
	 * </ul>
	 *
	 * @param <T>
	 */
	interface Factory<T> {

		default LoadBalancerProperties getProperties(String serviceId) {
			return null;
		}

		ReactiveLoadBalancer<T> getInstance(String serviceId);

		/**
		 * 访问指定服务Id，指定类的属性配置
		 *
		 * @param name Name of the beans to be returned
		 * @param type The class of the beans to be returned
		 * @param <X> The type of the beans to be returned
		 * @return a {@link Map} of beans
		 * @see <code>@LoadBalancerClient</code>
		 */
		<X> Map<String, X> getInstances(String name, Class<X> type);

		/**
		 * Allows accessing a bean registered within client-specific LoadBalancer
		 * contexts.
		 * @param name Name of the bean to be returned
		 * @param clazz The class of the bean to be returned
		 * @param generics The classes of generic types of the bean to be returned
		 * @param <X> The type of the bean to be returned
		 * @return a {@link Map} of beans
		 * @see <code>@LoadBalancerClient</code>
		 */
		<X> X getInstance(String name, Class<?> clazz, Class<?>... generics);

	}

}
