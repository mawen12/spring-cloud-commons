/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.util.function.SingletonSupplier;

/**
 * 基于随机算法实现的{@link ReactorServiceInstanceLoadBalancer}
 *
 * <p>底层主要通过{@link ServiceInstanceListSupplier}获取服务实例列表
 *
 * @author Olga Maciaszek-Sharma
 * @author Nan Chiu
 * @since 2.2.7
 */
public class RandomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RandomLoadBalancer.class);

	/**
	 * 服务名称
	 */
	private final String serviceId;

	/**
	 * 提供获取服务实例列表的单例提供
	 */
	private final SingletonSupplier<ServiceInstanceListSupplier> serviceInstanceListSingletonSupplier;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RandomLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
		this.serviceId = serviceId;
		this.serviceInstanceListSingletonSupplier = SingletonSupplier.of(() -> serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		// 获取服务实例列表提供器
		ServiceInstanceListSupplier supplier = serviceInstanceListSingletonSupplier.obtain();
		// 获取服务实例
		return supplier.get(request)
			.next()
			// 触发SelectedInstanceCallback回调
			.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
	}

	private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances) {
		// 获取带有实例的响应
		Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
		if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {// 如果存在回调，并且存在服务器，则执行回调
			((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
		}
		return serviceInstanceResponse;
	}

	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}
		// 线程本地随机算法
		int index = ThreadLocalRandom.current().nextInt(instances.size());

		ServiceInstance instance = instances.get(index);

		return new DefaultResponse(instance);
	}

}
