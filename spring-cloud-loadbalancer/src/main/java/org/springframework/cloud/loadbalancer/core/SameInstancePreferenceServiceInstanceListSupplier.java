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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

/**
 * 基于相同服务实例优先的{@link ServiceInstanceListSupplier}，即记录上一次选择的服务，如果还是可以使用的话，则继续使用
 *
 * @author Olga Maciaszek-Sharma
 * @author Jürgen Kreitler
 * @since 2.2.7
 */
public class SameInstancePreferenceServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier
		implements SelectedInstanceCallback {

	private static final Log LOG = LogFactory.getLog(SameInstancePreferenceServiceInstanceListSupplier.class);

	/**
	 * 由负载均衡器上一次选择的服务
	 */
	private ServiceInstance previouslyReturnedInstance;

	private boolean callGetWithRequestOnDelegates;

	public SameInstancePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		super(delegate);
	}

	public SameInstancePreferenceServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId()).isCallGetWithRequestOnDelegates();
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get().map(this::filteredBySameInstancePreference);
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		if (callGetWithRequestOnDelegates) {
			return delegate.get(request).map(this::filteredBySameInstancePreference);
		}
		return get();
	}

	private List<ServiceInstance> filteredBySameInstancePreference(List<ServiceInstance> serviceInstances) {
		if (previouslyReturnedInstance != null && serviceInstances.contains(previouslyReturnedInstance)) {
			if (LOG.isDebugEnabled()) {
				// TODO by mawen 也许可以去掉，String format
				LOG.debug(String.format("Returning previously selected service instance: %s", previouslyReturnedInstance));
			}
			/**
			 * 如果上次选择过服务，并且本次待选的服务实例列表中包含之前的，则直接使用之前的服务实例
			 */
			return Collections.singletonList(previouslyReturnedInstance);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Previously selected service instance %s was not available. Returning all the instances returned by delegate.", previouslyReturnedInstance));
		}

		/**
		 * 如果本地选择的服务列表中不包含之前的，则清空之前的服务实例
		 */
		previouslyReturnedInstance = null;
		return serviceInstances;
	}

	@Override
	public void selectedServiceInstance(ServiceInstance serviceInstance) {
		super.selectedServiceInstance(serviceInstance);
		/**
		 * 如果之前未选择过服务，或本次选择和上次不一致，则将上次更新为本次选择的服务
		 */
		if (previouslyReturnedInstance == null || !previouslyReturnedInstance.equals(serviceInstance)) {
			previouslyReturnedInstance = serviceInstance;
		}
	}

}
