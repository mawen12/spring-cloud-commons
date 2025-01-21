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
 * 基于轮询算法的{@link ReactorServiceInstanceLoadBalancer}实现
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Zhuozhi JI
 * @author Nan Chiu
 */
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	/**
	 * 选择位置
	 */
	final AtomicInteger position;

	/**
	 * 服务名称
	 */
	final String serviceId;

	/**
	 * 封装提供服务实例列表的懒加载类
	 */
	private final SingletonSupplier<ServiceInstanceListSupplier> serviceInstanceListSingletonSupplier;

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId id of the service for which to choose an instance
	 */
	public RoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
		this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
	}

	/**
	 * @param serviceInstanceListSupplierProvider a provider of
	 * {@link ServiceInstanceListSupplier} that will be used to get available instances
	 * @param serviceId 要选择实例的服务Id
	 * @param seedPosition 轮询机制的开始位置
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
		// 获取服务实例列表
		ServiceInstanceListSupplier supplier = serviceInstanceListSingletonSupplier.obtain();
		// 返回带有服务实例的响应
		return supplier.get(request)
			.next()
			.map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
	}

	private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances) {
		// 选择一个实例，发起请求，并发起请求，保存响应
		Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
		if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
			// 存在选择回调，响应中存在服务实例，则会回写实例
			// 此处可以用于底层实现在下次排除该服务实例
			((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
		}
		return serviceInstanceResponse;
	}

	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			// 实例为空，则返回空响应
			return new EmptyResponse();
		}

		// 如果只有一个实例，则直接返回，无需增加偏移位置，因为可能实例列表已经被过滤了
		if (instances.size() == 1) {
			return new DefaultResponse(instances.get(0));
		}

		// 忽略符号位，有可能在达到上限后，再往后走就变成负数了，所以需要进行&操作，确保循环范围在[0, Integer.MAX_VALUE]
		int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;

		// 取模计算获取实例
		ServiceInstance instance = instances.get(pos % instances.size());

		return new DefaultResponse(instance);
	}

}
