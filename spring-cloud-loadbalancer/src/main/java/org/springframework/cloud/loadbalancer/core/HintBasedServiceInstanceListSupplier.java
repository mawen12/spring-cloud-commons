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

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.HintRequestContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * 使用由代理提供的{@code hint}进行服务实例列表过滤的{@link ServiceInstanceListSupplier}实现。
 *
 * <p>该类为开发过程中的断点调试提供了可能。例如在修改完对应的业务代码后，需要去验证该功能，
 * 此时就可以在请求头上加上{@code X-SC-LB-Hint=mawen}，并设置对应的值就可以了。
 * 需要注意，注册的服务实例的元信息上也要设置{@code spring.cloud.nacos.discovery.metadata.hint=mawen}。
 *
 * <p>为了确保服务之间调用能够延续这种关系，需要对{@code feign.RequestInterceptor}进行改造，
 * 需要将请求头{@code X-SC-LB-Hint=mawen}复制到新的请求上。
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.2
 */
public class HintBasedServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	private final LoadBalancerProperties properties;

	public HintBasedServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		super(delegate);
		this.properties = factory.getProperties(getServiceId());
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return delegate.get();
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		return delegate.get(request).map(instances -> filteredByHint(instances, getHint(request.getContext())));
	}

	/**
	 * 从请求上获取hint属性值
	 *
	 * @param requestContext
	 * @return
	 */
	private String getHint(Object requestContext) {
		if (requestContext == null) {
			return null;
		}
		String hint = null;
		if (requestContext instanceof RequestDataContext) {// 从请求头获取
			hint = getHintFromHeader((RequestDataContext) requestContext);
		}
		if (!StringUtils.hasText(hint) && requestContext instanceof HintRequestContext) {// 从特定请求上下文中获取
			hint = ((HintRequestContext) requestContext).getHint();
		}
		return hint;
	}

	/**
	 * 读取请求头为{@link LoadBalancerProperties#getHintHeaderName()}的值
	 *
	 * @param context
	 * @return
	 */
	private String getHintFromHeader(RequestDataContext context) {
		if (context.getClientRequest() != null) {
			HttpHeaders headers = context.getClientRequest().getHeaders();
			if (headers != null) {
				return headers.getFirst(properties.getHintHeaderName());
			}
		}
		return null;
	}

	/**
	 * 使用指定hint值对服务实例的元数据进行过滤。
	 *
	 * <p>如果没有找到匹配的，那么将返回原始实例
	 *
	 * @param instances
	 * @param hint
	 * @return 过滤后的服务实例列表
	 */
	private List<ServiceInstance> filteredByHint(List<ServiceInstance> instances, String hint) {
		if (!StringUtils.hasText(hint)) {
			return instances;
		}
		List<ServiceInstance> filteredInstances = new ArrayList<>();
		for (ServiceInstance serviceInstance : instances) {
			if (serviceInstance.getMetadata().getOrDefault("hint", "").equals(hint)) {
				filteredInstances.add(serviceInstance);
			}
		}
		if (filteredInstances.size() > 0) {
			return filteredInstances;
		}

		// If instances cannot be found based on hint,
		// we return all instances retrieved for given service id.
		return instances;
	}

}
