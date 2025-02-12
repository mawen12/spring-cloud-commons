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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
 * 基于轮询算法实现的{@link ReactorServiceInstanceLoadBalancer}
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Zhuozhi JI
 * @author Nan Chiu
 */
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	/**
	 * 轮询标记点
	 */
	final AtomicInteger position;

	/**
	 * 服务ID
	 */
	final String serviceId;

	/**
	 * 用于提供单例的{@link ServiceInstanceListSupplier}
	 */
	private final SingletonSupplier<ServiceInstanceListSupplier> serviceInstanceListSingletonSupplier;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
		// 默认从1000开始
		this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 * @param seedPosition Round Robin element position marker
	 */
	public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId, int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceListSingletonSupplier = SingletonSupplier.of(() -> serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new));
		this.position = new AtomicInteger(seedPosition);
	}

	@SuppressWarnings("rawtypes")
	@Override
	// see original
	// https://github.com/Netflix/ocelli/blob/master/ocelli-core/
	// src/main/java/netflix/ocelli/loadbalancer/RoundRobinLoadBalancer.java
	public Mono<Response<ServiceInstance>> choose(Request request) {
		// 获取服务实例列表体提供器
		ServiceInstanceListSupplier supplier = serviceInstanceListSingletonSupplier.obtain();
		// 获取实例列表
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
		if (instances.isEmpty()) {// 实例为空时，返回空响应
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}

		if (instances.size() == 1) {// 仅有一个实例时，无需移动position，因为上游的supplier已经过滤了实例
			return new DefaultResponse(instances.get(0));
		}

		// 忽略符号位，确保pos的范围为[0, Integer.MAX_VALUE]
		int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;

		// 轮询获取实例
		ServiceInstance instance = instances.get(pos % instances.size());

		return new DefaultResponse(instance);
	}

}
