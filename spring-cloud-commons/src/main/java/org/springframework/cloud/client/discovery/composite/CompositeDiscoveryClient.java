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

package org.springframework.cloud.client.discovery.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * 基于{@link DiscoveryClient}的实现，支持代理一组{@link DiscoveryClient}，并按顺序调用
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Sean Ruffatti
 */
public class CompositeDiscoveryClient implements DiscoveryClient {

	/**
	 * 被代理的一组服务发现客户端
	 */
	private final List<DiscoveryClient> discoveryClients;

	public CompositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
		/**
		 * 按{@link org.springframework.core.Ordered}排序
		 */
		AnnotationAwareOrderComparator.sort(discoveryClients);
		this.discoveryClients = discoveryClients;
	}

	@Override
	public String description() {
		return "Composite Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		if (this.discoveryClients != null) {
			/**
			 * 依次从服务发现客户端获取实例，只有有一个有值，就直接返回
			 */
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
				if (instances != null && !instances.isEmpty()) {
					return instances;
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		LinkedHashSet<String> services = new LinkedHashSet<>();
		if (this.discoveryClients != null) {
			/**
			 * 获取服务名称，将所有的服务发现客户端返回的服务名称进行汇总，并去重
			 */
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				List<String> serviceForClient = discoveryClient.getServices();
				if (serviceForClient != null) {
					services.addAll(serviceForClient);
				}
			}
		}
		return new ArrayList<>(services);
	}

	@Override
	public void probe() {
		if (this.discoveryClients != null) {
			/**
			 * 依次调用服务发现客户端的{@link DiscoveryClient#probe()}
			 */
			for (DiscoveryClient discoveryClient : this.discoveryClients) {
				discoveryClient.probe();
			}
		}
	}

	public List<DiscoveryClient> getDiscoveryClients() {
		return this.discoveryClients;
	}

}
