/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

/**
 * 基于权重来扩展委托提供{@link ServiceInstanceListSupplier}的实现
 *
 * @author Zhuozhi Ji
 * @author Olga Maciaszek-Sharma
 */
public class WeightedServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private static final Log LOG = LogFactory.getLog(WeightedServiceInstanceListSupplier.class);

	/**
	 * 实例元信息中的权重的key
	 */
	static final String METADATA_WEIGHT_KEY = "weight";

	/**
	 * 默认权重，默认为1.
	 * Nacos中默认权重为1.0D
	 */
	static final int DEFAULT_WEIGHT = 1;

	/**
	 * 权重函数，用来从{@link ServiceInstance}获取权重
	 */
	private final WeightFunction weightFunction;

	private boolean callGetWithRequestOnDelegates;

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		this(delegate, WeightedServiceInstanceListSupplier::metadataWeightFunction);
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, WeightFunction weightFunction) {
		super(delegate);
		this.weightFunction = weightFunction;
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		this(delegate, WeightedServiceInstanceListSupplier::metadataWeightFunction, loadBalancerClientFactory);
	}

	public WeightedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, WeightFunction weightFunction,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		this.weightFunction = weightFunction;
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId())
			.isCallGetWithRequestOnDelegates();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::expandByWeight);
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (callGetWithRequestOnDelegates) {
			return delegate.get(request).map(this::expandByWeight);
		}
		return get();
	}

	private List<ServiceInstance> expandByWeight(List<ServiceInstance> instances) {
		/**
		 * 没有任何实例，无法扩展
		 */
		if (instances.size() == 0) {
			return instances;
		}

		/**
		 * 获取实例所有的权重
		 */
		int[] weights = instances.stream().mapToInt(instance -> {
			try {
				/**
				 * 获取实例的权重
				 */
				int weight = weightFunction.apply(instance);
				if (weight <= 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format("The weight of the instance %s should be a positive integer, but it got %d, using %d as default", instance.getInstanceId(), weight, DEFAULT_WEIGHT));
					}
					/**
					 * 对权重<=0进行修正，修正为1
					 */
					return DEFAULT_WEIGHT;
				}
				return weight;
			}
			catch (Exception e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Exception occurred during apply weight function to instance %s, using %d as default", instance.getInstanceId(), DEFAULT_WEIGHT), e);
				}
				/**
				 * 出现转换异常，修正为1
				 */
				return DEFAULT_WEIGHT;
			}
		}).toArray();

		return new LazyWeightedServiceInstanceList(instances, weights);
	}

	/**
	 * 从服务实例的元信息中读取权重，从 metadata(weight) -> DEFAULT(1)
	 *
	 * @param serviceInstance
	 * @return
	 */
	static int metadataWeightFunction(ServiceInstance serviceInstance) {
		/**
		 * 获取服务实例的元信息
		 */
		Map<String, String> metadata = serviceInstance.getMetadata();
		if (metadata != null) {
			/**
			 * 读取metadata(weight)的值
			 */
			String weightValue = metadata.get(METADATA_WEIGHT_KEY);
			if (weightValue != null) {
				/**
				 * 将值转换为数字
				 */
				return Integer.parseInt(weightValue);
			}
		}
		// using default weight when metadata is missing or
		// weight is not specified
		/**
		 * 当服务实例上未设置权重时，使用 DEFAULT(1)
		 */
		return DEFAULT_WEIGHT;
	}

}
